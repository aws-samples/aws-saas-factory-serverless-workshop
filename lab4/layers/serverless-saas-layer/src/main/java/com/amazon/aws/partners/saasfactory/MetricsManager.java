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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class MetricsManager {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    public static void recordMetric(Map<String, Object> event, String source, String action, Long duration) {
        String tenantId = new TokenManager().getTenantId(event);

        Map<String, Object> metric = new HashMap<>();
        metric.put("tenantId", tenantId);
        metric.put("source", source);
        metric.put("action", action);
        metric.put("duration", duration);
        metric.put("utc", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString().substring(0, 23).replace('T', ' '));

        String json;
        try {
            json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(metric);
        } catch (JsonProcessingException e) {
            LoggingManager.error(event, getFullStackTrace(e));
            json = e.getMessage();
        }

        LoggingManager.log(event, "MetricsManager::recordMetric\n" + json);
    }

    static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}