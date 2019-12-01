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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TenantServiceDAL {

    private final static Logger LOGGER = LoggerFactory.getLogger(TenantServiceDAL.class);
    private final static String TENANT_TABLE = "saas-factory-srvls-wrkshp-tenants";
    private DynamoDbClient ddb;

    public TenantServiceDAL() {
        this.ddb = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public List<Tenant> getTenants() {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::getTenants");
        List<Tenant> tenants = new ArrayList<>();
        try {
            ScanResponse response = ddb.scan(request -> request.tableName(TENANT_TABLE));
            response.items().forEach(item ->
                    tenants.add(fromAttributeValueMap(item))
            );
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::getTenants " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::getTenants exec " + totalTimeMillis);
        return tenants;
    }

    public Tenant getTenant(UUID tenantId) {
        return getTenant(tenantId.toString());
    }

    public Tenant getTenant(String tenantId) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::getTenant");
        Map<String, AttributeValue> item = null;
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(tenantId).build());
            GetItemResponse response = ddb.getItem(request -> request.tableName(TENANT_TABLE).key(key));
            item = response.item();
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::getTenant " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::getTenant exec " + totalTimeMillis);
        return fromAttributeValueMap(item);
    }

    public Tenant insertTenant(Tenant tenant) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::insertTenant");
        UUID tenantId = UUID.randomUUID();
        tenant.setId(tenantId);
        tenant.setActive(Boolean.TRUE);
        try {
            Map<String, AttributeValue> item = toAttributeValueMap(tenant);
            PutItemResponse response = ddb.putItem(request -> request.tableName(TENANT_TABLE).item(item));
            long putItemTimeMillis = System.currentTimeMillis() - startTimeMillis;
            LOGGER.info("TenantServiceDAL::insertTenant PutItem exec " + putItemTimeMillis);

            // Bit of a hack here to keep our record keeping of RDS clusters up-to-date
            if (tenant.getDatabase() != null && !tenant.getDatabase().isEmpty()) {
                long scanStartTimeMillis = System.currentTimeMillis();
                ScanResponse scan = ddb.scan(request -> request
                        .tableName("saas-factory-srvls-wrkshp-rds-clusters")
                        .filterExpression("Endpoint = :host")
                        .expressionAttributeValues(Stream
                                .of(new AbstractMap.SimpleEntry<String, AttributeValue>(":host", AttributeValue.builder().s(tenant.getDatabase()).build()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                        )
                );
                long scanTimeMillis = System.currentTimeMillis() - scanStartTimeMillis;
                LOGGER.info("TenantServiceDAL::insertTenant Scan exec " + scanTimeMillis);

                if (!scan.items().isEmpty()) {
                    long updateItemStartTimeMillis = System.currentTimeMillis();
                    Map<String, AttributeValue> rdsHousekeepingItem = scan.items().get(0);
                    Map<String, AttributeValue> updateKey = new HashMap<>();
                    updateKey.put("DBClusterIdentifier", AttributeValue.builder().s(rdsHousekeepingItem.get("DBClusterIdentifier").s()).build());
                    ddb.updateItem(request -> request
                            .tableName("saas-factory-srvls-wrkshp-rds-clusters")
                            .key(updateKey)
                            .updateExpression("SET TenantId = :tenantId")
                            .expressionAttributeValues(Stream
                                    .of(new AbstractMap.SimpleEntry<String, AttributeValue>(":tenantId", AttributeValue.builder().s(tenant.getId().toString()).build()))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                            )
                    );
                    long updateItemTimeMills = System.currentTimeMillis() - updateItemStartTimeMillis;
                    LOGGER.info("TenantServiceDAL::insertTenant UpdateItem exec " + scanTimeMillis);
                }
            }
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::insertTenant " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::insertTenant exec " + totalTimeMillis);
        return tenant;
    }

    // Choosing to do a replacement update as you might do in a RDBMS by
    // setting columns = NULL when they do not exist in the updated value
    public Tenant updateTenant(Tenant tenant) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::updateTenant");
        try {
            Map<String, AttributeValue> item = toAttributeValueMap(tenant);
            PutItemResponse response = ddb.putItem(request -> request.tableName(TENANT_TABLE).item(item));
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::updateTenant " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::updateTenant exec " + totalTimeMillis);
        return tenant;
    }

    public Tenant updateDatabase(Tenant tenant) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::updateDatabase");
        Tenant updated = tenant;
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(tenant.getId().toString()).build());
            UpdateItemResponse response = ddb.updateItem(request -> request
                    .tableName(TENANT_TABLE)
                    .key(key)
                    .updateExpression("SET database = :db")
                    .expressionAttributeValues(Stream
                            .of(new AbstractMap.SimpleEntry<String, AttributeValue>(":db", AttributeValue.builder().s(tenant.getDatabase()).build()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    )
                    .returnValues(ReturnValue.ALL_NEW)
            );
            updated = fromAttributeValueMap(response.attributes());
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::updateDatabase " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::updateDatabase exec " + totalTimeMillis);
        return updated;
    }

    public Tenant updateUserPool(Tenant tenant) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::updateUserPool");
        Tenant updated = tenant;
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(tenant.getId().toString()).build());
            UpdateItemResponse response = ddb.updateItem(request -> request
                    .tableName(TENANT_TABLE)
                    .key(key)
                    .updateExpression("SET userPool = :up")
                    .expressionAttributeValues(Stream
                            .of(new AbstractMap.SimpleEntry<String, AttributeValue>(":up", AttributeValue.builder().s(tenant.getUserPool()).build()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    )
                    .returnValues(ReturnValue.ALL_NEW)
            );
            updated = fromAttributeValueMap(response.attributes());
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::updateUserPool " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::updateUserPool exec " + totalTimeMillis);
        return updated;
    }

    public void deleteTenant(UUID tenantId) {
        deleteTenant(tenantId.toString());
    }

    public void deleteTenant(String tenantId) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::deleteTenant");
        try {
            //TODO remove TenantId from saas-factory-srvls-wrkshp-rds-clusters
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(tenantId).build());
            DeleteItemResponse response = ddb.deleteItem(request -> request.tableName(TENANT_TABLE).key(key));
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::deleteTenant " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::deleteTenant exec " + totalTimeMillis);
        return;
    }

    public Map<String, String> nextAvailableDatabase() {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("TenantServiceDAL::nextAvailableDatabase");
        Map<String, String> availableDatabase = new HashMap<>();
        try {
            // Get the records from our pool management table that haven't
            // been assigned to a tenant yet
            ScanResponse response = ddb.scan(request -> request
                    .tableName("saas-factory-srvls-wrkshp-rds-clusters")
                    .filterExpression("attribute_not_exists(TenantId)")
            );
            if (!response.items().isEmpty()) {
                Map<String, AttributeValue> item = response.items().get(0);
                availableDatabase.put("DBClusterIdentifier", item.get("DBClusterIdentifier").s());
                availableDatabase.put("Endpoint", item.get("Endpoint").s());
            }
        } catch (DynamoDbException e) {
            LOGGER.error("TenantServiceDAL::nextAvailableDatabase " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("TenantServiceDAL::nextAvailableDatabase exec " + totalTimeMillis);
        return availableDatabase;
    }

    private static Map<String, AttributeValue> toAttributeValueMap(Tenant tenant) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(tenant.getId().toString()).build());
        if (tenant.getActive() != null) {
            item.put("active", AttributeValue.builder().bool(tenant.getActive()).build());
        }
        if (tenant.getCompanyName() != null && !tenant.getCompanyName().isEmpty()) {
            item.put("companyName", AttributeValue.builder().s(tenant.getCompanyName()).build());
        }
        if (tenant.getPlan() != null && !tenant.getPlan().isEmpty()) {
            item.put("plan", AttributeValue.builder().s(tenant.getPlan()).build());
        }
        if (tenant.getUserPool() != null && !tenant.getUserPool().isEmpty()) {
            item.put("userPool", AttributeValue.builder().s(tenant.getUserPool()).build());
        }
        if (tenant.getDatabase() != null && !tenant.getDatabase().isEmpty()) {
            item.put("database", AttributeValue.builder().s(tenant.getDatabase()).build());
        }
        return item;
    }

    private static Tenant fromAttributeValueMap(Map<String, AttributeValue> item) {
        Tenant tenant = null;
        if (item != null) {
            tenant = new Tenant();
            if (item.containsKey("id")) {
                tenant.setId(UUID.fromString(item.get("id").s()));
            }
            if (item.containsKey("active")) {
                tenant.setActive(item.get("active").bool());
            }
            if (item.containsKey("companyName")) {
                tenant.setCompanyName(item.get("companyName").s());
            }
            if (item.containsKey("plan")) {
                tenant.setPlan(item.get("plan").s());
            }
            if (item.containsKey("userPool")) {
                tenant.setUserPool(item.get("userPool").s());
            }
            if (item.containsKey("database")) {
                tenant.setDatabase(item.get("database").s());
            }
        }
        return tenant;
    }

    private static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
