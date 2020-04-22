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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class CopyS3Objects implements RequestHandler<Map<String, Object>, Object> {

    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            ObjectMapper mapper = new ObjectMapper();
            logger.log(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input) + "\n");
        } catch (JsonProcessingException e) {
            logger.log("Could not log input\n");
        }

        final String requestType = (String) input.get("RequestType");
        Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");
        final String source = (String) resourceProperties.get("Source");
        final String sourceRegion = (String) resourceProperties.get("SourceRegion");
        final String destination = (String) resourceProperties.get("Destination");
        final String destinationRegion = (String) resourceProperties.get("DestinationRegion");
        final List<String> objects = (List<String>) resourceProperties.get("Objects");

        ExecutorService service = Executors.newSingleThreadExecutor();
        ObjectNode responseData = JsonNodeFactory.instance.objectNode();
        try {
            Runnable r = () -> {
                logger.log("Creating client for source bucket in " + sourceRegion + "\n");
                S3Client s3Source = S3Client.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder())
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .region(Region.of(sourceRegion))
                        .build();

                S3Client s3Destination = null;
                if (destinationRegion.equals(sourceRegion)) {
                    logger.log("Reusing client for destination bucket in " + destinationRegion + "\n");
                    s3Destination = s3Source;
                } else {
                    logger.log("Creating client for destination bucket in " + destinationRegion + "\n");
                    s3Destination = S3Client.builder()
                            .httpClientBuilder(UrlConnectionHttpClient.builder())
                            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                            .region(Region.of(destinationRegion))
                            .build();
                }

                if ("Create".equalsIgnoreCase(requestType) || "Update".equalsIgnoreCase(requestType)) {
                    for (String key : objects) {
                        logger.log("Copying: " + source + "/" + key + " -> " + destination + "/" + key + "\n");
                        s3Destination.copyObject(request -> request
                                .copySource(source + "/" + key)
                                .bucket(destination)
                                .key(key)
                        );
                    }
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Delete".equalsIgnoreCase(requestType)) {
                    for (String key : objects) {
                        logger.log("Deleting: " + destination + "/" + key + "\n");
                        s3Destination.deleteObject(request -> request.bucket(destination).key(key));
                    }
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
            // Timed out
            logger.log("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
            String stackTrace = getFullStackTrace(e);
            logger.log(stackTrace + "\n");
            responseData.put("Reason", stackTrace);
            sendResponse(input, context, "FAILED", responseData);
        } finally {
            service.shutdown();
        }
        return null;
    }

    /**
     * Send a response to CloudFormation regarding progress in creating resource.
     *
     * @param event
     * @param context
     * @param responseStatus
     * @param responseData
     * @return
     */
    public final Object sendResponse(final Map<String, Object> event, final Context context, final String responseStatus, ObjectNode responseData) {
        LambdaLogger logger = context.getLogger();
        String responseUrl = (String) event.get("ResponseURL");
        logger.log("ResponseURL: " + responseUrl + "\n");

        URL url;
        try {
            url = new URL(responseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "");
            connection.setRequestMethod("PUT");

            ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
            responseBody.put("Status", responseStatus);
            responseBody.put("RequestId", (String) event.get("RequestId"));
            responseBody.put("LogicalResourceId", (String) event.get("LogicalResourceId"));
            responseBody.put("StackId", (String) event.get("StackId"));
            responseBody.put("PhysicalResourceId", (String) event.get("LogicalResourceId"));
            if (!"FAILED".equals(responseStatus)) {
                responseBody.set("Data", responseData);
            } else {
                responseBody.put("Reason", responseData.get("Reason").asText());
            }
            logger.log("Response Body: " + responseBody.toString() + "\n");

            try (OutputStreamWriter response = new OutputStreamWriter(connection.getOutputStream())) {
                response.write(responseBody.toString());
            } catch (IOException ioe) {
                logger.log("Failed to call back to CFN response URL\n");
                logger.log(getFullStackTrace(ioe) + "\n");
            }

            logger.log("Response Code: " + connection.getResponseCode() + "\n");
            connection.disconnect();
        } catch (IOException e) {
            logger.log("Failed to open connection to CFN response URL\n");
            logger.log(getFullStackTrace(e) + "\n");
        }

        return null;
    }

    private static String getFullStackTrace(Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}
