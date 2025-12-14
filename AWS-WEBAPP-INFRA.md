# aws-webapp-infra

Serverless web application infrastructure with auth, API, and database. Java 21, AWS CDK + Lambda.

**Depends on**: `cdk-common` (install first with `mvn install -DskipTests`)

## Architecture

```
WebappStack (main)
  ├── NetworkNestedStack    # VPC
  ├── SesNestedStack        # Email service
  ├── AuthNestedStack       # Cognito (depends on SES)
  ├── DbNestedStack         # DynamoDB
  └── ApiNestedStack        # API Gateway + Lambda (depends on Auth)
```

## Directory Layout

```
fn/                               # Lambda functions
  layer/
    shared/                       # Logging, JsonUtil, models
    api/                          # API helpers
    auth/                         # Auth helpers
  api/user/                       # User management Lambda
  auth/
    message/                      # Cognito custom message trigger
    post-confirmation/            # Post-signup handler

infra/                            # CDK infrastructure
  src/main/java/fasti/sh/webapp/
    Launch.java                   # CDK app entry point
    stack/
      WebappStack.java            # Main orchestrator
      WebappReleaseConf.java      # Configuration record
      model/                      # ApiConf, AuthConf, DbConf, SesConf
      nested/                     # Nested stack implementations
```

## Configuration

Edit `infra/cdk.context.json`:
- AWS account/region
- Domain name
- SES hosted zone and email
- Environment and version

## Commands

```bash
mvn clean install                 # Build all (fn + infra)
cd infra && cdk synth             # Synthesize
cd infra && cdk deploy            # Deploy
```

## Key Files

- `infra/.../WebappStack.java` - Stack orchestration with dependencies
- `infra/.../nested/*.java` - Api, Auth, Db, Ses nested stacks
- `fn/api/user/Handler.java` - User API Lambda
- `fn/auth/message/Handler.java` - Cognito message trigger

## Testing

369 unit tests focused on configuration model validation.

## Don't

- Deploy without Lambda layer symlink: `sudo ln -s ~/.m2 /root/.m2`
- Skip SES email verification post-deploy
