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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JsonDeserialize(builder = Statement.Builder.class)
public class Statement {

    @JsonIgnore
    private static final String ACTION = "execute-api:Invoke";
    @JsonIgnore
    private final String effect;
    @JsonIgnore
    private final List<String> resources;

    private Statement(Builder builder) {
        this.effect = builder.effect;
        this.resources = builder.resources;
    }

    @JsonProperty("Action")
    public String getAction() {
        return ACTION;
    }

    @JsonProperty("Effect")
    public String getEffect() {
        return effect;
    }

    @JsonProperty("Resource")
    public List<String> getResource() {
        return List.copyOf(resources);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String effect = "Deny";
        private List<String> resources = new ArrayList<>(List.of("*"));

        private Builder() {
        }

        public Builder effect(String effect) {
            this.effect = "Allow".equals(effect) ? effect : "Deny";
            return this;
        }

        public Builder resource(String... resource) {
            if (resource != null && resource.length > 0) {
                resources.clear();
                Collections.addAll(this.resources, resource);
            }
            return this;
        }

        public Builder resource(Collection<String> resource) {
            if (resource != null && !resource.isEmpty()) {
                resources.clear();
                resources.addAll(resource);
            }
            return this;
        }

        public Statement build() {
            return new Statement(this);
        }
    }
}
