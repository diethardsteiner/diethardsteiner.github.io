#!/bin/bash

STACK_NAME=hat
REGION=eu-west-2
CLI_PROFILE=Administrator
EC2_INSTANCE_TYPE=t2.micro

# Deploy the CloudFormation template

echo -e "\n\n=========== Deploying main.yml ============"

aws cloudformation deploy \
   --region $REGION \
   --profile $CLI_PROFILE \
   --stack-name $STACK_NAME \
   --template-file hop-cloudformation-template.yml \
   --no-fail-on-empty-changeset \
   --capabilities CAPABILITY_NAMED_IAM \
   --parameter-overrides \
     EC2InstanceType=$EC2_INSTANCE_TYPE
     
# If the deployment succeeded, show the DNS name of the created instance
if [ $? -eq 0 ]; then
  aws cloudformation list-exports \
    --profile $CLI_PROFILE \
    --query "Exports[?Name=='InstanceEndpoint'].Value"
fi

------

STACK_NAME=hat
REGION=eu-west-2
CLI_PROFILE=Administrator
EC2_INSTANCE_TYPE=t2.micro

aws cloudformation create-stack \
   --region $REGION \
   --profile $CLI_PROFILE \
   --stack-name $STACK_NAME \
   --template-body file://hop-cloudformation-template.yml \
   --disable-rollback \
   --capabilities CAPABILITY_NAMED_IAM \
   --parameters \
   ParameterKey=EC2InstanceType,ParameterValue=$EC2_INSTANCE_TYPE
