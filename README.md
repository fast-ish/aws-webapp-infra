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

this cdk application provisions a complete serverless web application infrastructure with five main components:

### infrastructure components

#### 1. network (vpc)
+ **public subnets**: host nat gateways and internet-facing resources
+ **private subnets**: isolate backend resources (cognito, lambda functions)
+ **internet gateway**: enables outbound internet access
+ **nat gateways**: allow private subnet resources to access the internet
+ **route tables**: manage traffic flow between subnets and internet

#### 2. authentication (cognito)
+ **user pool**: manages user registration, authentication, and profile data
+ **password policy**: enforces minimum length of 8 characters
+ **mfa configuration**: supports optional multi-factor authentication
+ **standard attributes**: email (required and mutable)
+ **custom attributes**: extensible user profile fields
+ **email verification**: automated via ses integration
+ **account recovery**: email-based password reset flow
+ **vpc integration**: deployed in private subnets for enhanced security

#### 3. database (dynamodb)
+ **tables**: nosql tables for application data storage
+ **partition keys**: optimized for access patterns
+ **billing mode**: pay-per-request (on-demand) or provisioned capacity
+ **encryption**: server-side encryption at rest enabled by default
+ **point-in-time recovery**: optional backup and restore capability
+ **global secondary indexes**: additional query patterns as needed

#### 4. email service (ses)
+ **domain identity**: verifies your domain for sending emails
+ **email identity**: verifies specific email addresses
+ **dkim signing**: authenticates emails to prevent spoofing
+ **route 53 integration**: automatically creates required dns records
+ **sending authorization**: iam policies for secure email sending
+ **email templates**: transactional email templates for user notifications

#### 5. api gateway
+ **rest api**: http endpoints for client-server communication
+ **cognito authorizer**: validates jwt tokens from cognito user pool
+ **lambda integration**: routes requests to backend lambda functions
+ **cors configuration**: enables cross-origin requests from web clients
+ **deployment stages**: separate environments (dev, staging, prod)
+ **request validation**: schema-based validation of api requests
+ **usage plans**: optional rate limiting and throttling

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

## architecture

the webapp infrastructure uses a layered architecture with nested stacks:

```
DeploymentStack (main)
├── NetworkNestedStack      (vpc, subnets, routing)
├── SesNestedStack          (email service setup)
├── AuthNestedStack         (cognito user pool) [depends on ses]
├── DbNestedStack           (dynamodb tables)
└── ApiNestedStack          (api gateway + lambda) [depends on auth]
```

**dependency chain**:
1. network and ses are created first (independent)
2. auth waits for ses (email verification dependency)
3. api waits for auth (cognito authorizer dependency)
4. db is created independently

## deployment

### step 1: clone repositories

```bash
gh repo clone fast-ish/cdk-common
gh repo clone fast-ish/aws-webapp-infra
```

### step 2: build projects

```bash
mvn -f cdk-common/pom.xml clean install
mvn -f aws-webapp-infra/pom.xml clean install
```

### step 3: setup local maven repository symlink

the infrastructure configuration references lambda function artifacts from the maven local repository at `/root/.m2/repository`. this path works in aws codepipeline (which runs as root), but locally your maven repository is at `~/.m2/repository`.

to ensure the same configuration works in both environments, create a symlink:

```bash
sudo ln -s ~/.m2 /root/.m2
```

**why this is needed**:
+ infrastructure templates reference lambda jar/zip files using absolute paths like `/root/.m2/repository/ui/webapp/fn/...`
+ locally, maven installs artifacts to `~/.m2/repository` (e.g., `/Users/yourname/.m2/repository`)
+ in aws codepipeline, maven runs as root and installs to `/root/.m2/repository`
+ the symlink makes `/root/.m2` point to your local `~/.m2`, allowing the same configuration to work in both environments
+ this eliminates the need to maintain separate configurations or use relative paths that depend on the build directory structure

### step 4: configure deployment

create `aws-webapp-infra/infra/cdk.context.json` from `aws-webapp-infra/infra/cdk.context.template.json`:

**required configuration parameters**:

| parameter | description | example |
|-----------|-------------|---------|
| `:account` | aws account id (12-digit number) | `123456789012` |
| `:region` | aws region for deployment | `us-west-2` |
| `:domain` | registered domain in route 53 for ses | `fasti.sh` |
| `:ses:hosted:zone` | route 53 hosted zone id for dns records | `Z1234567890ABC` |
| `:ses:email` | email address for ses verification | `no-reply@fasti.sh` |
| `:environment` | environment name (do not change) | `prototype` |
| `:version` | resource version identifier | `v1` |

**notes**:
+ `:environment` and `:version` map to resource files at `aws-webapp-infra/infra/src/main/resources/prototype/v1`
+ domain must be registered and hosted in route 53 before deployment
+ email address will receive a verification link from ses

### step 5: deploy infrastructure

```bash
cd aws-webapp-infra/infra

# preview changes
cdk synth

# deploy all stacks
cdk deploy
```

**what gets deployed**:
+ 1 main cloudformation stack
+ 5 nested cloudformation stacks (network, ses, auth, db, api)
+ vpc with 2 azs, public/private subnets
+ cognito user pool with vpc endpoints
+ dynamodb tables as defined in configuration
+ ses domain and email identity with dkim
+ api gateway with lambda integrations

### step 6: verify ses email

after deployment completes:
1. check the email inbox for `:ses:email`
2. click the verification link from amazon ses
3. confirmation is required before cognito can send emails

## license

[mit license](LICENSE)

for your convenience, you can find the full mit license text at

+ [https://opensource.org/license/mit/](https://opensource.org/license/mit/) (official osi website)
+ [https://choosealicense.com/licenses/mit/](https://choosealicense.com/licenses/mit/) (choose a license website)
