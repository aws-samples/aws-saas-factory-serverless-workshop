# Lab 5 - Cleanup.

We hope you had a great time doing the labs.
Now that you have completed all the labs, it's time to do some clean up.
Please follow below steps to execute the cleanup scripts


## Step-By-Step Guide

1.	To simplify the cleanup process you can execute the **delete_all.sh** bash script below.
    This script will delete the cloudformation stacks related to the Lab in the following order; lab4,lab3,Tenant stack, lab2,lab0 & 1
    The process will take approx 30 mins to complete

    ```
    cd aws-saas-factory-serverless-workshop/lab5
    bash cleanup_all_labs.sh <region id>
    ```
    e.g:

    ```
    cd aws-saas-factory-serverless-workshop/lab5
    bash cleanup_all_labs.sh ap-southeast-2
    ```



