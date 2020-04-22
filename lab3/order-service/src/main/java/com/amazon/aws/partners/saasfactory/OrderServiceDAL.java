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
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class OrderServiceDAL {

    private final static Logger LOGGER = LoggerFactory.getLogger(OrderServiceDAL.class);
    private final Map<String, String> tenantTableCache = new HashMap<>();
    private DynamoDbClient ddb;

    public OrderServiceDAL() {
        this.ddb = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public List<Order> getOrders(Map<String, Object> event) {
        LOGGER.info("OrderServiceDAL::getOrders");

        List<Order> orders = new ArrayList<>();
        try {
            ScanResponse response = ddb.scan(request -> request.tableName(tableName(event)));
            response.items().forEach(item ->
                    orders.add(DynamoDbHelper.orderFromAttributeValueMap(item))
            );
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::getTenants " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        return orders;
    }

    public Order getOrder(Map<String, Object> event, UUID orderId) {
        return getOrder(event, orderId.toString());
    }

    public Order getOrder(Map<String, Object> event, String orderId) {
        LOGGER.info("OrderServiceDAL::getOrder " + orderId);

        Order order = null;
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(orderId).build());
            GetItemResponse response = ddb.getItem(request -> request.tableName(tableName(event)).key(key));
            Map<String, AttributeValue> item = response.item();
            order = DynamoDbHelper.orderFromAttributeValueMap(item);
        } catch (DynamoDbException e) {
            LOGGER.error("OrderServiceDAL::getOrder " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        return order;
    }

    // Choosing to do a replacement update as you might do in a RDBMS by
    // setting columns = NULL when they do not exist in the updated value
    public Order updateOrder(Map<String, Object> event, Order order) {
        LOGGER.info("OrderServiceDAL::updateOrder");
        try {
            Map<String, AttributeValue> item = DynamoDbHelper.toAttributeValueMap(order);
            PutItemResponse response = ddb.putItem(request -> request.tableName(tableName(event)).item(item));
        } catch (DynamoDbException e) {
            LOGGER.error("OrderServiceDAL::updateOrder " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        return order;
    }

    public Order insertOrder(Map<String, Object> event, Order order) {
        UUID orderId = UUID.randomUUID();
        LOGGER.info("OrderServiceDAL::insertOrder " + orderId);
        order.setId(orderId);

        try {
            Map<String, AttributeValue> item = DynamoDbHelper.toAttributeValueMap(order);
            String tableName = tableName(event);

            PutItemResponse response = ddb.putItem(request -> request.tableName(tableName).item(item));
        } catch (DynamoDbException e) {
            LOGGER.error("OrderServiceDAL::insertOrder " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }

        return order;
    }

    public void deleteOrder(Map<String, Object> event, UUID orderId) {
        deleteOrder(event, orderId.toString());
    }

    public void deleteOrder(Map<String, Object> event, String orderId) {
        LOGGER.info("OrderServiceDAL::deleteOrder");
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(orderId).build());
            DeleteItemResponse response = ddb.deleteItem(request -> request.tableName(tableName(event)).key(key));
        } catch (DynamoDbException e) {
            LOGGER.error("deleteOrder::deleteOrder " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        return;
    }

    private String tableName(Map<String, Object> event) {
        String tenantId = new TokenManager().getTenantId(event);
        String tableName = "order_fulfillment_" + tenantId;

        if (!tenantTableCache.containsKey(tenantId) || !tenantTableCache.get(tenantId).equals(tableName)) {
            boolean exits = false;
            ListTablesResponse response = ddb.listTables();
            for (String table : response.tableNames()) {
                if (table.equals(tableName)) {
                    exits = true;
                    break;
                }
            }
            if (!exits) {
                CreateTableResponse createTable = ddb.createTable(request -> request
                        .tableName(tableName)
                        .attributeDefinitions(AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build())
                        .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                        .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build())
                );
                waitForActive(tableName);
            }
            tenantTableCache.put(tenantId, tableName);
        }

        return tenantTableCache.get(tenantId);
    }

    private void waitForActive(String tableName) {
        int max = 12;
        DescribeTableResponse tableDescription = ddb.describeTable(request -> request.tableName(tableName));
        TableStatus tableStatus = tableDescription.table().tableStatus();
        LOGGER.debug(String.format("%d %s table status %s", max, tableDescription.responseMetadata().requestId(), tableStatus.toString()));
        while (TableStatus.ACTIVE != tableStatus && --max > 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                LOGGER.error(getFullStackTrace(ie));
            }
            tableDescription = ddb.describeTable(request -> request.tableName(tableName));
            tableStatus = tableDescription.table().tableStatus();
            LOGGER.debug(String.format("%d %s table status %s", max, tableDescription.responseMetadata().requestId(), tableStatus.toString()));
        }
    }

    private static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}