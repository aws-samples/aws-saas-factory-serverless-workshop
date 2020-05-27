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

args=("$@")

echo "Setting environment variables"
MY_AWS_REGION=$(aws configure list | grep region | awk '{print $2}')

MY_AWS_REGION=${args[0]}
echo "AWS Region = $MY_AWS_REGION"

STACK_OUTPUTS=$(aws cloudformation --region $MY_AWS_REGION describe-stacks | jq -r '.Stacks[] | select(.Outputs != null) | .Outputs[]')

WORKSHOP_STACK=$(echo $STACK_OUTPUTS | jq -r 'select(.OutputKey == "SaaSFactoryServerlessSaaSWorkshopStack") | .OutputValue')
echo "Workshop stack = $WORKSHOP_STACK"

echo "Deleting ${WORKSHOP_STACK}-lab4"
aws cloudformation delete-stack --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab4" 2>/dev/null

until [ "$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab4" --query Stacks[0].StackStatus --output text 2>/dev/null )" = "*"DELETE_IN_PROGRESS"*" ];
do
  echo -n "Delete Lab 4 is running. ";echo -e "\r"
  STATUS="$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab4" --query Stacks[0].StackStatus --output text 2>/dev/null )"
  RESULT=$?
  if [ $RESULT -eq 0 ]; then
    sleep 10s
    if [ $STATUS != "DELETE_IN_PROGRESS" ]; then
       echo "Delete Lab 4 status $STATUS"
       if [$STATUS != "DELETE_COMPLETE"]; then
          echo "Delete Lab 4 Status $STATUS"
       fi
    fi
  else
    echo "Delete Lab 4 complete."
    break
  fi
done

echo "Deleting ${WORKSHOP_STACK}-lab3"
aws cloudformation delete-stack --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab3" 2>/dev/null

until [ "$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab3" --query Stacks[0].StackStatus --output text 2>/dev/null )" = "*"DELETE_IN_PROGRESS"*" ];
do
  echo -n "Delete Lab 3 is running.";echo -e "\r"
  STATUS="$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab3" --query Stacks[0].StackStatus --output text 2>/dev/null )"
  RESULT=$?
  if [ $RESULT -eq 0 ]; then
    sleep 10s
    if [ $STATUS != "DELETE_IN_PROGRESS" ]; then
       echo "Delete Lab 3 status $STATUS"
       if [$STATUS != "DELETE_COMPLETE"]; then
          echo "Delete Lab 3 Status $STATUS"
       fi
    fi
  else
    echo "Delete Lab 3 complete."
    break
  fi
done

TENANT_STACK=$(aws cloudformation describe-stacks --region $MY_AWS_REGION | jq -r '.Stacks[] | select(.StackName | contains("Tenant")) | .StackName') 2>/dev/null;
aws cloudformation delete-stack --region $MY_AWS_REGION  --stack-name "${TENANT_STACK}" 2>/dev/null

until [ "$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${TENANT_STACK}" --query Stacks[0].StackStatus --output text 2>/dev/null )" = "*"DELETE_IN_PROGRESS"*" ];
do
  echo -n "Delete Tenant is running.";echo -e "\r"
  STATUS="$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${TENANT_STACK}" --query Stacks[0].StackStatus --output text 2>/dev/null)"
  RESULT=$?
  if [ $RESULT -eq 0 ]; then
    sleep 10s
    if [ $STATUS != "DELETE_IN_PROGRESS" ]; then
       echo "Delete Tenant status $STATUS"
       if [$STATUS != "DELETE_COMPLETE"]; then
          echo "Delete Tenant Status $STATUS"
       fi
    fi
  else
    echo "Delete Tenant complete."
    break
  fi
done

echo "Deleting ${WORKSHOP_STACK}-lab2"
aws cloudformation delete-stack --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab2" 2>/dev/null

until [ "$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab2" --query Stacks[0].StackStatus --output text 2>/dev/null)" = "*"DELETE_IN_PROGRESS"*" ];
do
  echo -n "Delete Lab 2 is running.";echo -e "\r"
  STATUS="$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}-lab2" --query Stacks[0].StackStatus --output text 2>/dev/null)"
  RESULT=$?
  if [ $RESULT -eq 0 ]; then
    sleep 10s
    if [ $STATUS != "DELETE_IN_PROGRESS" ]; then
       echo "Delete Lab 2 status $STATUS"
       if [$STATUS != "DELETE_COMPLETE"]; then
          echo "Delete Lab 2 Status $STATUS"
       fi
    fi
  else
    echo "Delete Lab 2 complete."
    break
  fi
done

echo "Deleting ${WORKSHOP_STACK}"
aws cloudformation delete-stack --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}" 2>/dev/null

until [ "$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}" --query Stacks[0].StackStatus --output text 2>/dev/null)" = "*"DELETE_IN_PROGRESS"*" ];
do
  echo -n "Delete Lab 0 & 1 is running.";echo -e "\r"
  STATUS="$(aws cloudformation describe-stacks --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}" --query Stacks[0].StackStatus --output text 2>/dev/null)"
  RESULT=$?
  if [ $RESULT -eq 0 ]; then
    sleep 10s
    if [ $STATUS != "DELETE_IN_PROGRESS" ]; then
      echo "Delete Lab 0 & 1 status $STATUS"
      if [$STATUS != "DELETE_COMPLETE"]; then
          echo "Retrying Delete ${WORKSHOP_STACK}"
          aws cloudformation delete-stack --region $MY_AWS_REGION --stack-name "${WORKSHOP_STACK}" 2>/dev/null
      fi
    fi
  else
    echo "Delete Lab 0 & 1 complete."
    break
  fi
done