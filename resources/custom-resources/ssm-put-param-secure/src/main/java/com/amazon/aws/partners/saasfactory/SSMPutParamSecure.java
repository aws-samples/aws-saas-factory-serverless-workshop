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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest;
import software.amazon.awssdk.services.ssm.model.DeleteParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterResponse;

public class SSMPutParamSecure implements RequestHandler<Map<String, Object>, Object> {

	private SsmClient ssm;

	public SSMPutParamSecure() {
		this.ssm = SsmClient.builder()
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
		final String ssmParameterName = (String) resourceProperties.get("Name");
		final String ssmParameterValue = (String) resourceProperties.get("Value");
		final boolean update = "Update".equalsIgnoreCase(requestType);
		final boolean withKey = resourceProperties.containsKey("KeyId");
		final boolean withDescription = resourceProperties.containsKey("Description");

		final String[] stackArn = ((String) input.get("StackId")).split(":");
		final String region = stackArn[3];
		final String accountId = stackArn[4];
//		final String stack = stackArn[5].split("/")[1];
		final String ssmArn = "arn:aws:ssm:" + region + ":" + accountId + ":parameter/" + ssmParameterName;

		ExecutorService service = Executors.newSingleThreadExecutor();

		ObjectNode responseData = JsonNodeFactory.instance.objectNode();
		try {
			if (requestType == null) {
				throw new RuntimeException();
			}
			Runnable r = () -> {
				PutParameterRequest request = PutParameterRequest.builder()
						.name(ssmParameterName)
						.value(ssmParameterValue)
						.type(ParameterType.SECURE_STRING)
						.overwrite(update)
						.build();

				if (withKey && !withDescription) {
					request = request.toBuilder()
							.keyId((String) resourceProperties.get("KeyId"))
							.build();
				} else if (!withKey && withDescription) {
					request = request.toBuilder()
							.description((String) resourceProperties.get("Description"))
							.build();
				} else if (withKey && withDescription) {
					request = request.toBuilder()
							.keyId((String) resourceProperties.get("KeyId"))
							.description((String) resourceProperties.get("Description"))
							.build();
				}

				if ("Create".equalsIgnoreCase(requestType)) {
					logger.log("CREATE\n");

					PutParameterResponse response = ssm.putParameter(request);
					Long version = response.version();

					responseData.put("Parameter", ssmParameterName);
					responseData.put("ARN", ssmArn);
					responseData.put("Version", version);

					logger.log("Adding new parameter to SSM: " + ssmArn + "\n");

					sendResponse(input, context, "SUCCESS", responseData);
				} else if ("Update".equalsIgnoreCase(requestType)) {
					logger.log("UDPATE\n");

					PutParameterResponse response = ssm.putParameter(request);
					Long version = response.version();

					responseData.put("Parameter", ssmParameterName);
					responseData.put("ARN", ssmArn);
					responseData.put("Version", version);

					logger.log("Updating value of existing SSM parameter: " + ssmArn + "\n");

					sendResponse(input, context, "SUCCESS", responseData);
				} else if ("Delete".equalsIgnoreCase(requestType)) {
					logger.log("DELETE\n");

					DeleteParameterRequest deleteRequest = DeleteParameterRequest.builder()
							.name(ssmParameterName)
							.build();

					DeleteParameterResponse response = ssm.deleteParameter(deleteRequest);

					responseData.put("ARN", ssmArn);

					logger.log("Deleting SSM parameter: " + ssmArn + "\n");

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
			logger.log(sw.getBuffer().toString() + "\n");

			responseData.put("Reason", e.getMessage());
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
