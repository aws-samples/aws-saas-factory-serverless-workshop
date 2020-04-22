/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BootstrapRDS implements RequestHandler<Map<String, Object>, Object> {

    private final static String RDS_HOT_POOL_TABLE = "saas-factory-srvls-wrkshp-rds-clusters";
    private SsmClient ssm;
    private DynamoDbClient ddb;

    public BootstrapRDS() {
        this.ssm = SsmClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
        this.ddb = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.log(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
            logger.log("\n");
        } catch (JsonProcessingException e) {
            logger.log("Could not log input\n");
        }

        final String requestType = (String) input.get("RequestType");
        Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");
        final String dbMasterUser = (String) resourceProperties.get("RDSMasterUsername");
        final String dbMasterPass = (String) resourceProperties.get("RDSMasterPassword");
        final String dbAppUser = (String) resourceProperties.get("RDSAppUsername");
        final String dbAppPass = (String) resourceProperties.get("RDSAppPassword");
        final String dbHost = (String) resourceProperties.get("RDSClusterEndpoint");
        final String dbDatabase = (String) resourceProperties.get("RDSDatabase");
        final String clusterId = (String) resourceProperties.get("RDSCluster");
        final String tenantId = (String) resourceProperties.get("TenantId");

        ExecutorService service = Executors.newSingleThreadExecutor();

        ObjectNode responseData = JsonNodeFactory.instance.objectNode();
        try {
            if (requestType == null) {
                throw new RuntimeException();
            }

            Runnable r = () -> {
                GetParameterResponse ssmResponse = ssm.getParameter(request -> request
                        .name(dbMasterPass)
                        .withDecryption(Boolean.TRUE)
                );
                String decryptedMasterPassword = ssmResponse.parameter().value();

                Properties masterConnectionProperties = new Properties();
                masterConnectionProperties.put("user", dbMasterUser);
                masterConnectionProperties.put("password", decryptedMasterPassword);

                String jdbcUrl = "jdbc:postgresql://" + dbHost + ":5432/" + dbDatabase;

                if ("Create".equalsIgnoreCase(requestType)) {
                    logger.log("CREATE\n");

                    // Create the initial table schema. The RDS master user will be
                    // the owner of these tables.
                    try (Connection connection = DriverManager.getConnection(jdbcUrl, masterConnectionProperties);
                            Statement sql = connection.createStatement()) {
                        connection.setAutoCommit(false);
                        InputStream bootstrapSql = Thread.currentThread().getContextClassLoader().getResourceAsStream("bootstrap.sql");
                        Scanner bootstrapSqlScanner = new Scanner(bootstrapSql, "UTF-8");
                        bootstrapSqlScanner.useDelimiter(";");
                        while (bootstrapSqlScanner.hasNext()) {
                            String ddl = bootstrapSqlScanner.next();
                            sql.addBatch(ddl);
                        }
                        sql.executeBatch();
                        connection.commit();

                        // Add some mock data to the tables for the lab 1 "monolith" user
                        if ("MONOLITH".equalsIgnoreCase(tenantId)) {
                            InputStream dataSql = Thread.currentThread().getContextClassLoader().getResourceAsStream("data.sql");
                            Scanner dataSqlScanner = new Scanner(dataSql, "UTF-8");
                            dataSqlScanner.useDelimiter(";");
                            while (dataSqlScanner.hasNext()) {
                                String inserts = dataSqlScanner.next();
                                sql.addBatch(inserts);
                            }
                            sql.executeBatch();
                            connection.commit();
                        }

                        // Use the RDS master user to create a new read/write non-root
                        // user for our app to access the database as.
                        if (dbAppUser != null && !dbAppUser.isEmpty() && dbAppPass != null && !dbAppPass.isEmpty()) {
                            GetParameterResponse response = ssm.getParameter(request -> request
                                    .name(dbAppPass)
                                    .withDecryption(Boolean.TRUE)
                            );
                            String decryptedAppPassword = response.parameter().value();

                            InputStream userSql = Thread.currentThread().getContextClassLoader().getResourceAsStream("user.sql");
                            Scanner userSqlScanner = new Scanner(userSql, "UTF-8");
                            userSqlScanner.useDelimiter(";");
                            while (userSqlScanner.hasNext()) {
                                // Simple variable replacement in the SQL
                                String ddl = userSqlScanner.next()
                                        .replace("{{DB_APP_USER}}", dbAppUser)
                                        .replace("{{DB_APP_PASS}}", decryptedAppPassword);
                                sql.addBatch(ddl);
                            }
                            sql.executeBatch();
                            connection.commit();
                        }

                        // Keep a list of bootstrapped databases for use in Lab 2
                        // Convenient to do it here because this Lambda is already
                        // a VPC function and is called from CloudFormation after
                        // creating the RDS instances
                        Map<String, AttributeValue> item = new HashMap<>();
                        item.put("DBClusterIdentifier", AttributeValue.builder().s(clusterId).build());
                        item.put("Endpoint", AttributeValue.builder().s(dbHost).build());
                        if (tenantId != null && !tenantId.isEmpty()) {
                            item.put("TenantId", AttributeValue.builder().s(tenantId).build());
                        }
                        try {
                            logger.log("Inserting database cluster in hot pool " + clusterId);
                            PutItemResponse response = ddb.putItem(request -> request.tableName(RDS_HOT_POOL_TABLE).item(item));
                        } catch (DynamoDbException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Update".equalsIgnoreCase(requestType)) {
                    logger.log("UDPATE\n");
                    // TODO: Is there really any logical update process here?
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Delete".equalsIgnoreCase(requestType)) {
                    logger.log("DELETE\n");
                    // TODO: Do we dare drop the database here?
                    sendResponse(input, context, "SUCCESS", responseData);
                } else {
                    logger.log("FAILED unknown requestType " + requestType + "\n");
                    responseData.put("Reason", "Unknown RequestType " + requestType);
                    sendResponse(input, context, "FAILED", responseData);
                }
            };
            Future<?> f = service.submit(r);
            f.get(context.getRemainingTimeInMillis() - 1000, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException | InterruptedException | ExecutionException e) {
            // Timed out or unexpected exception
            logger.log("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
            // Print the full stack trace
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            logger.log(sw.getBuffer().toString() + "\n");

            responseData.put("Reason", "Request timed out");
            sendResponse(input, context, "FAILED", responseData);
        } finally {
            service.shutdown();
        }

        return null;
    }

    public Object addApplicationUser(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        final String requestType = (String) input.get("RequestType");
        Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");

        final String tenantId = (String) resourceProperties.get("TenantId");

        ExecutorService service = Executors.newSingleThreadExecutor();

        ObjectNode responseData = JsonNodeFactory.instance.objectNode();
        try {
            if (requestType == null) {
                throw new RuntimeException();
            }

            Runnable r = () -> {
                if ("Create".equalsIgnoreCase(requestType)) {
                    final String dbMasterUsername = "master";
                    final String dbMasterPassParam = "saas-factory-srvls-wrkshp-owner-pw";
                    if (tenantId != null && !tenantId.isEmpty()) {
                        final String dbHostParam = tenantId + "_DB_HOST";
                        final String dbUserParam = tenantId + "_DB_USER";
                        final String dbPassParam = tenantId + "_DB_PASS";
                        final String dbNameParam = tenantId + "_DB_NAME";
                        GetParametersResponse ssmResponse = ssm.getParameters(request -> request
                                .names(dbMasterPassParam, dbHostParam, dbUserParam, dbPassParam, dbNameParam)
                                .withDecryption(Boolean.TRUE)
                        );

                        Map<String, String> params = ssmResponse.parameters()
                                .stream()
                                .map(p -> new AbstractMap.SimpleEntry<String, String>(p.name(), p.value()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        Properties masterConnectionProperties = new Properties();
                        masterConnectionProperties.put("user", dbMasterUsername);
                        masterConnectionProperties.put("password", params.get(dbMasterPassParam));

                        String jdbcUrl = "jdbc:postgresql://" + params.get(dbHostParam) + ":5432/" + params.get(dbNameParam);
                        try (Connection connection = DriverManager.getConnection(jdbcUrl, masterConnectionProperties);
                             Statement sql = connection.createStatement()) {
                            connection.setAutoCommit(false);
                            InputStream userSql = Thread.currentThread().getContextClassLoader().getResourceAsStream("user.sql");
                            Scanner userSqlScanner = new Scanner(userSql, "UTF-8");
                            userSqlScanner.useDelimiter(";");
                            while (userSqlScanner.hasNext()) {
                                // Simple variable replacement in the SQL
                                String ddl = userSqlScanner.next()
                                        .replace("{{DB_APP_USER}}", params.get(dbUserParam))
                                        .replace("{{DB_APP_PASS}}", params.get(dbPassParam));
                                sql.addBatch(ddl);
                            }
                            sql.executeBatch();
                            connection.commit();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        sendResponse(input, context, "SUCCESS", responseData);
                    }
                } else if ("Update".equalsIgnoreCase(requestType)) {
                    logger.log("UDPATE\n");
                    // TODO: Should we really support updating a login role name or password?
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Delete".equalsIgnoreCase(requestType)) {
                    logger.log("DELETE\n");
                    // TODO: Do we dare delete the user here?
                    sendResponse(input, context, "SUCCESS", responseData);
                } else {
                    logger.log("FAILED unknown requestType " + requestType + "\n");
                    responseData.put("Reason", "Unknown RequestType " + requestType);
                    sendResponse(input, context, "FAILED", responseData);
                }
            };
            Future<?> f = service.submit(r);
            f.get(context.getRemainingTimeInMillis() - 1000, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException | InterruptedException | ExecutionException e) {
            // Timed out or unexpected exception
            logger.log("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
            // Print the full stack trace
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            logger.log(sw.getBuffer().toString() + "\n");

            responseData.put("Reason", "Request timed out");
            sendResponse(input, context, "FAILED", responseData);
        } finally {
            service.shutdown();
        }

        return null;
    }

    public Object bootstrapPool(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.log(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
            logger.log("\n");
        } catch (JsonProcessingException e) {
            logger.log("Could not log input\n");
        }

        final String requestType = (String) input.get("RequestType");
        Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");
        final String dbMasterUser = (String) resourceProperties.get("RDSMasterUsername");
        final String dbMasterPass = (String) resourceProperties.get("RDSMasterPassword");
        final String dbAppUser = (String) resourceProperties.get("RDSAppUsername");
        final String dbAppPass = (String) resourceProperties.get("RDSAppPassword");
        final String dbHost = (String) resourceProperties.get("RDSClusterEndpoint");
        final String dbDatabase = (String) resourceProperties.get("RDSDatabase");

        ExecutorService service = Executors.newSingleThreadExecutor();

        ObjectNode responseData = JsonNodeFactory.instance.objectNode();
        try {
            if (requestType == null) {
                throw new RuntimeException();
            }

            Runnable r = () -> {
                GetParameterResponse ssmResponse = ssm.getParameter(request -> request
                        .name(dbMasterPass)
                        .withDecryption(Boolean.TRUE)
                );
                String decryptedMasterPassword = ssmResponse.parameter().value();

                GetParameterResponse response = ssm.getParameter(request -> request
                        .name(dbAppPass)
                        .withDecryption(Boolean.TRUE)
                );
                String decryptedAppPassword = response.parameter().value();

                Properties masterConnectionProperties = new Properties();
                masterConnectionProperties.put("user", dbMasterUser);
                masterConnectionProperties.put("password", decryptedMasterPassword);

                String jdbcUrl = "jdbc:postgresql://" + dbHost + ":5432/" + dbDatabase;

                if ("Create".equalsIgnoreCase(requestType)) {
                    logger.log("CREATE\n");

                    try (Connection connection = DriverManager.getConnection(jdbcUrl, masterConnectionProperties);
                         Statement sql = connection.createStatement()) {
                        connection.setAutoCommit(false);
                        InputStream bootstrapSql = Thread.currentThread().getContextClassLoader().getResourceAsStream("bootstrap_pool.sql");
                        Scanner bootstrapSqlScanner = new Scanner(bootstrapSql, "UTF-8");
                        bootstrapSqlScanner.useDelimiter(";");
                        while (bootstrapSqlScanner.hasNext()) {
                            // Simple variable replacement in the SQL
                            String ddl = bootstrapSqlScanner.next()
                                    .replace("{{DB_APP_USER}}", dbAppUser)
                                    .replace("{{DB_APP_PASS}}", decryptedAppPassword);
                            sql.addBatch(ddl);
                        }
                        sql.executeBatch();
                        connection.commit();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Update".equalsIgnoreCase(requestType)) {
                    logger.log("UDPATE\n");
                    // TODO: Is there really any logical update process here?
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Delete".equalsIgnoreCase(requestType)) {
                    logger.log("DELETE\n");
                    // TODO: Do we dare drop the database here?
                    sendResponse(input, context, "SUCCESS", responseData);
                } else {
                    logger.log("FAILED unknown requestType " + requestType + "\n");
                    responseData.put("Reason", "Unknown RequestType " + requestType);
                    sendResponse(input, context, "FAILED", responseData);
                }
            };
            Future<?> f = service.submit(r);
            f.get(context.getRemainingTimeInMillis() - 1000, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException | InterruptedException | ExecutionException e) {
            // Timed out or unexpected exception
            logger.log("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
            // Print the full stack trace
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            logger.log(sw.getBuffer().toString() + "\n");

            responseData.put("Reason", "Request timed out");
            sendResponse(input, context, "FAILED", responseData);
        } finally {
            service.shutdown();
        }

        return null;
    }

    /**
     * Send a response to CloudFormation regarding progress in creating resource.
     *
     * @param input
     * @param context
     * @param responseStatus
     * @param responseData
     * @return
     */
    public final Object sendResponse(final Map<String, Object> input, final Context context, final String responseStatus, ObjectNode responseData) {

        String responseUrl = (String) input.get("ResponseURL");
        context.getLogger().log("ResponseURL: " + responseUrl + "\n");

        URL url;
        try {
            url = new URL(responseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");

            ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
            responseBody.put("Status", responseStatus);
            responseBody.put("RequestId", (String) input.get("RequestId"));
            responseBody.put("LogicalResourceId", (String) input.get("LogicalResourceId"));
            responseBody.put("StackId", (String) input.get("StackId"));
            responseBody.put("PhysicalResourceId", context.getLogStreamName());
            if (!"FAILED".equals(responseStatus)) {
                responseBody.set("Data", responseData);
            } else {
                responseBody.put("Reason", responseData.get("Reason").asText());
            }
            try (OutputStreamWriter response = new OutputStreamWriter(connection.getOutputStream())) {
                response.write(responseBody.toString());
            }
            context.getLogger().log("Response Code: " + connection.getResponseCode() + "\n");
            connection.disconnect();
        } catch (IOException e) {
            // Print the full stack trace
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            context.getLogger().log(sw.getBuffer().toString() + "\n");
        }

        return null;
    }
}
