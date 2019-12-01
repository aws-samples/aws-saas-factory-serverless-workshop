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

WORKSHOP_BUCKET=$1

if [ $(basename $PWD) != "resources" ]; then
	echo "Can't find resources directory"
	exit 1
fi

for LAMBDA in $(ls -d */); do
	if [ $LAMBDA != "custom-resources/" ]; then
		cd $LAMBDA
		mvn
		cd ..
	fi
done

find . -type f -name '*-lambda.zip' -exec aws s3 cp {} s3://$WORKSHOP_BUCKET \;

cd custom-resources
for CFN_RES in $(ls -d */); do
	cd $CFN_RES
	mvn
	cd ..
done

find . -type f -name '*.jar' -a ! -name '*original*' -exec aws s3 cp {} s3://$WORKSHOP_BUCKET \;

cd ..
for CFN_TEMPLATE in $(ls *.template); do
	aws s3 cp $CFN_TEMPLATE s3://$WORKSHOP_BUCKET
done

read -p "Launch Workshop CloudFormation Template? (y/n)?" ANSWER
case ${ANSWER:0:1} in
	y|Y )
		echo Yes
		# Auto expand
		# Named IAM
	;;
	* )
		exit 0
	;;
esac

exit 0
