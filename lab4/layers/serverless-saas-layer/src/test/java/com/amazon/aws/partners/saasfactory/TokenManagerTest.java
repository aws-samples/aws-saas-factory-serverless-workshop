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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.Base64Codec;
import io.jsonwebtoken.impl.crypto.MacProvider;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class TokenManagerTest {

    private Key secret;
    private String encodedSecret;
    private String token;

    @org.junit.Before
    public void setUp() throws Exception {
        secret = MacProvider.generateKey(SignatureAlgorithm.HS256);
        encodedSecret = Base64.getEncoder().encodeToString(secret.getEncoded());
//        System.out.println("encodedSecret = " + encodedSecret);
        Map<String, Object> customeClaims = new HashMap<>();
        customeClaims.put("TenantId", UUID.randomUUID());
        token = Jwts.builder()
                .setSubject("SaaS Factory")
                .addClaims(customeClaims)
                .signWith(SignatureAlgorithm.HS256, encodedSecret)
                .compact();
    }

    @Test
    public void testJwt() {
        System.out.println("testJwt");
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTYWFTIEZhY3RvcnkiLCJUZW5hbnRJZCI6ImZlNTNlZDQ2LTg4ODgtNGYxMi1iZThmLTRkNWViYjM4NTUzYiJ9.Oac177ZGLkf6oaTCfKaip0PFY6oOhAnylkPSAezA27g";
        System.out.println(token);
        String[] parts = token.split("\\.");
        String header = Base64Codec.BASE64.decodeToString(parts[0]);
        String body = Base64Codec.BASE64.decodeToString(parts[1]);
//        Object claims = Jwts.parser()
//                .setSigningKey(encodedSecret)
//                .parseClaimsJws(token)
//                .parse(token).getBody();
//                .getBody();
        System.out.println("Header = " + header);
        System.out.println("Body = " + body);
//        claims.forEach((key, value) -> System.out.println(key + " => " + value));
        String issuer = "https://cognito-idp.us-east-1.amazonaws.com/<userpoolID>";
        String userPoolId = issuer.substring(issuer.lastIndexOf("/") + 1);
        System.out.println("UserPoolId = " + userPoolId);
    }

    @Test
    public void testCreateJwt() {
        System.out.println("testCreateJwt");
        byte[] keyBytes = Base64.getDecoder().decode("+Kahb1I+prVQZF41dRpNj22qBtAw4Qn1P45VwpELXCc=");
        //SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
        Map<String, Object> customeClaims = new HashMap<>();
        customeClaims.put("TenantId", "fe53ed46-8888-4f12-be8f-4d5ebb38553b");
        String jwt = Jwts.builder()
                .setSubject("SaaS Factory")
                .addClaims(customeClaims)
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();

        String expected = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTYWFTIEZhY3RvcnkiLCJUZW5hbnRJZCI6ImZlNTNlZDQ2LTg4ODgtNGYxMi1iZThmLTRkNWViYjM4NTUzYiJ9.Oac177ZGLkf6oaTCfKaip0PFY6oOhAnylkPSAezA27g";
        System.out.println("Expected: " + expected);
        System.out.println("Created:  " + jwt);
        assertEquals(expected, jwt);

        Claims knownClaims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(expected)
                .getBody();
        knownClaims.forEach((k, v) -> System.out.println(k + " => " + v));

        System.out.println();

        Claims createdClaims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(jwt)
                .getBody();
        createdClaims.forEach((k, v) -> System.out.println(k + " => " + v));
    }

    @Test
    public void generateJwtTest() {
        String[] tenantIds = new String[] {"96240b04-4fc6-4948-bacd-594272c3e9cf", "ebd89913-7f81-4e92-9bf8-80b04da96a5f"};
        byte[] keyBytes = Base64.getDecoder().decode("+Kahb1I+prVQZF41dRpNj22qBtAw4Qn1P45VwpELXCc=");
        //SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
        for (int i = 0; i < tenantIds.length; i++) {
            Map<String, Object> customeClaims = new HashMap<>();
            customeClaims.put("TenantId", tenantIds[i]);
            String jwt = Jwts.builder()
                    .setSubject("SaaS Factory")
                    .addClaims(customeClaims)
                    .signWith(SignatureAlgorithm.HS256, key)
                    .compact();
            System.out.println(tenantIds[i]);
            System.out.println(jwt);
            System.out.println();
        }
    }
}