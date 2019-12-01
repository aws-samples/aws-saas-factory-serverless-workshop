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

MY_AWS_REGION=$(aws configure list | grep region | awk '{print $2}')
echo "AWS Region = $MY_AWS_REGION"

STACK_OUTPUTS=$(aws cloudformation describe-stacks | jq -r '.Stacks[] | select(.Outputs != null) | .Outputs[]')

WORKSHOP_BUCKET=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "WorkshopBucket") | .OutputValue')
echo "Workshop bucket = $WORKSHOP_BUCKET"

LAMBDA_CODE=OrderService-lambda.zip

FUNCTIONS=("saas-factory-srvls-wrkshp-orders-get-all-${MY_AWS_REGION}" 
	"saas-factory-srvls-wrkshp-orders-get-by-id-${MY_AWS_REGION}"
	"saas-factory-srvls-wrkshp-orders-insert-${MY_AWS_REGION}"
	"saas-factory-srvls-wrkshp-orders-update-${MY_AWS_REGION}"
	"saas-factory-srvls-wrkshp-orders-delete-${MY_AWS_REGION}"
)

for FUNCTION in ${FUNCTIONS[@]}; do
	#echo $FUNCTION
	aws lambda --region $MY_AWS_REGION update-function-code --function-name $FUNCTION --s3-bucket $WORKSHOP_BUCKET --s3-key $LAMBDA_CODE
done
