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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderService implements RequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private final static OrderServiceDAL DAL = new OrderServiceDAL();
    private final static ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    private final static Map<String, String> CORS = Stream
            .of(new AbstractMap.SimpleEntry<String, String>("Access-Control-Allow-Origin", "*"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public APIGatewayProxyResponseEvent handleRequest(Map<String, Object> event, Context context) {
        return getOrders(event, context);
    }

    public APIGatewayProxyResponseEvent getOrders(Map<String, Object> event, Context context) {
        LOGGER.info("OrderService::getOrders");
        List<Order> orders = DAL.getOrders(event);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(orders));
        return response;
    }

    public APIGatewayProxyResponseEvent getOrder(Map<String, Object> event, Context context) {
        Map<String, String> params = (Map) event.get("pathParameters");
        String orderId = params.get("id");
        LOGGER.info("OrderService::getOrder " + orderId);

        Order order = DAL.getOrder(event, orderId);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(order));
        return response;
    }

    public APIGatewayProxyResponseEvent updateOrder(Map<String, Object> event, Context context) {
        LOGGER.info("OrderService::updateOrder");
        APIGatewayProxyResponseEvent response = null;
        Map<String, String> params = (Map) event.get("pathParameters");
        String orderId = params.get("id");
        LOGGER.info("OrderService::updateOrder " + orderId);
        Order order = fromJson((String) event.get("body"));
        if (order == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            if (order.getId() == null || !order.getId().toString().equals(orderId)) {
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400);
            } else {
                order = DAL.updateOrder(event, order);
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withHeaders(CORS)
                        .withBody(toJson(order));
            }
        }
        return response;
    }

    public APIGatewayProxyResponseEvent insertOrder(Map<String, Object> event, Context context) {
        LOGGER.info("OrderService::insertOrder");

        APIGatewayProxyResponseEvent response = null;
        Order order = fromJson((String) event.get("body"));
        if (order == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            // TODO Add code to call DAL.insertOrder here and return a 200
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withHeaders(CORS);
        }
        return response;
    }

    public APIGatewayProxyResponseEvent deleteOrder(Map<String, Object> event, Context context) {
        LOGGER.info("OrderService::deleteOrder");
        APIGatewayProxyResponseEvent response = null;
        Map<String, String> params = (Map) event.get("pathParameters");
        String orderId = params.get("id");
        LOGGER.info("OrderService::deleteOrder " + orderId);
        Order order = fromJson((String) event.get("body"));
        if (order == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            if (order.getId() == null || !order.getId().toString().equals(orderId)) {
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400);
            } else {
                DAL.deleteOrder(event, orderId);
                response = new APIGatewayProxyResponseEvent()
                        .withHeaders(CORS)
                        .withStatusCode(200);
            }
        }
        return response;
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

    public static Order fromJson(String json) {
        Order order = null;
        try {
            order = MAPPER.readValue(json, Order.class);
        } catch (IOException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return order;
    }

    private static void logRequestEvent(Map<String, Object> event) {
        try {
            LOGGER.info(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(event));
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not log request event " + getFullStackTrace(e));
        }
    }

    private static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}