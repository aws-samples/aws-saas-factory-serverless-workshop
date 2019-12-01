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

cd /home/ec2-user

# Get the tenant who owns this environment
PREFIX=''
if [ -f /etc/profile.d/saas.sh ]; then
  . /etc/profile.d/saas.sh
  PREFIX="${TENANT_ID}_"
fi

AWS_REGION=$(curl -s http://169.254.169.254/latest/dynamic/instance-identity/document | jq -r '.region')
DB_HOST=$(aws ssm get-parameters --region $AWS_REGION --names "${PREFIX}DB_HOST" | jq -r '.Parameters[0].Value')
DB_NAME=$(aws ssm get-parameters --region $AWS_REGION --names "${PREFIX}DB_NAME" | jq -r '.Parameters[0].Value')
DB_USER=$(aws ssm get-parameters --region $AWS_REGION --names "${PREFIX}DB_USER" | jq -r '.Parameters[0].Value')
DB_PASS="${PREFIX}DB_PASS"

export AWS_REGION DB_HOST DB_NAME DB_USER DB_PASS

java -jar /home/ec2-user/application.jar > /dev/null 2> /dev/null < /dev/null &