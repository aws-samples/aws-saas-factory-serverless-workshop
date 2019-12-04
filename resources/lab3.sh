#!/bin/bash

# Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this
# software and associated documentation files (the "Software"), to deal in the Software
# without restriction, including without limitation the rights to use, copy, modify,
# merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
# INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
# PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
# HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

if ! [ -x "$(command -v jq)" ]; then
	echo "Installing jq"
    sudo yum install -y jq
fi

echo "Setting environment variables"
MY_AWS_REGION=$(aws configure list | grep region | awk '{print $2}')
echo "AWS Region = $MY_AWS_REGION"

STACK_OUTPUTS=$(aws cloudformation describe-stacks | jq -r '.Stacks[] | select(.Outputs != null) | .Outputs[]')

WORKSHOP_STACK=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "SaaSFactoryServerlessSaaSWorkshopStack") | .OutputValue')
echo "Workshop stack = $WORKSHOP_STACK"

WORKSHOP_BUCKET=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "WorkshopBucket") | .OutputValue')
echo "Workshop bucket = $WORKSHOP_BUCKET"

LOAD_BALANCER_DNS=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "LoadBalancerEndpointLab2") | .OutputValue')
echo "Application load balancer = $LOAD_BALANCER_DNS"

TENANT_SVC_GET_ALL=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceGetAllArn") | .OutputValue')
echo "TenantService GetAll = $TENANT_SVC_GET_ALL"

TENANT_SVC_GET_ID=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceGetByIdArn") | .OutputValue')
echo "TenantService GetById = $TENANT_SVC_GET_ID"

TENANT_SVC_UPDATE=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceUpdateArn") | .OutputValue')
echo "TenantService Update = $TENANT_SVC_UPDATE"

TENANT_SVC_INSERT=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceInsertArn") | .OutputValue')
echo "TenantService Insert = $TENANT_SVC_INSERT"

TENANT_SVC_DELETE=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceDeleteArn") | .OutputValue')
echo "TenantService Delete = $TENANT_SVC_DELETE"

TENANT_SVC_NEXT_DB=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceNextDbArn") | .OutputValue')
echo "TenantService NextDb = $TENANT_SVC_NEXT_DB"

TENANT_SVC_UPDATE_USER_POOL=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "TenantServiceUpdateUserPoolArn") | .OutputValue')
echo "TenantService UpdateUserPool = $TENANT_SVC_UPDATE_USER_POOL"

REG_SVC_REGISTER=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "RegistrationServiceRegisterArn") | .OutputValue')
echo "RegistrationService Register = $REG_SVC_REGISTER"

AUTH_SVC_SIGN_IN=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "AuthServiceSignInArn") | .OutputValue')
echo "AuthService Sign In = $AUTH_SVC_SIGN_IN"

CUSTOM_AUTHORIZER=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "LambdaCustomAuthorizerArn") | .OutputValue')
echo "Custom Authorizer = $CUSTOM_AUTHORIZER"

AUTHORIZER_ROLE=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "ApiGatewayLambdaAuthorizerRoleArn") | .OutputValue')
echo "Custom Authorizer Role = $AUTHORIZER_ROLE"

LAMBDA_WARMER=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "LambdaWarmerArn") | .OutputValue')
echo "LambdaWarmer = $LAMBDA_WARMER"

TEMPLATE_URL="https://${WORKSHOP_BUCKET}.s3.amazonaws.com/lab3.template"
echo "CloudFormation template URL = $TEMPLATE_URL"

if [ -z "$WORKSHOP_STACK" ] \
	|| [ -z "$WORKSHOP_BUCKET" ] \
	|| [ -z "$LOAD_BALANCER_DNS" ] \
	|| [ -z "$TENANT_SVC_GET_ALL" ] \
	|| [ -z "$TENANT_SVC_GET_ID" ] \
	|| [ -z "$TENANT_SVC_UPDATE" ] \
	|| [ -z "$TENANT_SVC_INSERT" ] \
	|| [ -z "$TENANT_SVC_DELETE" ] \
	|| [ -z "$TENANT_SVC_NEXT_DB" ] \
	|| [ -z "$TENANT_SVC_UPDATE_USER_POOL" ] \
	|| [ -z "$REG_SVC_REGISTER" ] \
	|| [ -z "$AUTH_SVC_SIGN_IN" ] \
	|| [ -z "$CUSTOM_AUTHORIZER" ] \
	|| [ -z "$AUTHORIZER_ROLE" ] \
	|| [ -z "$LAMBDA_WARMER" ]; then
	echo "Missing required environment variables. Please make sure the lab2 CloudFormation stack has completed successfully."
	exit 1
fi

cd /home/ec2-user/environment/saas-factory-serverless-workshop/lab3/order-service
mvn
aws s3 cp target/OrderService-lambda.zip s3://$WORKSHOP_BUCKET

echo
aws cloudformation create-stack --stack-name "${WORKSHOP_STACK}-lab3" --on-failure DO_NOTHING --capabilities CAPABILITY_NAMED_IAM --template-url "${TEMPLATE_URL}" --parameters \
ParameterKey=LoadBalancerDNS,ParameterValue="${LOAD_BALANCER_DNS}" \
ParameterKey=TenantServiceGetAllArn,ParameterValue="${TENANT_SVC_GET_ALL}" \
ParameterKey=TenantServiceGetByIdArn,ParameterValue="${TENANT_SVC_GET_ID}" \
ParameterKey=TenantServiceUpdateArn,ParameterValue="${TENANT_SVC_UPDATE}" \
ParameterKey=TenantServiceInsertArn,ParameterValue="${TENANT_SVC_INSERT}" \
ParameterKey=TenantServiceDeleteArn,ParameterValue="${TENANT_SVC_DELETE}" \
ParameterKey=TenantServiceNextDbArn,ParameterValue="${TENANT_SVC_NEXT_DB}" \
ParameterKey=TenantServiceUpdateUserPoolArn,ParameterValue="${TENANT_SVC_UPDATE_USER_POOL}" \
ParameterKey=RegistrationServiceRegisterArn,ParameterValue="${REG_SVC_REGISTER}" \
ParameterKey=AuthServiceSignInArn,ParameterValue="${AUTH_SVC_SIGN_IN}" \
ParameterKey=LambdaCustomAuthorizerArn,ParameterValue="${CUSTOM_AUTHORIZER}" \
ParameterKey=ApiGatewayLambdaAuthorizerRoleArn,ParameterValue="${AUTHORIZER_ROLE}" \
ParameterKey=LambdaWarmerArn,ParameterValue="${LAMBDA_WARMER}" \
ParameterKey=WorkshopS3Bucket,ParameterValue="${WORKSHOP_BUCKET}"

cd /home/ec2-user/environment/saas-factory-serverless-workshop/resources