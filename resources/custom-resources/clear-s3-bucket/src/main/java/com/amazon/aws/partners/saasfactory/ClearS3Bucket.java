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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ClearS3Bucket implements RequestHandler<Map<String, Object>, Object> {

    private S3Client s3;

    public ClearS3Bucket() {
        this.s3 = S3Client.builder()
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
        final String bucket = (String) resourceProperties.get("Bucket");

        ExecutorService service = Executors.newSingleThreadExecutor();
        ObjectNode responseData = JsonNodeFactory.instance.objectNode();
        try {
            if (requestType == null) {
                throw new RuntimeException();
            }
            Runnable r = () -> {
                if ("Create".equalsIgnoreCase(requestType) || "Update".equalsIgnoreCase(requestType)) {
                    logger.log("CREATE or UPDATE\n");
                    sendResponse(input, context, "SUCCESS", responseData);
                } else if ("Delete".equalsIgnoreCase(requestType)) {
                    logger.log("DELETE\n");

                    // The list of objects in the bucket to delete
                    List<ObjectIdentifier> toDelete = new ArrayList<>();

                    // Is the bucket versioned?
                    GetBucketVersioningResponse versioningResponse = s3.getBucketVersioning(request -> request.bucket(bucket));
                    if (BucketVersioningStatus.ENABLED.equals(versioningResponse.status())) {
                        logger.log("Bucket " + bucket + " is versioned (" + versioningResponse.status() + ")\n");
                        ListObjectVersionsResponse response;
                        do {
                            response = s3.listObjectVersions(builder -> builder.bucket(bucket));
                            response.versions()
                                    .stream()
                                    .map(version ->
                                        ObjectIdentifier.builder()
                                                .key(version.key())
                                                .versionId(version.versionId())
                                                .build()
                                    )
                                    .forEachOrdered(toDelete::add);
                        } while (response.isTruncated());
                    } else {
                        logger.log("Bucket " + bucket + " is not versioned (" + versioningResponse.status() + ")\n");
                        ListObjectsV2Response response;
                        do {
                            response = s3.listObjectsV2(request -> request.bucket(bucket));
                            response.contents()
                                    .stream()
                                    .map(obj ->
                                            ObjectIdentifier.builder()
                                                    .key(obj.key())
                                                    .build()
                                    )
                                    .forEachOrdered(toDelete::add);
                        } while (response.isTruncated());
                    }
                    if (!toDelete.isEmpty()) {
                        DeleteObjectsResponse deleteResponse = s3.deleteObjects(builder -> builder
                                .bucket(bucket)
                                .delete(Delete.builder().objects(toDelete).build())
                        );
                        logger.log("Cleaned up " + deleteResponse.deleted().size() + " objects in bucket " + bucket + "\n");
                    } else {
                        logger.log("Bucket " + bucket + " is empty. No objects to clean up.\n");
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
            // Print entire stack trace
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            String stackTrace = sw.getBuffer().toString();
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
            // Print whole stack trace
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            context.getLogger().log(sw.getBuffer().toString() + "\n");
        }

        return null;
    }

}