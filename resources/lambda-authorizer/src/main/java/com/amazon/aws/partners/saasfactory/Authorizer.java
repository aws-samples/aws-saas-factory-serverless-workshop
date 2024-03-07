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
import software.amazon.awssdk.regions.Region;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Authorizer implements RequestStreamHandler {

    private final static Logger LOGGER = LoggerFactory.getLogger(Authorizer.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();

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
        String userPoolId = getTokenIssuer(request.tokenPayload());
        JWTVerifier verifier = JWT
                .require(Algorithm.RSA256(new CognitoKeyProvider(userPoolId)))
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

    protected String getTokenIssuer(String token) {
        String issuer = JWT.decode(token).getClaim("iss").asString();
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