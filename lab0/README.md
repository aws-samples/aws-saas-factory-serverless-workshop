# Lab 0 – Deploying, baseline resources

Before we begin with our Lab, we will deploy our baseline resources where our monolith application reside.
The following step in will deploy all pre-requisite resources for Lab 1 - 4.
This saves you from building these resources manually, so you can focus on migrating the monolith application to microservices.

## Step-By-Step Guide

1.	To deploy the resources in this lab you first need to have the following tool installed in your local machine.

    * [Git](https://git-scm.com/)
    * [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)

2.  Once you have them installed, we will clone the repository to our local machine. ( Run below command in your local machine command prompt ).

    `git clone https://github.com/aws-samples/aws-saas-factory-serverless-workshop.git`

3.  Then locate the **workshop.template** file under **aws-saas-factory-serverless-workshop/resources** folder.

    ```
    cd aws-saas-factory-serverless-workshop/resources
    ls workshop.template
    ```

    This will be the master cloudformation template that we will deploy in this lab.

4.  Create an S3 bucket in the same region where you will be launching the master cloudformation template.

    `aws mb s3://<unique bucket name> --region <regionid>`

    e.g:

    `aws mb s3://aws-serverless-workshop-bucket --region ap-southeast-2`

5.	Download CopyS3Objects.jar & ClearS3Bucket.jar from bucket to your local machine

    ```
    aws s3api get-object \
        --bucket aws-saas-factory-serverless-saas-workshop-us-west-2 \
        --key CopyS3Objects.jar CopyS3Objects.jar

    aws s3api get-object \
        --bucket aws-saas-factory-serverless-saas-workshop-us-west-2 \
        --key ClearS3Bucket.jar ClearS3Bucket.jar
    ```

6.	Upload those 2 JAR files to the S3 bucket you made in step #1

    ```
    aws s3 cp CopyS3Objects.jar s3://<bucket created in step 4>
    aws s3 cp ClearS3Bucket.jar s3://<bucket created in step 4>
    ```

    e.g:

    ```
    aws s3 cp CopyS3Objects.jar s3://aws-serverless-workshop-bucket
    aws s3 cp ClearS3Bucket.jar s3://aws-serverless-workshop-bucket
    ```

7.	Deploy the cloudformation stack using the `workshop.template`
8.
    ```
    aws cloudformation create-stack --stack-name <stack name> \
                                    --template-body file://aws-saas-factory-serverless-workshop/resources/workshop.template \
                                    --parameters    ParameterKey="SourceBucket",ParameterValue="aws-saas-factory-serverless-saas-workshop-us-west-2" \
                                                    ParameterKey="EETeamRoleArn",ParameterValue="" \
                                                    ParameterKey="EEAssetsBucket",ParameterValue="<bucket created in step 4>" \
                                                    ParameterKey="EEAssetsKeyPrefix",ParameterValue="" --capabilities CAPABILITY_NAMED_IAM --region <regionid>;
    ```
    e.g:
    ```
    aws cloudformation create-stack --stack-name aws-serverless-workshop \
                                    --template-body file://aws-saas-factory-serverless-workshop/resources/workshop.template \
                                    --parameters    ParameterKey="SourceBucket",ParameterValue="aws-saas-factory-serverless-saas-workshop-us-west-2" \
                                                    ParameterKey="EETeamRoleArn",ParameterValue="" \
                                                    ParameterKey="EEAssetsBucket",ParameterValue="aws-serverless-workshop-bucket" \
                                                    ParameterKey="EEAssetsKeyPrefix",ParameterValue="" --capabilities CAPABILITY_NAMED_IAM --region ap-southeast-2;
    ```


## Review

The goal of Lab 0 was to pre-create all the resources needed for Lab 1 - 4

You have now completed Lab 0.

[Continue to Lab 1](../lab1/README.md)



