/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Authorizer implements RequestStreamHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(Authorizer.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String AWS_REGION = System.getenv("AWS_REGION");

    // A Cognito user pool id is always "<region>_<alphanumeric>". The value we extract from the
    // token's issuer claim is interpolated into a JWKS URL, so constrain its shape before use.
    private final static Pattern USER_POOL_ID = Pattern.compile("^[a-z]{2}(-[a-z]+)+-\\d_[A-Za-z0-9]+$");

    // User pool ids confirmed to belong to this AWS account. Lambda reuses an execution
    // environment across invocations, so this cache spares a DescribeUserPool call per request
    // for pools we have already vouched for. Only positive results are cached.
    private final static Set<String> TRUSTED_USER_POOLS = ConcurrentHashMap.newKeySet();

    private final static CognitoIdentityProviderClient COGNITO = CognitoIdentityProviderClient.create();

    public void handleRequest(InputStream input, OutputStream output, Context context) {
        // Using a RequestSteamHandler here because there doesn't seem to be a way to get
        // a hold of the internal Jackson ObjectMapper from AWS to adjust it to deal with
        // the uppercase property names in the Policy document
        TokenAuthorizerRequest event = fromJson(input, TokenAuthorizerRequest.class);
        if (null == event) {
            throw new RuntimeException("Can't deserialize input");
        }
        LOGGER.info(toJson(event));

        AuthorizerResponse response;
        DecodedJWT token = verifyToken(event);
        if (token == null) {
            LOGGER.error("JWT not verified. Returning Not Authorized");
            response = AuthorizerResponse.builder()
                    .principalId(event.getAccountId())
                    .policyDocument(PolicyDocument.builder()
                            .statement(Statement.builder()
                                    .effect("Deny")
                                    .resource(apiGatewayResource(event))
                                    .build()
                            )
                            .build()
                    )
                    .context(new HashMap<>())
                    .build();
        } else {
            LOGGER.info("JWT verified. Returning Authorized.");
            String tenantId = getTenantId(token);

            // Pass the tenant id back to API Gateway so we can map it to a custom
            // HTTP header value -- will be available as context.authorizer.TenantId
            // in the Integration Request configuration of the API method
            Map<String, String> extraContext = new HashMap<>();
            extraContext.put("TenantId", tenantId);

            // This authorizer is shared across our API, so we are just going to
            // grant access to all REST Resources of all HTTP methods defined for
            // this API for this Stage in this Region for this Account
            response = AuthorizerResponse.builder()
                    .principalId(event.getAccountId())
                    .policyDocument(PolicyDocument.builder()
                            .statement(Statement.builder()
                                    .effect("Allow")
                                    .resource(apiGatewayResource(event))
                                    .build()
                            )
                            .build()
                    )
                    .context(extraContext)
                    .build();
        }
        LOGGER.info(toJson(response));

        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write(toJson(response));
            writer.flush();
        } catch (Exception e) {
            LOGGER.error(getFullStackTrace(e));
            throw new RuntimeException(e.getMessage());
        }
    }

    protected DecodedJWT verifyToken(TokenAuthorizerRequest request) {
        // The user pool id is read from the token's own issuer claim, which at this point is
        // NOT verified -- JWT.decode() only base64-decodes the payload. It is therefore
        // attacker-chosen, and is used here solely as a lookup key.
        //
        // That matters because Cognito's JWKS endpoint is public and account-agnostic. Without
        // the check below, an attacker could stand up a user pool in their OWN AWS account,
        // mint a correctly-signed token naming any tenant, and this authorizer would fetch
        // that pool's public keys and verify the signature successfully. Signature validity is
        // not issuer trust.
        String userPoolId = getTokenIssuer(request.tokenPayload());
        if (!isTrustedUserPool(userPoolId)) {
            LOGGER.error("Rejecting token: issuer user pool {} is not owned by this deployment",
                    userPoolId);
            return null;
        }

        JWTVerifier verifier = JWT
                .require(Algorithm.RSA256(new CognitoKeyProvider(userPoolId)))
                .withIssuer(issuerUrl(userPoolId)) // the verified token must carry the issuer we resolved
                .acceptLeeway(5L) // Allowed seconds of clock skew between token issuer and verifier
                .build();
        DecodedJWT token = null;
        try {
            token = verifier.verify(request.tokenPayload());
        } catch (JWTVerificationException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return token;
    }

    /**
     * Confirms that a user pool belongs to the AWS account this authorizer runs in.
     *
     * DescribeUserPool is account-scoped: it cannot describe a pool in somebody else's account,
     * so a successful call is proof of ownership. Every tenant pool in this solution is created
     * by the registration service inside this account, so legitimate pools always pass.
     */
    protected boolean isTrustedUserPool(String userPoolId) {
        if (userPoolId == null || !USER_POOL_ID.matcher(userPoolId).matches()) {
            return false;
        }
        if (TRUSTED_USER_POOLS.contains(userPoolId)) {
            return true;
        }
        try {
            COGNITO.describeUserPool(request -> request.userPoolId(userPoolId));
            TRUSTED_USER_POOLS.add(userPoolId);
            return true;
        } catch (ResourceNotFoundException e) {
            // The pool does not exist in this account. This is the attack above, and is the
            // expected outcome for a token minted by a pool we do not own.
            return false;
        } catch (SdkException e) {
            // Fail closed. A throttle or a missing cognito-idp:DescribeUserPool permission must
            // deny rather than fall back to trusting the token's own issuer claim.
            LOGGER.error("Could not verify ownership of user pool {}: {}", userPoolId, e.getMessage());
            return false;
        }
    }

    protected String issuerUrl(String userPoolId) {
        return String.format("https://cognito-idp.%s.amazonaws.com/%s", AWS_REGION, userPoolId);
    }

    protected String getTokenIssuer(String token) {
        String issuer = JWT.decode(token).getClaim("iss").asString();
        if (issuer == null) {
            return null;
        }
        return issuer.substring(issuer.lastIndexOf("/") + 1);
    }

    protected String getTenantId(DecodedJWT token) {
        return token.getClaim("custom:tenant_id").asString();
    }

    protected String apiGatewayResource(TokenAuthorizerRequest event) {
        return apiGatewayResource(event, "*", "*");
    }

    protected String apiGatewayResource(TokenAuthorizerRequest event, String method, String resource) {
        String arn = String.format("arn:%s:execute-api:%s:%s:%s/%s/%s/%s",
                Region.of(event.getRegion()).metadata().partition().id(),
                event.getRegion(),
                event.getAccountId(),
                event.getApiId(),
                event.getStage(),
                method,
                resource
        );
        return arn;
    }
    protected String toJson(Object obj) {
        String json = null;
        try {
            json = MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return json;
    }

    protected <T> T fromJson(String json, Class<T> serializeTo) {
        T object = null;
        try {
            object = MAPPER.readValue(json, serializeTo);
        } catch (Exception e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return object;
    }

    protected <T> T fromJson(InputStream json, Class<T> serializeTo) {
        T object = null;
        try {
            object = MAPPER.readValue(json, serializeTo);
        } catch (Exception e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return object;
    }

    protected String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}