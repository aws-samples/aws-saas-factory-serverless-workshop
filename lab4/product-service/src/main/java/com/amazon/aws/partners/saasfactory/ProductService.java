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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProductService implements RequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductService.class);
    private final static ProductServiceDAL DAL = new ProductServiceDAL();
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static Map<String, String> CORS = Stream
            .of(new AbstractMap.SimpleEntry<String, String>("Access-Control-Allow-Origin", "*"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public APIGatewayProxyResponseEvent handleRequest(Map<String, Object> event, Context context) {
        return getProducts(event, context);
    }

    public APIGatewayProxyResponseEvent getProducts(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::getProducts");
        List<Product> products = DAL.getProducts(event);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(products));
        return response;
    }

    public APIGatewayProxyResponseEvent getProduct(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::getProduct");
        Map<String, String> params = (Map) event.get("pathParameters");
        Integer productId = Integer.valueOf(params.get("id"));

        Product product = DAL.getProduct(event, productId);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(product));
        return response;
    }

    public APIGatewayProxyResponseEvent updateProduct(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::updateProduct");
        Map<String, String> params = (Map) event.get("pathParameters");
        Integer productId = Integer.valueOf(params.get("id"));
        Product product = productFromJson((String) event.get("body"));
        APIGatewayProxyResponseEvent response = null;
        if (product == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else if (product.getId() != productId) {
            LoggingManager.log(event, "ProductService::updateProduct product.id does not match resource path id");
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("product.id does not match resource path id");
        } else {
            product = DAL.updateProduct(event, product);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
                    .withBody(toJson(product));
        }
        return response;
    }

    public APIGatewayProxyResponseEvent insertProduct(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::insertProduct");
        APIGatewayProxyResponseEvent response = null;
        Product product = productFromJson((String) event.get("body"));
        if (product == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            product = DAL.insertProduct(event, product);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
                    .withBody(toJson(product));
        }
        return response;
    }

    public APIGatewayProxyResponseEvent deleteProduct(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::deleteProduct");
        Map<String, String> params = (Map) event.get("pathParameters");
        Integer productId = Integer.valueOf(params.get("id"));
        APIGatewayProxyResponseEvent response = null;
        Product product = productFromJson((String) event.get("body"));
        if (product == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else if (product.getId() != productId) {
            LoggingManager.log(event, "ProductService::deleteProduct product.id does not match resource path id");
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("product.id does not match resource path id");
        } else {
            product = DAL.deleteProduct(event, product);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
            .withBody(toJson(product));
        }
        return response;
    }

    public APIGatewayProxyResponseEvent getCategories(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::getCategories");
        List<Category> categories = DAL.getCategories(event);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(categories));
        return response;
    }

    public APIGatewayProxyResponseEvent getCategory(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::getCategory");
        Map<String, String> params = (Map) event.get("pathParameters");
        Integer categoryId = Integer.valueOf(params.get("id"));

        Category category = DAL.getCategory(event, categoryId);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(CORS)
                .withBody(toJson(category));
        return response;
    }

    public APIGatewayProxyResponseEvent updateCategory(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::updateCategory");
        Map<String, String> params = (Map) event.get("pathParameters");
        Integer categoryId = Integer.valueOf(params.get("id"));
        Category category = categoryFromJson((String) event.get("body"));
        APIGatewayProxyResponseEvent response = null;
        if (category == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else if (category.getId() != categoryId) {
            LoggingManager.log(event, "ProductService::updateCategory category.id does not match resource path id");
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("category.id does not match resource path id");
        } else {
            category = DAL.updateCategory(event, category);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
                    .withBody(toJson(category));
        }
        return response;
    }

    public APIGatewayProxyResponseEvent insertCategory(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::insertCategory");
        APIGatewayProxyResponseEvent response = null;
        Category category = categoryFromJson((String) event.get("body"));
        if (category == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else {
            category = DAL.insertCategory(event, category);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
                    .withBody(toJson(category));
        }
        return response;
    }

    public APIGatewayProxyResponseEvent deleteCategory(Map<String, Object> event, Context context) {
        LoggingManager.log(event, "ProductService::deleteCategory");
        Map<String, String> params = (Map) event.get("pathParameters");
        Integer categoryId = Integer.valueOf(params.get("id"));
        APIGatewayProxyResponseEvent response = null;
        Category category = categoryFromJson((String) event.get("body"));
        if (category == null) {
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400);
        } else if (category.getId() != categoryId) {
            LoggingManager.log(event, "ProductService::deleteCategory category.id does not match resource path id");
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("category.id does not match resource path id");
        } else {
            category = DAL.deleteCategory(event, category);
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(CORS)
            .withBody(toJson(category));
        }
        return response;
    }

    public static String toJson(Object obj) {
        String json = null;
        try {
            json = MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return json;
    }

    public static Product productFromJson(String json) {
        Product product = null;
        try {
            product = MAPPER.readValue(json, Product.class);
        } catch (IOException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return product;
    }

    public static Category categoryFromJson(String json) {
        Category category = null;
        try {
            category = MAPPER.readValue(json, Category.class);
        } catch (IOException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return category;
    }

    public static void logRequestEvent(Map<String, Object> event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            LOGGER.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not log request event " + e.getMessage());
        }
    }

    public static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}