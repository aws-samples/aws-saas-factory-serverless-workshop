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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonDeserialize(builder = PolicyDocument.Builder.class)
public class PolicyDocument {

    @JsonIgnore
    private static final String VERSION = "2012-10-17";
    @JsonIgnore
    private final List<Statement> statements;

    private PolicyDocument(Builder builder) {
        this.statements = builder.statements;
    }

    @JsonProperty("Version")
    public String getVersion() {
        return VERSION;
    }

    @JsonProperty("Statement")
    public List<Statement> getStatement() {
        return List.copyOf(statements);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private List<Statement> statements = new ArrayList<>(List.of(Statement.builder().build()));

        private Builder() {
        }

        public Builder statement(Statement... statement) {
            if (statement != null && statement.length > 0) {
                statements.clear();
                Collections.addAll(this.statements, statement);
            } else {
                this.statements = new ArrayList<>();
            }
            return this;
        }

        public PolicyDocument build() {
            return new PolicyDocument(this);
        }
    }
}
