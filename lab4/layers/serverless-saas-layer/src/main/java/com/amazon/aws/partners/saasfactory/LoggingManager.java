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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LoggingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingManager.class);

    public static void debug(Map<String, Object> event, String message) {
        LOGGER.debug(decorateMessage(event, message));
    }

    public static void log(Map<String, Object> event, String message) {
        info(event, message);
    }

    public static void info(Map<String, Object> event, String message) {
        LOGGER.info(decorateMessage(event, message));
    }

    public static void warn(Map<String, Object> event, String message) {
        LOGGER.warn(decorateMessage(event, message));
    }

    public static void error(Map<String, Object> event, String message) {
        LOGGER.error(decorateMessage(event, message));
    }

    private static String decorateMessage(Map<String, Object> event, String message) {
        return "Tenant ID [" + new TokenManager().getTenantId(event) + "] " + message;
    }
}