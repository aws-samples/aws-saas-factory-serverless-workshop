/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.regex.Pattern;

@JsonDeserialize(builder = TokenAuthorizerRequest.Builder.class)
public class TokenAuthorizerRequest {

    private static final Pattern BEARER_TOKEN_REGEX = Pattern.compile("^[B|b]earer +");
    private final String type;
    private final String methodArn;
    private final String authorizationToken;
    @JsonIgnore
    private final String region;
    @JsonIgnore
    private final String accountId;
    @JsonIgnore
    private final String apiId;
    @JsonIgnore
    private final String stage;

    private TokenAuthorizerRequest(Builder builder) {
        this.type = builder.type;
        this.methodArn = builder.methodArn;
        this.authorizationToken = builder.authorizationToken;

        String[] request = methodArn.split(":");
        String[] apiGatewayArn = request[5].split("/");

        region = request[3];
        accountId = request[4];
        apiId = apiGatewayArn[0];
        stage = apiGatewayArn[1];
    }

    public String getType() {
        return type;
    }

    public String getRegion() {
        return region;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getApiId() {
        return apiId;
    }

    public String getStage() {
        return stage;
    }

    public String tokenPayload() {
        return BEARER_TOKEN_REGEX.split(authorizationToken)[1];
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String type = "TOKEN";
        private String methodArn;
        private String authorizationToken;

        private Builder() {
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder methodArn(String methodArn) {
            this.methodArn = methodArn;
            return this;
        }

        public Builder authorizationToken(String authorizationToken) {
            this.authorizationToken = authorizationToken;
            return this;
        }

        public TokenAuthorizerRequest build() {
            return new TokenAuthorizerRequest(this);
        }
    }
}
