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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.*;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LambdaWarmer implements RequestHandler<Map<String, Object>, Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LambdaWarmer.class);
	private LambdaAsyncClient lambda;
	private ApiGatewayClient apigw;

	public LambdaWarmer() {
		this.lambda = LambdaAsyncClient.builder()
				.httpClientBuilder(NettyNioAsyncHttpClient.builder())
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
		this.apigw = ApiGatewayClient.builder()
				.httpClientBuilder(UrlConnectionHttpClient.builder())
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
	}
	
	@Override
	public Object handleRequest(Map<String, Object> input, Context context) {
		String apiGatewayEndpoint = (String) input.get("Endpoint");
		List<String> functions = (List<String>) input.get("Warmup");
//		if (functions != null && !functions.isEmpty()) {
//			List<CompletableFuture<InvokeResponse>> requests = new ArrayList<>();
//			for (String function : functions) {
//				LOGGER.info("Warming up " + function);
//				CompletableFuture<InvokeResponse> response = lambda.invoke(request -> request
//						.functionName(function)
//						.invocationType(InvocationType.EVENT)
//						.payload(SdkBytes.fromUtf8String("{\"source\": \"warmup\"}"))
//				);
//				requests.add(response);
//			}
//			CompletableFuture.allOf(requests.toArray(new CompletableFuture[requests.size()])).join();
//		}

		// API Gateway seems inconsistent when it comes to using warm, available Lambda functions
		// even though we're not using X-Ray... So, we'll brute force the warm up with a call through
		// the gateway
		CloseableHttpClient apiGateway = HttpClients.createDefault();
		try {
			URIBuilder nextAvailableDatabaseGet = new URIBuilder(apiGatewayEndpoint + "/tenants/pool/database");
			nextAvailableDatabaseGet.setParameter("source", "warmup");

			HttpGet nextAvailableDatabaseCall = new HttpGet(nextAvailableDatabaseGet.build());
			nextAvailableDatabaseCall.setHeader("Accept", "application/json");
			nextAvailableDatabaseCall.setHeader("Content-Type", "application/json");

			LOGGER.info("Warming up " + nextAvailableDatabaseCall.getURI().toString());
			CloseableHttpResponse nextAvailableDatabaseResponse = apiGateway.execute(nextAvailableDatabaseCall);
			EntityUtils.consume(nextAvailableDatabaseResponse.getEntity());

			HttpPost createTenantCall = new HttpPost(apiGatewayEndpoint + "/tenants");
			createTenantCall.setHeader("Accept", "application/json");
			createTenantCall.setHeader("Content-Type", "application/json");
			String warmup = "{\"source\": \"warmup\"}";
			StringEntity json = new StringEntity(warmup);
			createTenantCall.setEntity(json);

			LOGGER.info("Warming up " + createTenantCall.getURI().toString());
			CloseableHttpResponse createTenantResponse = apiGateway.execute(createTenantCall);
			EntityUtils.consume(createTenantResponse.getEntity());

			HttpPut updateTenantUserPoolCall = new HttpPut(apiGatewayEndpoint + "/tenants/00000000-0000-0000-0000-000000000000/userpool");
			updateTenantUserPoolCall.setHeader("Accept", "application/json");
			updateTenantUserPoolCall.setHeader("Content-Type", "application/json");
			updateTenantUserPoolCall.setEntity(json);

			LOGGER.info("Warming up " + updateTenantUserPoolCall.getURI().toString());
			CloseableHttpResponse updateTenantUserPoolResponse = apiGateway.execute(updateTenantUserPoolCall);
			EntityUtils.consume(updateTenantUserPoolResponse.getEntity());

			HttpPost registrationCall = new HttpPost(apiGatewayEndpoint + "/registration");
			registrationCall.setHeader("Accept", "application/json");
			registrationCall.setHeader("Content-Type", "application/json");
			registrationCall.setEntity(json);

			LOGGER.info("Warming up " + registrationCall.getURI().toString());
			CloseableHttpResponse registrationResponse = apiGateway.execute(registrationCall);
			EntityUtils.consume(registrationResponse.getEntity());

			HttpPost authCall = new HttpPost(apiGatewayEndpoint + "/auth");
			authCall.setHeader("Accept", "application/json");
			authCall.setHeader("Content-Type", "application/json");
			authCall.setEntity(json);

			LOGGER.info("Warming up " + authCall.getURI().toString());
			CloseableHttpResponse authResponse = apiGateway.execute(authCall);
			EntityUtils.consume(authResponse.getEntity());
		} catch (URISyntaxException | IOException ioe) {
		} finally {
			try {
				apiGateway.close();
			} catch (IOException e) {
			}
		}

		return null;
	}

//	public void foo(Map<String, Object> input, Context context) {
//		String apiGatewayName = (String) input.get("api");
//		String apiGatewayId = null;
//		GetRestApisResponse apis = apigw.getRestApis();
//		for (RestApi api : apis.items()) {
//			if (apiGatewayName.equals(api.name())) {
//				apiGatewayId = api.id();
//				break;
//			}
//		}
//		if (apiGatewayId != null) {
//			GetResourcesResponse resourcesResponse = apigw.getResources(GetResourcesRequest.builder().restApiId(apiGatewayId).build());
//			for (Resource resource : resourcesResponse.items()) {
//				for (Map.Entry<String, Method> method : resource.resourceMethods().entrySet()) {
//
//				}
//			}
//		}
//	}
}