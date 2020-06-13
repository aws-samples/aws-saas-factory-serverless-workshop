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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TenantService implements RequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(TenantService.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static Map<String, String> CORS = Stream
            .of(new AbstractMap.SimpleEntry<String, String>("Access-Control-Allow-Origin", "*"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private final TenantServiceDAL dal = new TenantServiceDAL();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(Map<String, Object> event, Context context) {
        return getTenants(event, context);
    }

    public APIGatewayProxyResponseEvent getTenants(Map<String, Object> event, Context context) {
        Map<String, String> queryParams = (Map<String, String>) event.get("queryStringParameters");
        if (queryParams != null && "warmup".equals(queryParams.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        } else if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantService::getTenants");
        List<Tenant> tenants = dal.getTenants();
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(tenants));
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::getTenants exec " + totalTimeMillis);
        return response;
    }

    public APIGatewayProxyResponseEvent getTenant(Map<String, Object> event, Context context) {
        Map<String, String> queryParams = (Map<String, String>) event.get("queryStringParameters");
        if (queryParams != null && "warmup".equals(queryParams.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        } else if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        //logRequestEvent(event);
        Map<String, String> params = (Map) event.get("pathParameters");
        String tenantId = params.get("id");
        LOGGER.info("TenantService::getTenant " + tenantId);
        Tenant tenant = dal.getTenant(tenantId);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(tenant));
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::getTenant exec " + totalTimeMillis);
        return response;
    }

    public APIGatewayProxyResponseEvent insertTenant(Map<String, Object> event, Context context) {
        //logRequestEvent(event);
        if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        } else if (event.containsKey("body")) {
            try {
                Map<String, String> requestBody = MAPPER.readValue((String) event.get("body"), HashMap.class);
                if (requestBody != null && "warmup".equals(requestBody.get("source"))) {
                    LOGGER.info("Warming up");
                    return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
                }
            } catch (IOException e) {
            }
        }

        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantService::insertTenant");
        APIGatewayProxyResponseEvent response = null;
        Map<String, String> params = (Map) event.get("pathParameters");
        Tenant tenant = fromJson((String) event.get("body"));
        if (tenant == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            tenant = dal.insertTenant(tenant);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
                    .withBody(toJson(tenant));
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::insertTenant exec " + totalTimeMillis);
        return response;
    }

    public APIGatewayProxyResponseEvent updateTenant(Map<String, Object> event, Context context) {
        if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        //logRequestEvent(event);
        APIGatewayProxyResponseEvent response = null;
        LOGGER.info("TenantService::updateTenant");
        Map<String, String> params = (Map) event.get("pathParameters");
        String tenantId = params.get("id");
        LOGGER.info("TenantService::updateTenant " + tenantId);
        Tenant tenant = fromJson((String) event.get("body"));
        if (tenant == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            if (tenant.getId() == null || !tenant.getId().toString().equals(tenantId)) {
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400);
            } else {
                tenant = dal.updateTenant(tenant);
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withHeaders(CORS)
                        .withBody(toJson(tenant));
            }
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::updateTenant exec " + totalTimeMillis);
        return response;
    }

    public APIGatewayProxyResponseEvent deleteTenant(Map<String, Object> event, Context context) {
        if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        //logRequestEvent(event);
        APIGatewayProxyResponseEvent response = null;
        LOGGER.info("TenantService::deleteTenant");
        Map<String, String> params = (Map) event.get("pathParameters");
        String tenantId = params.get("id");
        LOGGER.info("TenantService::deleteTenant " + tenantId);
        Tenant tenant = fromJson((String) event.get("body"));
        if (tenant == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            if (tenant.getId() == null || !tenant.getId().toString().equals(tenantId)) {
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400);
            } else {
                dal.deleteTenant(tenantId);
                //TODO remove Cognito UserPool
                //TODO remove tenant's parameters from SSM
                //TODO delete onboarding CFN stack
                response = new APIGatewayProxyResponseEvent()
                        .withHeaders(CORS)
                        .withStatusCode(200);
            }
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::deleteTenant exec " + totalTimeMillis);
        return response;
    }

    /**
     * <b>Not</b> thread-safe! - just throw-away sample code to avoid the delay in provisioning
     * an RDS cluster when registering a tenant during the workshop.
     * @param event
     * @param context
     * @return
     */
    public APIGatewayProxyResponseEvent nextAvailableDatabase(Map<String, Object> event, Context context) {
        Map<String, String> queryParams = (Map<String, String>) event.get("queryStringParameters");
        if (queryParams != null && "warmup".equals(queryParams.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        } else if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        APIGatewayProxyResponseEvent response = null;
        LOGGER.info("TenantService::nextAvailableDatabase");
        Map<String, String> rds = dal.nextAvailableDatabase();
        response = new APIGatewayProxyResponseEvent()
                .withBody(toJson(rds))
                .withHeaders(CORS)
                .withStatusCode(200);
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::nextAvailableDatabase exec " + totalTimeMillis);
        return response;
    }

    public APIGatewayProxyResponseEvent updateDatabase(Map<String, Object> event, Context context) {
        if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        APIGatewayProxyResponseEvent response = null;
        LOGGER.info("TenantService::updateDatabase");
        Map<String, String> params = (Map) event.get("pathParameters");
        String tenantId = params.get("id");
        LOGGER.info("TenantService::updateDatabase " + tenantId);
        Tenant tenant = fromJson((String) event.get("body"));
        if (tenant == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            if (tenant.getId() == null || !tenant.getId().toString().equals(tenantId)
                    || tenant.getDatabase() == null || tenant.getDatabase().isEmpty()) {
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400);
            } else {
                tenant = dal.updateDatabase(tenant);
                response = new APIGatewayProxyResponseEvent()
                        .withBody(toJson(tenant))
                        .withHeaders(CORS)
                        .withStatusCode(200);
            }
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::updateDatabase exec " + totalTimeMillis);
        return response;
    }

    public APIGatewayProxyResponseEvent updateUserPool(Map<String, Object> event, Context context) {
        if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        }

        long startTimeMillis = System.currentTimeMillis();
        APIGatewayProxyResponseEvent response = null;
        LOGGER.info("TenantService::updateUserPool");
        Map<String, String> params = (Map) event.get("pathParameters");
        String tenantId = params.get("id");
        LOGGER.info("TenantService::updateUserPool " + tenantId);
        Tenant tenant = fromJson((String) event.get("body"));
        if (tenant == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            if (tenant.getId() == null || !tenant.getId().toString().equals(tenantId)
                    || tenant.getUserPool() == null || tenant.getUserPool().isEmpty()) {
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400);
            } else {
                tenant = dal.updateUserPool(tenant);
                response = new APIGatewayProxyResponseEvent()
                        .withBody(toJson(tenant))
                        .withHeaders(CORS)
                        .withStatusCode(200);
            }
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantService::updateUserPool exec " + totalTimeMillis);
        return response;
    }

    private static Tenant fromJson(String json) {
        Tenant tenant = null;
        try {
            tenant = MAPPER.readValue(json, Tenant.class);
        } catch (IOException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return tenant;
    }

    private static String toJson(Object obj) {
        String json = null;
        try {
            json = MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return json;
    }

    private static void logRequestEvent(Map<String, Object> event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            LOGGER.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not log request event " + e.getMessage());
        }
    }

    private static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}