# aws-webapp-infra

<div align="center">

*fastish aws cdk application written in java that provisions a complete serverless web application infrastructure with
authentication, database, email services, and api gateway for building modern web applications.*

[![license: mit](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![java](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://www.oracle.com/java/)
[![aws cdk](https://img.shields.io/badge/AWS%20CDK-latest-orange.svg)](https://aws.amazon.com/cdk/)
[![vpc](https://img.shields.io/badge/Amazon-VPC-ff9900.svg)](https://aws.amazon.com/vpc/)
[![cognito](https://img.shields.io/badge/Amazon-Cognito-ff9900.svg)](https://aws.amazon.com/cognito/)
[![ses](https://img.shields.io/badge/Amazon-SES-ff9900.svg)](https://aws.amazon.com/ses/)
[![dynamodb](https://img.shields.io/badge/Amazon-DynamoDB-ff9900.svg)](https://aws.amazon.com/dynamodb/)
[![api gateway](https://img.shields.io/badge/Amazon-APIGateway-ff9900.svg)](https://aws.amazon.com/api-gateway/)
[![lambda](https://img.shields.io/badge/Amazon-Lambda-ff9900.svg)](https://aws.amazon.com/lambda/)

</div>

## overview

+ complete vpc with public and private subnets
+ cognito user pool with custom configuration for authentication
+ dynamodb tables for user data storage
+ ses (simple email service) configuration for transactional emails
+ api gateway with cognito authorizer
+ aws lambda functions integrated with api gateway
+ iam roles and security configurations

## prerequisites

+ [java 21+](https://sdkman.io/)
+ [maven](https://maven.apache.org/download.cgi)
+ [aws cli](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
+ [aws cdk cli](https://docs.aws.amazon.com/cdk/v2/guide/getting-started.html)
+ [github cli](https://cli.github.com/)
+ registered domain in Route 53 for SES integration
+ prepare aws environment by running `cdk bootstrap` with the appropriate aws account and region:

  ```bash
  cdk bootstrap aws://<account-id>/<region>
  ```

    + replace `<account-id>` with your aws account id and `<region>` with your desired aws region (e.g., `us-west-2`).
    + this command sets up the necessary resources for deploying cdk applications, such as an S3 bucket for storing
      assets and a CloudFormation execution role
    + for more information, see the aws cdk documentation:
        + https://docs.aws.amazon.com/cdk/v2/guide/bootstrapping.html
        + https://docs.aws.amazon.com/cdk/v2/guide/ref-cli-cmd-bootstrap.html

## deployment

1. clone the required repositories:
   ```bash
   gh repo clone fast-ish/cdk-common
   gh repo clone fast-ish/aws-webapp-infra
   ```

2. build projects:
   ```bash
   mvn -f cdk-common/pom.xml clean install
   mvn -f aws-webapp-infra/pom.xml clean install
   ```

3. create `aws-webapp-infra/infra/cdk.context.json` from `aws-webapp-infra/infra/cdk.context.template.json` with your
   aws account details
    + `:account` - aws account id
    + `:region` - aws region (e.g., `us-west-2`
    + `:domain` - registered domain name for ses (e.g., `fasti.sh`)
    + `:ses:hosted:zone` - route 53 hosted zone id for ses (e.g., `Z1234567890`)
    + `:ses:email` - email address for ses verification (e.g., `john.doe@email.com`)
    + `:environment` - this should not be changed unless you add a new set of resources to configure that environment
    + `:version` - version of the resources to deploy, this is used to differentiate between different versions of the
      resources
        + currently set to prototype/v1 for the resources at `aws-webapp-infra/infra/src/main/resources/prototype/v1`

4. deploy web application infrastructure:
   ```bash
   cd aws-webapp-infra/infra
   
   cdk synth
   cdk deploy
   ```

5. verify ses email address:
    + verify the email address specified in `cdk.context.json`
    + follow the instructions to complete the verification process emailed to the specified address

## license

[mit license](LICENSE)

for your convenience, you can find the full mit license text at

+ [https://opensource.org/license/mit/](https://opensource.org/license/mit/) (official osi website)
+ [https://choosealicense.com/licenses/mit/](https://choosealicense.com/licenses/mit/) (choose a license website)
