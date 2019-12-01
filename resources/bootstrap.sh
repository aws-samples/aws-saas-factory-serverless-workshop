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

# Supported regions (Cloud9 limitation)
# us-east-1      N Virginia 
# us-east-2      Ohio
# us-west-2      Oregon
# eu-west-1      Ireland
# eu-central-1   Frankfurt
# ap-southeast-1 Singapore
# ap-northeast-1 Tokyo


#Bootstrap a Cloud9 IDE instance for Java 8 development
sudo yum install -y java-1.8.0-openjdk-devel
sudo update-alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk.x86_64/bin/javac

sudo curl -L http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -o /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven

# Pull a mirror of the repository and push it to the CodeCommit repository generated
# for this workshop
MY_REGION=$(aws configure list | grep region | awk '{print $2}')
DIST_REPO=https://github.com/aws-samples/aws-saas-factory-serverless-workshop.git

git config --global credential.helper '!aws codecommit credential-helper $@'
git config --global credential.UseHttpPath true

cd /home/ec2-user/environment
git clone --mirror $DIST_REPO github-dist
cd github-dist
git push https://git-codecommit.$MY_REGION.amazonaws.com/v1/repos/saas-factory-serverless-workshop --all
cd ..
rm -rf github-dist
git clone https://git-codecommit.$MY_REGION.amazonaws.com/v1/repos/saas-factory-serverless-workshop
cd /home/ec2-user/environment/saas-factory-serverless-workshop
