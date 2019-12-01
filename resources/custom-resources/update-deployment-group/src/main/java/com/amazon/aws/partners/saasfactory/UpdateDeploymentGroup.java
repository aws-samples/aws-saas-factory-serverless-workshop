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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.codedeploy.CodeDeployClient;
import software.amazon.awssdk.services.codedeploy.model.AutoScalingGroup;
import software.amazon.awssdk.services.codedeploy.model.DeploymentGroupInfo;
import software.amazon.awssdk.services.codedeploy.model.GetDeploymentGroupResponse;
import software.amazon.awssdk.services.codedeploy.model.UpdateDeploymentGroupResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class UpdateDeploymentGroup implements RequestHandler<Map<String, Object>, Object> {

	private static final Logger logger = LoggerFactory.getLogger(UpdateDeploymentGroup.class);
	private CodeDeployClient codeDeploy;

 	public UpdateDeploymentGroup() {
		this.codeDeploy = CodeDeployClient.builder()
				.httpClientBuilder(UrlConnectionHttpClient.builder())
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
 	}

	@Override
	public Object handleRequest(Map<String, Object> input, Context context) {

//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			logger.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input));
//			logger.info("\n");
//		} catch (JsonProcessingException e) {
//			logger.info("Could not log input\n");
//		}

		final String requestType = (String) input.get("RequestType");
		Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");
		final String codeDeployApplication = (String) resourceProperties.get("ApplicationName");
		final String deploymentGroup = (String) resourceProperties.get("DeploymentGroup");
		final String autoScalingGroup = (String) resourceProperties.get("AutoScalingGroup");

		ExecutorService service = Executors.newSingleThreadExecutor();
		ObjectNode responseData = JsonNodeFactory.instance.objectNode();
		try {
			if (requestType == null) {
				throw new RuntimeException();
			}
			Runnable r = () -> {
				// We have to get the current state of the deployment group because
				// CodeDeploy::UpdateDeploymentGroup is destructive not additive
				GetDeploymentGroupResponse existingDeploymentGroup = codeDeploy.getDeploymentGroup(request -> request
						.applicationName(codeDeployApplication)
						.deploymentGroupName(deploymentGroup)
				);
				DeploymentGroupInfo deploymentGroupInfo = existingDeploymentGroup.deploymentGroupInfo();
				List<AutoScalingGroup> existingAutoScalingGroups = deploymentGroupInfo.autoScalingGroups();

				if ("Create".equalsIgnoreCase(requestType) || "Update".equalsIgnoreCase(requestType)) {
					logger.info("CREATE or UPDATE\n");

					// Add the requested auto scaling group to the deployment group's
					List<String> autoScalingGroups = new ArrayList<>(Arrays.asList(autoScalingGroup));
					existingAutoScalingGroups
							.stream()
							.map(asg -> asg.name())
							.forEachOrdered(autoScalingGroups::add);

					UpdateDeploymentGroupResponse response = codeDeploy.updateDeploymentGroup(request -> request
							.applicationName(codeDeployApplication)
							.currentDeploymentGroupName(deploymentGroup)
							.autoScalingGroups(autoScalingGroups)
					);

					sendResponse(input, context, "SUCCESS", responseData);
				} else if ("Delete".equalsIgnoreCase(requestType)) {
					logger.info("DELETE\n");

					// Filter out the auto scaling group we're deleting and call update
					List<String> autoScalingGroups = existingAutoScalingGroups
							.stream()
							.map(asg -> asg.name())
							.filter(asg -> !autoScalingGroup.equals(asg))
							.collect(Collectors.toList());

					UpdateDeploymentGroupResponse response = codeDeploy.updateDeploymentGroup(request -> request
							.applicationName(codeDeployApplication)
							.currentDeploymentGroupName(deploymentGroup)
							.autoScalingGroups(autoScalingGroups)
					);

					sendResponse(input, context, "SUCCESS", responseData);
				} else {
					logger.error("FAILED unknown requestType " + requestType + "\n");
					responseData.put("Reason", "Unknown RequestType " + requestType);
					sendResponse(input, context, "FAILED", responseData);
				}
			};
			Future<?> f = service.submit(r);
			f.get(context.getRemainingTimeInMillis() - 1000, TimeUnit.MILLISECONDS);
		} catch (final TimeoutException | InterruptedException | ExecutionException e) {
			// Timed out
			logger.error("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
			// Print entire stack trace
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw, true);
			e.printStackTrace(pw);
			logger.error(sw.getBuffer().toString() + "\n");

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
			logger.error(sw.getBuffer().toString() + "\n");
		}

		return null;
	}

}