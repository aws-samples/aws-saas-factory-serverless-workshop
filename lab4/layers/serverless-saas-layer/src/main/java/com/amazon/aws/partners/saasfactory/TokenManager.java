/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;
import software.amazon.awssdk.utils.IoUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.stream.Collectors;

public class TokenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TENANT_CLAIM = "custom:tenant_id";
    private final Map<String, List<Map<String, String>>> userPoolsJwks = new HashMap<>();
    private CognitoIdentityProviderClient cognito;

    public TokenManager() {
        this.cognito = CognitoIdentityProviderClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        init();
    }

    public String getTenantId(Map<String, Object> event) {
        String bearerToken = ((Map<String, String>) event.get("headers")).get("Authorization");
        String jwtToken = bearerToken.substring(bearerToken.indexOf(" ") + 1);
        Claims verifiedClaims = Jwts.parser()
                .setSigningKeyResolver(keyResolver())
                .parseClaimsJws(jwtToken)
                .getBody();

        String tenantId = verifiedClaims.get(TENANT_CLAIM, String.class);

        if (tenantId == null) {
            throw new RuntimeException("No tenant id in token");
        }

        return tenantId;
    }

    public SigningKeyResolver keyResolver() {
        LOGGER.info("TokenManager::keyResolver building key resolver for " + userPoolsJwks.keySet().size() + " user pools");
        List<List<Map<String, String>>> cognitoKeys = new ArrayList<>();
        userPoolsJwks.entrySet()
                .stream()
                .map(e -> e.getValue())
                .forEachOrdered(cognitoKeys::add);
        return CognitoSigningKeyResolver.builder().jwks(cognitoKeys).build();
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

    static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    static class CognitoSigningKeyResolver extends SigningKeyResolverAdapter {

        public final static ObjectMapper MAPPER = new ObjectMapper();
        private List<List<Map<String, String>>> wellKnowns = new ArrayList<>();

        private CognitoSigningKeyResolver(Builder builder) {
            this.wellKnowns = builder.wellKnowns;
        }

        @Override
        public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
            Key key = null;
            String keyId = jwsHeader.getKeyId();

            Map<String, String> jwk = null;
            for (List<Map<String, String>> jwks : wellKnowns) {
                List<Map<String, String>> filter = jwks.stream().filter(j -> keyId.equals(j.get("kid"))).collect(Collectors.toList());
                if (filter != null && filter.size() == 1) {
                    jwk = filter.get(0);
                    break;
                }
            }

            if (jwk != null && !jwk.isEmpty()) {
                String keytype = jwk.get("kty");
                if ("RSA".equals(keytype)) {
                    BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("n")));
                    BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("e")));
                    RSAPublicKeySpec rsaSpec = new RSAPublicKeySpec(modulus, exponent);
                    try {
                        KeyFactory keyFactory = KeyFactory.getInstance(keytype);
                        key = (RSAPublicKey) keyFactory.generatePublic(rsaSpec);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return key;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            List<List<Map<String, String>>> wellKnowns = new ArrayList<>();

            private Builder() {
            }

            public Builder jwks(List<List<Map<String, String>>> jwks) {
                wellKnowns.addAll(jwks);
                return this;
            }

            public Builder jwksSingle(List<Map<String, String>> jwks) {
                wellKnowns.add(jwks);
                return this;
            }

            public Builder jwksJson(String cognitoWellKnownJson) {
                try {
                    Map<String, List<Map<String, String>>> cognitoWellKnownJwks = MAPPER.readValue(cognitoWellKnownJson, Map.class);
                    this.wellKnowns.add(cognitoWellKnownJwks.get("keys"));
                } catch (IOException ioe) {
                    System.out.println(ioe.getMessage());
                }
                return this;
            }

            public CognitoSigningKeyResolver build() {
                return new CognitoSigningKeyResolver(this);
            }
        }
    }
}