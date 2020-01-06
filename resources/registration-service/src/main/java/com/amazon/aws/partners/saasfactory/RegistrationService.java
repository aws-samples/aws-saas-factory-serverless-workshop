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
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeRulesResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterResponse;
import software.amazon.awssdk.utils.IoUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegistrationService implements RequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {

    private final static Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String ONBOARDING_TEMPLATE = "onboard-tenant.template";
    private final static Map<String, String> CORS = Stream
            .of(new AbstractMap.SimpleEntry<String, String>("Access-Control-Allow-Origin", "*"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private SsmClient ssm;
    private CloudFormationClient cfn;
    private ElasticLoadBalancingV2Client elbv2;
    private CognitoIdentityProviderClient cognito;
    private DynamoDbClient ddb;
    private String apiGatewayEndpoint;
    private String workshopBucket;
    private String keyPairName;
    private String vpcId;
    private String applicationServerSecurityGroup;
    private String privateSubnetIds;
    private String codePipelineBucket;
    private String codeDeployApplication;
    private String deploymentGroup;
    private String updateCodeDeployLambdaArn;
    private String albListenerArn;
    private String addDatabaseUserArn;

    public RegistrationService() {

        this.ssm = SsmClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        GetParametersResponse ssmBatch1 = this.ssm.getParameters(request -> request
                .names("API_GW", "WORKSHOP_BUCKET", "KEY_PAIR", "VPC", "APP_SG","PRIVATE_SUBNETS")
        );

        for (software.amazon.awssdk.services.ssm.model.Parameter parameter : ssmBatch1.parameters()) {
            switch (parameter.name()) {
                case "API_GW":
                    this.apiGatewayEndpoint = parameter.value();
                    LOGGER.info("Setting env api gateway = " + this.apiGatewayEndpoint);
                    break;
                case "WORKSHOP_BUCKET":
                    this.workshopBucket = parameter.value();
                    LOGGER.info("Setting env workshop bucket = " + this.workshopBucket);
                    break;
                case "KEY_PAIR":
                    this.keyPairName = parameter.value();
                    LOGGER.info("Setting env key pair name = " + this.keyPairName);
                    break;
                case "VPC":
                    this.vpcId = parameter.value();
                    LOGGER.info("Setting env vpc = " + this.vpcId);
                    break;
                case "APP_SG":
                    this.applicationServerSecurityGroup = parameter.value();
                    LOGGER.info("Setting env app server security group = " + this.applicationServerSecurityGroup);
                    break;
                case "PRIVATE_SUBNETS":
                    this.privateSubnetIds = parameter.value();
                    LOGGER.info("Setting env private subnets = " + this.privateSubnetIds);
                    break;
            }
        }

        // Can only query for a max of 10 parameters at a time...
        GetParametersResponse ssmBatch2 = this.ssm.getParameters(request -> request
                .names("PIPELINE_BUCKET", "CODE_DEPLOY", "DEPLOYMENT_GROUP", "CODE_DEPLOY_LAMBDA", "ALB_LISTENER", "RDS_ADD_USER_LAMBDA")
        );
        for (software.amazon.awssdk.services.ssm.model.Parameter parameter : ssmBatch2.parameters()) {
            switch (parameter.name()) {
                case "PIPELINE_BUCKET":
                    this.codePipelineBucket = parameter.value();
                    LOGGER.info("Setting env codepipeline bucket = " + this.codePipelineBucket);
                    break;
                case "CODE_DEPLOY":
                    this.codeDeployApplication = parameter.value();
                    LOGGER.info("Setting env codedeploy application = " + this.codeDeployApplication);
                    break;
                case "DEPLOYMENT_GROUP":
                    this.deploymentGroup = parameter.value();
                    LOGGER.info("Setting env codedeploy deployment group = " + this.deploymentGroup);
                    break;
                case "CODE_DEPLOY_LAMBDA":
                    this.updateCodeDeployLambdaArn = parameter.value();
                    LOGGER.info("Setting env update codedeploy lambda = " + this.updateCodeDeployLambdaArn);
                    break;
                case "ALB_LISTENER":
                    this.albListenerArn = parameter.value();
                    LOGGER.info("Setting env alb listener = " + this.albListenerArn);
                    break;
                case "RDS_ADD_USER_LAMBDA":
                    this.addDatabaseUserArn = parameter.value();
                    LOGGER.info("Setting env add db user = " + this.addDatabaseUserArn);
                    break;
            }
        }

        if (this.apiGatewayEndpoint == null || this.apiGatewayEndpoint.isEmpty() ||
                this.workshopBucket == null || this.workshopBucket.isEmpty() ||
                this.keyPairName == null || this.keyPairName.isEmpty() ||
                this.vpcId == null || this.vpcId.isEmpty() ||
                this.applicationServerSecurityGroup == null || this.applicationServerSecurityGroup.isEmpty() ||
                this.privateSubnetIds == null || this.privateSubnetIds.isEmpty() ||
                this.codePipelineBucket == null || this.codePipelineBucket.isEmpty() ||
                this.deploymentGroup == null || this.deploymentGroup.isEmpty() ||
                this.updateCodeDeployLambdaArn == null || this.updateCodeDeployLambdaArn.isEmpty() ||
                this.albListenerArn == null || this.albListenerArn.isEmpty() ||
                this.addDatabaseUserArn == null || this.addDatabaseUserArn.isEmpty()
        ) {
            throw new RuntimeException("Failed to get all required settings from Parameter Store!");
        }

        this.elbv2 = ElasticLoadBalancingV2Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        this.cfn = CloudFormationClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        this.cognito = CognitoIdentityProviderClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        this.ddb = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(Map<String, Object> event, Context context) {
        return register(event, context);
    }

    /**
     * 1. Call the TenantService to get an unclaimed RDS cluster from the hot pool
     * 2. Call the TenantService to create a new tenant record
     * 3. Update the RDS cluster to add the new application user and password (todo)
     * 4. Save the database connection properties to parameter store so the app servers
     *    for this tenant can configure themselves at runtime
     * 5. Trigger CloudFormation to run the onboarding stack for this tenant
     * @param event
     * @param context
     * @return
     */
    public APIGatewayProxyResponseEvent register(Map<String, Object> event, Context context) {
        //logRequestEvent(event);
        if ("warmup".equals(event.get("source"))) {
            LOGGER.info("Warming up");
            return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
        } else if (event.containsKey("body")) {
            try {
                Map<String, String> requestBody = MAPPER.readValue((String) event.get("body"), HashMap.class);
                if (requestBody != null && "warmup".equals(requestBody.get("source"))) {
                    LOGGER.info("Warming up");
                    return new APIGatewayProxyResponseEvent().withHeaders(CORS).withStatusCode(200);
                }
            } catch (IOException e) {
            }
        }

        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("RegistrationService::register");
        APIGatewayProxyResponseEvent response = null;
        Map<String, String> error = new HashMap<>();

        Registration registration = registrationFromJson((String) event.get("body"));
        Tenant tenant = null;
        if (registration != null && !registration.isEmpty()) {
            try {
                // Not thread safe - do not copy this

                // 1. Get an available RDS cluster
//                String availableDatabaseResponse = nextAvailableDatabase();
//                Map<String, String> availableDatabase = MAPPER.readValue(availableDatabaseResponse, Map.class);
                Map<String, String> availableDatabase = nextAvailableDatabase();
                if (availableDatabase == null || availableDatabase.isEmpty()) {
                    throw new RuntimeException("Cannot register new tenant. Hot pool of RDS clusters has been depleted.");
                }
                String tenantDatabase = availableDatabase.get("Endpoint");
                LOGGER.info("RegistrationService::register next available database = " + tenantDatabase);

                // 2. Create the new tenant record, generating the UUID
                tenant = createTenant(registration.getCompany(), registration.getPlan(), tenantDatabase);
                LOGGER.info("RegistrationService::register created tenant " + tenant.getId().toString());

                // 3. Create a Cognito User Pool for this tenant now that we have its
                // unique id, and then create a new user in that pool from the registration
                String userPoolId = createUserPool(tenant, registration);
                LOGGER.info("RegistrationService::register created user pool " + userPoolId);
                // Skip this for now since we don't use it and can't seem to avoid the cold start
                // tenant = updateTenantUserPool(tenant, userPoolId);
                tenant.setUserPool(userPoolId);
                createUser(tenant, registration);
                LOGGER.info("RegistrationService::register created user " + registration.getFirstName() + " " + registration.getLastName());

                // 4. Save this tenant's environment variables to parameter store
                storeParameters(tenant);

                // 5. Now provision this tenant's silo infrastructure (async)
                String stackName = createStack(tenant);

                Map<String, String> result = Stream.of(
                        new AbstractMap.SimpleEntry<>("TenantId", tenant.getId().toString()),
                        new AbstractMap.SimpleEntry<>("StackName", stackName))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody(MAPPER.writeValueAsString(result))
                        .withHeaders(CORS);
            } catch (Exception e) {
                error.put("message", e.getMessage());
                response = new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody(toJson(error));
            }
        } else {
            error.put("message", "request body invalid");
            response = new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(toJson(error));
        }

        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("RegistrationService::register exec " + totalTimeMillis);
        return response;
    }

//    protected String nextAvailableDatabase() throws Exception {
//        long startTimeMillis = System.currentTimeMillis();
//        LOGGER.info("RegistrationService::nextAvailableDatabase");
//        URI invokeURL = URI.create(apiGatewayEndpoint + "/tenants/pool/database");
//        HttpURLConnection apiGateway = (HttpURLConnection) invokeURL.toURL().openConnection();
//        apiGateway.setRequestMethod("GET");
//        apiGateway.setRequestProperty("Accept", "application/json");
//        apiGateway.setRequestProperty("Content-Type", "application/json");
//
//        LOGGER.info("RegistrationService::createTenant Invoking API Gateway at " + invokeURL.toString());
//
//        if (apiGateway.getResponseCode() >= 400) {
//            throw new Exception(IoUtils.toUtf8String(apiGateway.getErrorStream()));
//        }
//        String result = IoUtils.toUtf8String(apiGateway.getInputStream());
//        LOGGER.info("RegistrationService::nextAvailableDatabse TenantService call \n" + result);
//
//        apiGateway.disconnect();
//        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
//        LOGGER.info("RegistrationService::nextAvailableDatabase exec " + totalTimeMillis);
//        return result;
//    }

    protected Map<String, String> nextAvailableDatabase() {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("RegistrationService::nextAvailableDatabase");
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
            LOGGER.error("RegistrationService::nextAvailableDatabase " + getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("RegistrationService::nextAvailableDatabase exec " + totalTimeMillis);
        return availableDatabase;
    }

    protected Tenant createTenant(String companyName, String plan, String database) throws Exception {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("RegistrationService::createTenant " + companyName);
        Tenant tenant = new Tenant(null, Boolean.TRUE, companyName, plan, null, database);

        URI invokeURL = URI.create(apiGatewayEndpoint + "/tenants");
        HttpURLConnection apiGateway = (HttpURLConnection) invokeURL.toURL().openConnection();
        apiGateway.setDoOutput(true);
        apiGateway.setRequestMethod("POST");
        apiGateway.setRequestProperty("Accept", "application/json");
        apiGateway.setRequestProperty("Content-Type", "application/json");

        LOGGER.info("RegistrationService::createTenant Invoking API Gateway at " + invokeURL.toString());
        OutputStream body = apiGateway.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(body, StandardCharsets.UTF_8);
        writer.write(toJson(tenant));
        writer.flush();
        writer.close();
        body.close();

        if (apiGateway.getResponseCode() >= 400) {
            throw new Exception(IoUtils.toUtf8String(apiGateway.getErrorStream()));
        }
        Tenant result = tenantFromJson(IoUtils.toUtf8String(apiGateway.getInputStream()));
        apiGateway.disconnect();
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("RegistrationService::createTenant exec " + totalTimeMillis);
        return result;
    }

    protected String createUserPool(Tenant tenant, Registration registration) {
        LOGGER.info("RegistrationService::createUserPool");
        String tenantId = tenant.getId().toString();
        String poolName = tenantId.substring(0, 8);
        CreateUserPoolResponse createUserPoolResponse = cognito.createUserPool(request -> request
                .poolName(poolName + "_UserPool")
                .schema(
                        SchemaAttributeType.builder()
                                .attributeDataType(AttributeDataType.STRING)
                                .name("email")
                                .required(Boolean.TRUE)
                                .build(),
                        SchemaAttributeType.builder()
                                .attributeDataType(AttributeDataType.STRING)
                                .name("given_name")
                                .required(Boolean.TRUE)
                                .build(),
                        SchemaAttributeType.builder()
                                .attributeDataType(AttributeDataType.STRING)
                                .name("family_name")
                                .required(Boolean.TRUE)
                                .build(),
                        SchemaAttributeType.builder()
                                .attributeDataType(AttributeDataType.STRING)
                                .name("tenant_id")
                                .required(Boolean.FALSE) // Custom attributes can't be required
                                .mutable(Boolean.FALSE)
                                .build(),
                        SchemaAttributeType.builder()
                                .attributeDataType(AttributeDataType.STRING)
                                .name("company")
                                .required(Boolean.FALSE) // Custom attributes can't be required
                                .mutable(Boolean.TRUE)
                                .build(),
                        SchemaAttributeType.builder()
                                .attributeDataType(AttributeDataType.STRING)
                                .name("plan")
                                .required(Boolean.FALSE) // Custom attributes can't be required
                                .mutable(Boolean.TRUE)
                                .build()
                )
                .adminCreateUserConfig(
                        AdminCreateUserConfigType.builder()
                                .allowAdminCreateUserOnly(Boolean.TRUE)
                                .build()
                )
                .policies(
                        UserPoolPolicyType.builder()
                                .passwordPolicy(
                                        PasswordPolicyType.builder()
                                                .minimumLength(8)
                                                .requireLowercase(Boolean.TRUE)
                                                .requireUppercase(Boolean.TRUE)
                                                .requireNumbers(Boolean.TRUE)
                                                .temporaryPasswordValidityDays(7)
                                                .build()
                                )
                        .build()
                )
        );
        UserPoolType userPool = createUserPoolResponse.userPool();

        LOGGER.info("RegistrationService::createUserPool create app client");
        CreateUserPoolClientResponse userPoolClientResponse = cognito.createUserPoolClient(request -> request
                .userPoolId(userPool.id())
                .clientName(poolName + "_AppClient")
                .generateSecret(Boolean.FALSE)
                .explicitAuthFlows(ExplicitAuthFlowsType.ADMIN_NO_SRP_AUTH)
        );

        return userPool.id();
    }

    protected Tenant updateTenantUserPool(Tenant tenant, String userPoolId) throws Exception {
        long startTimeMillis = System.currentTimeMillis();
        String tenantId = tenant.getId().toString();
        LOGGER.info("RegistrationService::updateTenantUserPool " + tenantId);
        tenant.setUserPool(userPoolId);

        URI invokeURL = URI.create(apiGatewayEndpoint + "/tenants/" + tenantId + "/userpool");
        HttpURLConnection apiGateway = (HttpURLConnection) invokeURL.toURL().openConnection();
        apiGateway.setDoOutput(true);
        apiGateway.setRequestMethod("PUT");
        apiGateway.setRequestProperty("Accept", "application/json");
        apiGateway.setRequestProperty("Content-Type", "application/json");

        LOGGER.info("RegistrationService::updateTenantUserPool Invoking API Gateway at " + invokeURL.toString());
        OutputStream body = apiGateway.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(body, StandardCharsets.UTF_8);
        writer.write(toJson(tenant));
        writer.flush();
        writer.close();
        body.close();

        if (apiGateway.getResponseCode() >= 400) {
            throw new Exception(IoUtils.toUtf8String(apiGateway.getErrorStream()));
        }
        Tenant result = tenantFromJson(IoUtils.toUtf8String(apiGateway.getInputStream()));
        apiGateway.disconnect();
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("RegistrationService::updateTenantUserPool exec " + totalTimeMillis);
        return result;
    }

    protected String createUser(Tenant tenant, Registration registration) {
        LOGGER.info("RegistrationService::createUser create Cognito user " + registration.getEmail());
        final String userPool = tenant.getUserPool();
        AdminCreateUserResponse createUserResponse = null;
        try {
            createUserResponse = cognito.adminCreateUser(request -> request
                    .userPoolId(userPool)
                    .username(registration.getEmail())
                    .userAttributes(
                            AttributeType.builder().name("email").value(registration.getEmail()).build(),
                            AttributeType.builder().name("family_name").value(registration.getLastName()).build(),
                            AttributeType.builder().name("given_name").value(registration.getFirstName()).build(),
                            AttributeType.builder().name("custom:tenant_id").value(tenant.getId().toString()).build(),
                            AttributeType.builder().name("custom:company").value(registration.getCompany()).build(),
                            AttributeType.builder().name("custom:plan").value(registration.getPlan()).build()
                    )
                    .temporaryPassword(generatePassword())
                    .desiredDeliveryMediumsWithStrings("EMAIL")
                    .messageAction("SUPPRESS")
            );
        } catch (SdkServiceException cognitoError) {
            LOGGER.error("CognitoIdentity::AdminCreateUser", cognitoError);
            LOGGER.error(getFullStackTrace(cognitoError));
        }
        final UserType user = createUserResponse.user();

        LOGGER.info("RegistrationService::createUser setting password");
        AdminSetUserPasswordResponse passwordResponse = cognito.adminSetUserPassword(request -> request
                .userPoolId(userPool)
                .username(user.username())
                .password(registration.getPassword())
                .permanent(Boolean.TRUE)
        );

//        UserStatusType status = cognito.adminGetUser(request -> request.userPoolId(userPool).username(user.username())).userStatus();
//        LOGGER.info("RegistrationService::createUser " + user.username() + " " + status.toString());
        return user.username();
    }

    protected void storeParameters(Tenant tenant) {
        long startTimeMillis = System.currentTimeMillis();
        LOGGER.info("RegistrationService::storeParameters");
        Map<String, String> params = Stream.of(
                new AbstractMap.SimpleEntry<>("DB_NAME", "saas_factory_srvls_wrkshp"),
                new AbstractMap.SimpleEntry<>("DB_USER", "application"),
                new AbstractMap.SimpleEntry<>("DB_PASS", generatePassword()),
                new AbstractMap.SimpleEntry<>("DB_HOST", tenant.getDatabase())
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<CompletableFuture<PutParameterResponse>> threads = new ArrayList<>();
        params.forEach((key, value) -> {
            String param = tenant.getId().toString() + "_" + key;
            LOGGER.info("RegistrationService::storeParameters PutParameter " + param);
            PutParameterResponse response = ssm.putParameter(request -> request
                    .name(param)
                    .value(value)
                    .type(key.startsWith("DB_PASS") ? ParameterType.SECURE_STRING : ParameterType.STRING)
                    .overwrite(Boolean.TRUE)
            );
        });
        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("RegistrationService::storeParameters exec " + totalTimeMillis);
    }

    protected String createStack(Tenant tenant) {
        long startTimeMillis = System.currentTimeMillis();

        LOGGER.info("RegistrationService::createStack getting routing rule priority");
        int priority = 0;
        try {
            DescribeRulesResponse elbResponse = elbv2.describeRules(request -> request
                    .listenerArn(albListenerArn)
            );
            priority = elbResponse.rules().size() + 1;
        } catch (SdkServiceException e) {
            LOGGER.error(getFullStackTrace(e));
            throw new RuntimeException(e);
        }
        LOGGER.info("RegistrationService::createStack routing rule priority = " + priority);

        final Integer tenantAlbRulePriority = priority;
        String stackName = "Tenant-" + tenant.getId().toString().substring(0, 8);
        LOGGER.info("RegistrationService::createStack " + stackName);
        CreateStackResponse response = cfn.createStack(request -> request
                .stackName(stackName)
                .onFailure("DO_NOTHING")
                .capabilitiesWithStrings("CAPABILITY_NAMED_IAM")
                //.templateURL("https://" + workshopBucket + ".s3-" + System.getenv("AWS_REGION") + ".amazonaws.com/" + ONBOARDING_TEMPLATE)
                .templateURL("https://" + workshopBucket + ".s3.amazonaws.com/" + ONBOARDING_TEMPLATE)
                .parameters(
                        Parameter.builder().parameterKey("TenantId").parameterValue(tenant.getId().toString()).build(),
                        Parameter.builder().parameterKey("TenantRouteALBPriority").parameterValue(tenantAlbRulePriority.toString()).build(),
                        Parameter.builder().parameterKey("KeyPair").parameterValue(keyPairName).build(),
                        Parameter.builder().parameterKey("VPC").parameterValue(vpcId).build(),
                        Parameter.builder().parameterKey("PrivateSubnets").parameterValue(privateSubnetIds).build(),
                        Parameter.builder().parameterKey("AppServerSecurityGroup").parameterValue(applicationServerSecurityGroup).build(),
                        Parameter.builder().parameterKey("CodePipelineBucket").parameterValue(codePipelineBucket).build(),
                        Parameter.builder().parameterKey("CodeDeployApplication").parameterValue(codeDeployApplication).build(),
                        Parameter.builder().parameterKey("DeploymentGroup").parameterValue(deploymentGroup).build(),
                        Parameter.builder().parameterKey("LambdaUpdateDeploymentGroupArn").parameterValue(updateCodeDeployLambdaArn).build(),
                        Parameter.builder().parameterKey("ALBListener").parameterValue(albListenerArn).build(),
                        Parameter.builder().parameterKey("LambdaAddDatabaseUserArn").parameterValue(addDatabaseUserArn).build()
                )
        );
        LOGGER.info("RegistrationService::createStack stack id " + response.stackId());

        long totalTimeMillis = System.currentTimeMillis() - startTimeMillis;
        LOGGER.info("RegistrationService::createStack CloudFormation CreateStack returned in " + totalTimeMillis);
        return stackName;
    }

    /**
     * Generate a random password that matches the password policy of the Cognito user pool
     * @return
     */
    public static String generatePassword () {
        // Split the classes of characters into separate buckets so we can be sure to use
        // the correct amount of each type
        final char[][] chars = {
                {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'},
                {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'},
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'},
                {'!', '#', '$', '%', '&', '*', '+', '-', '.', ':', '=', '?', '^', '_'}
        };

        final int passwordLength = 12;
        Random random = new Random();
        StringBuilder password = new StringBuilder(passwordLength);

        // Randomly select one character from each of the required character types
        ArrayList<Integer> requiredCharacterBuckets = new ArrayList<>(3);
        requiredCharacterBuckets.add(0, 0);
        requiredCharacterBuckets.add(1, 1);
        requiredCharacterBuckets.add(2, 2);
        while (!requiredCharacterBuckets.isEmpty()) {
            Integer randomRequiredCharacterBucket = requiredCharacterBuckets.remove(random.nextInt(requiredCharacterBuckets.size()));
            password.append(chars[randomRequiredCharacterBucket][random.nextInt(chars[randomRequiredCharacterBucket].length)]);
        }

        // Fill out the rest of the password with randomly selected characters
        for (int i = 0; i < passwordLength - 3; i++) {
            int characterBucket = random.nextInt(chars.length);
            password.append(chars[characterBucket][random.nextInt(chars[characterBucket].length)]);
        }
        return password.toString();
    }

    public static Registration registrationFromJson(String json) {
        Registration registration = null;
        try {
            registration = MAPPER.readValue(json, Registration.class);
        } catch (IOException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return registration;
    }

    public static Tenant tenantFromJson(String json) {
        Tenant tenant = null;
        try {
            tenant = MAPPER.readValue(json, Tenant.class);
        } catch (IOException e) {
            LOGGER.error(getFullStackTrace(e));
        }
        return tenant;
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