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

RDS_SECURITY_GROUP=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "RDSAccessSecurityGroup") | .OutputValue')
echo "RDS security group = $RDS_SECURITY_GROUP"

PRIVATE_SUBNETS=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "PrivateSubnets") | .OutputValue')
PRIVATE_SUBNETS=$(sed -e 's|,|\\,|' <<< $PRIVATE_SUBNETS)
echo "Private subnets = $PRIVATE_SUBNETS"

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

ORDER_SVC_GET_ALL=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "OrderServiceGetAllArn") | .OutputValue')
echo "OrderService GetAll = $ORDER_SVC_GET_ALL"

ORDER_SVC_GET_ID=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "OrderServiceGetByIdArn") | .OutputValue')
echo "OrderService GetById = $ORDER_SVC_GET_ID"

ORDER_SVC_UPDATE=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "OrderServiceUpdateArn") | .OutputValue')
echo "OrderService Update = $ORDER_SVC_UPDATE"

ORDER_SVC_INSERT=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "OrderServiceInsertArn") | .OutputValue')
echo "OrderService Insert = $ORDER_SVC_INSERT"

ORDER_SVC_DELETE=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "OrderServiceDeleteArn") | .OutputValue')
echo "OrderService Delete = $ORDER_SVC_DELETE"

REG_SVC_REGISTER=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "RegistrationServiceRegisterArn") | .OutputValue')
echo "RegistrationService Register = $REG_SVC_REGISTER"

AUTH_SVC_SIGN_IN=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "AuthServiceSignInArn") | .OutputValue')
echo "AuthService Sign In = $AUTH_SVC_SIGN_IN"

TEMPLATE_URL="https://${WORKSHOP_BUCKET}.s3.amazonaws.com/lab4.template"
echo "CloudFormation template URL = $TEMPLATE_URL"

if [ -z "$WORKSHOP_STACK" ] \
	|| [ -z "$WORKSHOP_BUCKET" ] \
	|| [ -z "$RDS_SECURITY_GROUP" ] \
	|| [ -z "$PRIVATE_SUBNETS" ] \
	|| [ -z "$TENANT_SVC_GET_ALL" ] \
	|| [ -z "$TENANT_SVC_GET_ID" ] \
	|| [ -z "$TENANT_SVC_UPDATE" ] \
	|| [ -z "$TENANT_SVC_INSERT" ] \
	|| [ -z "$TENANT_SVC_DELETE" ] \
	|| [ -z "$TENANT_SVC_NEXT_DB" ] \
	|| [ -z "$TENANT_SVC_UPDATE_USER_POOL" ] \
	|| [ -z "$ORDER_SVC_GET_ALL" ] \
	|| [ -z "$ORDER_SVC_UPDATE" ] \
	|| [ -z "$ORDER_SVC_INSERT" ] \
	|| [ -z "$ORDER_SVC_DELETE" ] \
	|| [ -z "$REG_SVC_REGISTER" ] \
	|| [ -z "$AUTH_SVC_SIGN_IN" ]; then
	echo "Missing required environment variables. Please make sure the lab3 CloudFormation stack has completed successfully."
	exit 1
fi

# Build and upload the artifacts for Lab 4
cd /home/ec2-user/environment/saas-factory-serverless-workshop/lab4
# Build the layer first because it is a dependency in the subsequent POMs
cd layers/serverless-saas-layer
mvn
cd ../../order-service
mvn
cd ../product-service
mvn
cd ../
find . -type f -name '*-lambda.zip' -exec aws s3 cp {} s3://$WORKSHOP_BUCKET \;

echo
aws cloudformation create-stack --stack-name "${WORKSHOP_STACK}-lab4" --on-failure DO_NOTHING --capabilities CAPABILITY_NAMED_IAM --template-url "${TEMPLATE_URL}" --parameters \
ParameterKey=RDSSecurityGroup,ParameterValue="${RDS_SECURITY_GROUP}" \
ParameterKey=RDSSubnets,ParameterValue="${PRIVATE_SUBNETS}" \
ParameterKey=TenantServiceGetAllArn,ParameterValue="${TENANT_SVC_GET_ALL}" \
ParameterKey=TenantServiceGetByIdArn,ParameterValue="${TENANT_SVC_GET_ID}" \
ParameterKey=TenantServiceUpdateArn,ParameterValue="${TENANT_SVC_UPDATE}" \
ParameterKey=TenantServiceInsertArn,ParameterValue="${TENANT_SVC_INSERT}" \
ParameterKey=TenantServiceDeleteArn,ParameterValue="${TENANT_SVC_DELETE}" \
ParameterKey=TenantServiceNextDbArn,ParameterValue="${TENANT_SVC_NEXT_DB}" \
ParameterKey=TenantServiceUpdateUserPoolArn,ParameterValue="${TENANT_SVC_UPDATE_USER_POOL}" \
ParameterKey=OrderServiceGetAllArn,ParameterValue="${ORDER_SVC_GET_ALL}" \
ParameterKey=OrderServiceGetByIdArn,ParameterValue="${ORDER_SVC_GET_ID}" \
ParameterKey=OrderServiceUpdateArn,ParameterValue="${ORDER_SVC_UPDATE}" \
ParameterKey=OrderServiceInsertArn,ParameterValue="${ORDER_SVC_INSERT}" \
ParameterKey=OrderServiceDeleteArn,ParameterValue="${ORDER_SVC_DELETE}" \
ParameterKey=RegistrationServiceRegisterArn,ParameterValue="${REG_SVC_REGISTER}" \
ParameterKey=AuthServiceSignInArn,ParameterValue="${AUTH_SVC_SIGN_IN}" \
ParameterKey=WorkshopS3Bucket,ParameterValue="${WORKSHOP_BUCKET}"

cd /home/ec2-user/environment/saas-factory-serverless-workshop/resources