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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AuthorizerTest {

    private final static ObjectMapper MAPPER = new ObjectMapper();
    private static String ID_TOKEN;
    private static Map<String, String> ID_TOKEN_HEADER;
    private static String COGNITO_JWKS_JSON;
    private static Map<String, List<Map<String, String>>> COGNITO_KEYS;

    @BeforeClass
    public static void setup() throws Exception {
        ID_TOKEN = "eyJraWQiOiI1Y21PWU00b0paNXZsVXh1aDBRalhzZXBBNU02RldcL0sxcGQ2YkE3eXRjWT0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxNWY0OWQ5Mi1mNjBkLTQwODktYWRlOC1jYmY1YmY0ZWI1N2EiLCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAuZXUtd2VzdC0xLmFtYXpvbmF3cy5jb21cL2V1LXdlc3QtMV85RWJlS2lYME8iLCJjb2duaXRvOnVzZXJuYW1lIjoibWliZWFyZCtzdnMzMDNkcnlydW5AYW1hem9uLmNvbSIsImN1c3RvbTp0ZW5hbnRfaWQiOiI3N2IyYmUyMC1mMzBhLTQwYzYtYmZmZi0zNzYxMTI4MzJmOTUiLCJnaXZlbl9uYW1lIjoiTGFiIDIiLCJjdXN0b206Y29tcGFueSI6IkNvZ25pdG8gQ29uZmlybWVkIiwiYXVkIjoiNXI1MHZxY21wcDg1ZmdtcjU0bnYwOTczb3MiLCJjdXN0b206cGxhbiI6IlN0YW5kYXJkIFBsYW4iLCJldmVudF9pZCI6IjViZWUzNWMwLTM3MTMtNDA2My05NWY3LWViZWVlZGJhZmNiNyIsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNTczMTA2ODc1LCJleHAiOjE1NzMxMTA0NzUsImlhdCI6MTU3MzEwNjg3NSwiZmFtaWx5X25hbWUiOiJDb2duaXRvIiwiZW1haWwiOiJtaWJlYXJkK3N2czMwM2RyeXJ1bkBhbWF6b24uY29tIn0.caMbyrFGqUA2oWxiorymOek8iNhfhY9Yr6iwT2XrrDAJcRrix9NT3TzDI9fJkhsOGdnPNEhceNFRckuQOmLdjuoU0UneAc7vf3RwL1c3XCn6MvZwUFxKo3SX1liALEz7cJYZtApze5-7XHQ4X5Mo44kDwd5AbBsH-r8x_b2p7iUp1w6WjOIn1_kmLc1otnwH5BNUnXPdLWx-gaVyd2mJlc3GmJWZzzEmmWB1xy2w0osZwXcthu_lnseVcRNmKds9L2J8Y0i1mEH1ROOKbDo7RKT8k6CCq_akCJ3e4maJ9aRRrIlw3OKoZDz7YYdkpfIiT6_CXLK0BIvaPHGjndLVHw";

        String encodedHeader = ID_TOKEN.split("\\.")[0];
        String headerJson = new String(Base64.getDecoder().decode(encodedHeader), StandardCharsets.UTF_8);
        ID_TOKEN_HEADER = MAPPER.readValue(headerJson, Map.class);

        InputStream cognitoJwksStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("cognito_jwks.json");
        ByteArrayOutputStream cognitoJwksJson = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = cognitoJwksStream.read(buffer)) != -1) {
            cognitoJwksJson.write(buffer, 0, length);
        }
        COGNITO_JWKS_JSON = cognitoJwksJson.toString(StandardCharsets.UTF_8.name());
        COGNITO_KEYS = MAPPER.readValue(COGNITO_JWKS_JSON, Map.class);
    }

    @Before
    public void init() throws  Exception {
    }

    @Test
    @Ignore // static ID_TOKEN will be expired after an hour (by default) and JJWT will throw an error...
    public void testDecodeCognitoJwt() {
        System.out.println("testDecodeCognitoJwt");
        //ID_TOKEN_HEADER.forEach((k, v) -> System.out.format("%s => %s%n", k, v));
        //COGNITO_KEYS.get("keys").forEach(m -> m.entrySet().forEach(e -> System.out.format("%s => %s%n", e.getKey(), e.getValue())));

        //SigningKeyResolverAdapter resolver = new CognitoSigningKeyResolver(COGNITO_JWKS_JSON);
        SigningKeyResolver resolver = CognitoSigningKeyResolver.builder().jwksJson(COGNITO_JWKS_JSON).build();
        Claims claims = Jwts.parser()
                .setSigningKeyResolver(resolver)
                .parseClaimsJws(ID_TOKEN)
                .getBody();

        claims.forEach((k, v) -> System.out.format("%s => %s%n", k, v));
    }
}