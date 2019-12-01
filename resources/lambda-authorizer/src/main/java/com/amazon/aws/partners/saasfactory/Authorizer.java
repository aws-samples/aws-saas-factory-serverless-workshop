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
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;
import software.amazon.awssdk.utils.IoUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

public class Authorizer implements RequestHandler<Map<String, String>, AuthorizerResponse> {

    private final static Logger LOGGER = LoggerFactory.getLogger(Authorizer.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private Map<String, List<Map<String, String>>> userPoolsJwks = new HashMap<>();
    private CognitoIdentityProviderClient cognito;

    public Authorizer() {
        this.cognito = CognitoIdentityProviderClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        init();
    }

    public final void init() {
        ListUserPoolsResponse userPoolsResponse = cognito.listUserPools(request -> request.maxResults(60));
        List<UserPoolDescriptionType> userPools = userPoolsResponse.userPools();
        if (userPools != null) {
            for (UserPoolDescriptionType userPool : userPools) {
                String userPoolId = userPool.id();
                if (!userPoolsJwks.containsKey(userPoolId)) {
                    addUserPoolJwks(userPoolId);
                }
            }
        }
    }

    public final void addUserPoolJwks(String userPoolId) {
        String url = "https://cognito-idp." + System.getenv("AWS_REGION") + ".amazonaws.com/" + userPoolId + "/.well-known/jwks.json";
        try {
            HttpURLConnection cognitoIdp = (HttpURLConnection) URI.create(url).toURL().openConnection();
            cognitoIdp.setRequestMethod("GET");
            cognitoIdp.setRequestProperty("Accept", "application/json");
            cognitoIdp.setRequestProperty("Content-Type", "application/json");
            if (cognitoIdp.getResponseCode() >= 400) {
                throw new Exception(IoUtils.toUtf8String(cognitoIdp.getErrorStream()));
            }
            String jwks = IoUtils.toUtf8String(cognitoIdp.getInputStream());
            cognitoIdp.disconnect();

            Map<String, List<Map<String, String>>> cognitoWellKnownJwks = MAPPER.readValue(jwks, Map.class);
            userPoolsJwks.put(userPoolId, cognitoWellKnownJwks.get("keys"));
        } catch (Exception e) {
            LOGGER.error(getFullStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public AuthorizerResponse handleRequest(Map<String, String> event, Context context) {
        logRequestEvent(event);
        if (event.containsKey("source") && "warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return AuthorizerResponse.builder().build();
        }

        LOGGER.info("Invoking TOKEN Lambda Authorizer");
        AuthorizerResponse response = null;
        try {
            String token = event.get("authorizationToken");
            String tenantId = getTenantId(token);

            // Pass the tenant id back to API Gateway so we can map it to a custom
            // HTTP header value -- will be available as context.authorizer.TenantId
            // in the Integration Request configuration of the API method
            Map<String, String> extraContext = new HashMap<>();
            extraContext.put("TenantId", tenantId);

            String methodArn = event.get("methodArn");
            String[] request = methodArn.split(":");
            String[] apiGatewayArn = request[5].split("/");

            String region = request[3];
            String accountId = request[4];
            String apiId = apiGatewayArn[0];
            String stage = apiGatewayArn[1];

            // This authorizer is shared across our API, so we are just going to
            // grant access to all REST Resources of all HTTP methods defined for
            // this API for this Stage in this Region for this Account
            String resource = String.format("arn:aws:execute-api:%s:%s:%s/%s/*/*",
                    region,
                    accountId,
                    apiId,
                    stage
            );
            LOGGER.info("AuthPolicy resource ARN = " + resource);

            // Build the IAM policy document to grant authorization
            Statement statement = Statement.builder()
                    .effect("Allow")
                    .resource(resource)
                    .build();

            PolicyDocument policyDocument = PolicyDocument.builder()
                    .statements(Collections.singletonList(statement))
                    .build();

            response = AuthorizerResponse.builder()
                    .principalId(accountId)
                    .policyDocument(policyDocument)
                    .context(extraContext)  // <- This is the important piece for this workshop
                    .build();

        } catch (Exception e) {
            LOGGER.error(getFullStackTrace(e));
            throw new RuntimeException("Unauthorized");
        }

        return response;
    }

    String getTenantId(String token) {
        LOGGER.info("Authorizer::getTenantId");
        String jwtToken = token.substring(token.indexOf(" ") + 1);

        LOGGER.info("Authorizer::getTenantId building key resolver for " + userPoolsJwks.keySet().size() + " user pools");
        List<List<Map<String, String>>> cognitoKeys = new ArrayList<>();
        userPoolsJwks.entrySet()
                .stream()
                .map(e -> e.getValue())
                .forEachOrdered(cognitoKeys::add);

        SigningKeyResolver keyResolver = CognitoSigningKeyResolver.builder().jwks(cognitoKeys).build();

        LOGGER.info("Authorizer::getTenantId parsing JWT");
        Claims verifiedClaims = Jwts.parser()
                .setSigningKeyResolver(keyResolver)
                .parseClaimsJws(jwtToken)
                .getBody();

        String tenantId = verifiedClaims.get("custom:tenant_id", String.class);
        LOGGER.info("Authorizer::getTenantId returning " + tenantId);

        return tenantId;
    }

    static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private static void logRequestEvent(Map<String, String> event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            LOGGER.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not log request event " + e.getMessage());
        }
    }



//public class Authorizer implements RequestHandler<APIGatewayProxyRequestEvent, AuthorizerResponse> {
    /**
     * API Gateway also supports REQUEST Lambda Authorizers which gives your authorizer function
     * access to many parts of the HTTP request not just the authorization token and API Gateway
     * ARN. You could use this type of authorizer to inspect the URL path or request variables
     * for example.
     */
    public AuthorizerResponse handleRequestAuthorizer(APIGatewayProxyRequestEvent event, Context context) {
        LOGGER.info("Invoking REQUEST Lambda Authorizer");

        // API Gateway lowercases the HTTP header keys...
        String bearerToken = event.getHeaders().get("authorization");
        LOGGER.info("Parsing JWT " + bearerToken);
        String jwtToken = bearerToken.substring(bearerToken.indexOf(" ") + 1);
        Claims verifiedClaims = Jwts.parser()
                //.setSigningKey(SIGNING_KEY)
                .parseClaimsJws(jwtToken)
                .getBody();
        String tenantId = verifiedClaims.get("TenantId", String.class);

        LOGGER.info("Extracted Tenant ID from JWT token " + tenantId);
        Map<String, String> extraContext = new HashMap<>();
        extraContext.put("TenantId", tenantId);

        APIGatewayProxyRequestEvent.ProxyRequestContext proxyContext = event.getRequestContext();
        String resource = String.format("arn:aws:execute-api:%s:%s:%s/%s/*/*",
                System.getenv("AWS_REGION"),
                proxyContext.getAccountId(),
                proxyContext.getApiId(),
                proxyContext.getStage()
        );
        LOGGER.info("AuthPolicy ARN = " + resource);
        Statement statement = Statement.builder()
                .effect("Allow")
                .resource(resource)
                .build();

        PolicyDocument policyDocument = PolicyDocument.builder()
                .statements(Collections.singletonList(statement))
                .build();

        return AuthorizerResponse.builder()
                .principalId(proxyContext.getIdentity().getAccountId())
                .policyDocument(policyDocument)
                .context(extraContext)
                .build();
    }
}