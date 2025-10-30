# AI Context: aws-webapp-infra

> **Purpose**: This document helps AI assistants quickly understand the aws-webapp-infra codebase architecture, patterns, and conventions.

## What is aws-webapp-infra?

A **complete serverless web application infrastructure** built with AWS CDK that provides:
- Production-ready authentication (Cognito)
- Serverless database (DynamoDB)
- Email services (SES)
- API Gateway with Lambda functions
- Secure networking (VPC with public/private subnets)

**Key Technologies**: Java 21, AWS CDK 2.221.0, cdk-common library, Lambda, Cognito, DynamoDB, SES, API Gateway

## Architecture Overview

### Nested Stack Pattern

```
DeploymentStack (main)
├── NetworkNestedStack      # VPC, subnets, routing
├── SesNestedStack          # Email service setup
├── AuthNestedStack         # Cognito user pool [depends on SES]
├── DbNestedStack           # DynamoDB tables
└── ApiNestedStack          # API Gateway + Lambda [depends on Auth]
```

**Dependency Chain**:
1. Network and SES created first (independent)
2. Auth waits for SES (email verification)
3. API waits for Auth (Cognito authorizer)
4. DB created independently

### Project Structure

```
aws-webapp-infra/
├── fn/                          # Lambda functions
│   ├── layer/                   # Lambda layers (shared code)
│   │   ├── shared/             # Common utilities
│   │   ├── api/                # API-specific layer
│   │   └── auth/               # Auth-specific layer
│   ├── api/                     # API Lambda functions
│   │   └── user/               # User management endpoints
│   └── auth/                    # Auth Lambda functions
│       ├── message/            # Custom message handler
│       └── post-confirmation/  # Post-signup handler
│
├── infra/                       # CDK infrastructure
│   ├── src/main/
│   │   ├── java/fasti/sh/webapp/
│   │   │   ├── Launch.java              # CDK App entry point
│   │   │   └── stack/
│   │   │       ├── DeploymentStack.java # Main stack
│   │   │       ├── DeploymentConf.java  # Configuration record
│   │   │       ├── model/               # Configuration models
│   │   │       │   ├── ApiConf.java
│   │   │       │   ├── AuthConf.java
│   │   │       │   ├── DbConf.java
│   │   │       │   └── SesConf.java
│   │   │       └── nested/              # Nested stack implementations
│   │   │           ├── ApiNestedStack.java
│   │   │           ├── AuthNestedStack.java
│   │   │           ├── DbNestedStack.java
│   │   │           └── SesNestedStack.java
│   │   └── resources/prototype/v1/      # Configuration templates
│   │       ├── apigw/                   # API Gateway configs
│   │       ├── cognito/                 # Cognito configs
│   │       ├── dynamodb/                # DynamoDB configs
│   │       ├── ses/                     # SES configs
│   │       └── vpc/                     # VPC configs
│   └── src/test/
│       ├── java/fasti/sh/webapp/
│       │   └── stack/                   # Test suites
│       │       ├── DeploymentConfTest.java
│       │       └── model/               # Model tests (369 tests)
│       └── resources/                   # Test configuration files
```

## Core Concepts

### 1. Configuration-Driven Infrastructure

All infrastructure is defined through YAML/JSON configuration files processed by Mustache templates:

```yaml
# resources/prototype/v1/cognito/user-pool.yaml
name: "{{hosted:name}}"
passwordPolicy:
  minimumLength: 8
  requireLowercase: true
mfaConfiguration: OPTIONAL
```

### 2. cdk-common Dependency

This project heavily depends on the `cdk-common` library which provides:
- High-level AWS constructs (`VpcConstruct`, `DynamoDbConstruct`, etc.)
- Template processing (`Template.java`, `Mapper.java`)
- Common models (`Common`, `NetworkConf`, etc.)
- Naming conventions (`Format.java`)

**All nested stacks use cdk-common constructs internally.**

### 3. Lambda Architecture

**Layers**: Shared code packaged as Lambda layers
- `fn.shared`: Core utilities, AWS SDK wrappers
- `api.fn.shared`: API-specific helpers
- `auth.fn.shared`: Auth-specific helpers

**Functions**: Individual Lambda functions
- Depend on layers for shared code
- Built as separate Maven modules
- Packaged with Maven Shade plugin

### 4. Context-Driven Configuration

Infrastructure uses CDK context from `cdk.context.json`:

```json
{
  "host:account": "123456789012",
  "host:region": "us-west-2",
  "hosted:domain": "fasti.sh",
  "hosted:environment": "prototype",
  "hosted:version": "v1",
  "ses:hosted:zone": "Z1234567890ABC",
  "ses:email": "no-reply@fasti.sh"
}
```

**Context Variables**:
- `host:*` - AWS account/region context
- `hosted:*` - Application-level context
- Service-specific: `ses:*`, `vpc:*`, etc.

## Key Patterns

### 1. Nested Stack Pattern

Each nested stack:
- Extends `software.amazon.awscdk.NestedStack`
- Receives configuration via record classes
- Uses cdk-common constructs for resource creation
- Exposes getters for created resources

Example:
```java
public class AuthNestedStack extends NestedStack {
  @Getter
  private final UserPool userPool;

  public AuthNestedStack(Construct scope, Common common, AuthConf conf,
                         Vpc vpc, NestedStackProps props) {
    super(scope, Format.id("webapp.auth", common.id()), props);

    // Load config from template
    var poolConf = Template.load(UserPoolConf.class,
        "prototype/v1/cognito/user-pool.yaml", ctx());

    // Create construct using cdk-common
    var poolConstruct = new UserPoolConstruct(this, common, poolConf);
    this.userPool = poolConstruct.userPool();
  }
}
```

### 2. Template Processing

Templates use Mustache syntax with CDK context injection:

```yaml
# Input template
name: "{{hosted:name}}-user-pool"
domain: "{{hosted:domain}}"

# After processing (with context)
name: "webapp-user-pool"
domain: "fasti.sh"
```

### 3. Configuration Records

All configuration is defined as Java records:

```java
public record AuthConf(
  String userPool,           // Path to user pool config
  String userPoolClient,     // Path to client config
  // ... other fields
) {}
```

### 4. Lambda Layer Pattern

Lambda functions reference layers from local Maven repo:

```yaml
# Lambda config
layers:
  - name: "shared"
    asset: "/root/.m2/repository/ui/webapp/fn/fn.shared/1.0.0-SNAPSHOT/fn.shared-1.0.0-SNAPSHOT.zip"
```

**Local development**: Symlink `~/.m2` to `/root/.m2` for consistency with CI/CD

## Testing Strategy

### Model/Configuration Tests (369 tests)

High-value tests validating business logic:
- `DeploymentConfTest`: 64 tests for deployment configuration
- `ApiConfTest`: 73 tests for API configuration
- `AuthConfTest`: 69 tests for auth configuration
- `DbConfTest`: 69 tests for database configuration
- `SesConfTest`: 65 tests for email configuration
- `LaunchTest`: 29 tests for CDK app entry point

**All tests pass**: `mvn clean test` → 369/369 passing

### What We Don't Test

Integration/CDK tests were removed (low value, high maintenance):
- Nested stack instantiation tests
- CDK resource creation tests
- Mock-heavy integration tests

**Rationale**: These tests were brittle, broke with dependency updates, and didn't test real behavior.

## Dependencies

### Core Dependencies
- **AWS CDK**: 2.221.0
- **AWS SDK v2**: 2.37.1
- **Jackson BOM**: 2.20.0 (unified version management)
- **Lombok**: 1.18.42
- **JUnit 5**: 6.0.0
- **Mockito**: 5.20.0

### Lambda Runtime
- **Java**: 21
- **Lambda Runtime**: `java21`
- **AWS Lambda Java Core**: 1.4.0
- **AWS Lambda Java Events**: 3.16.1

## Deployment Workflow

### 1. Prerequisites
```bash
# Install tools
java 21+, maven, aws-cli, cdk-cli, gh-cli

# Bootstrap CDK
cdk bootstrap aws://ACCOUNT-ID/REGION

# Setup local Maven symlink
sudo ln -s ~/.m2 /root/.m2
```

### 2. Build
```bash
# Build cdk-common dependency
mvn -f cdk-common/pom.xml clean install

# Build this project
mvn -f aws-webapp-infra/pom.xml clean install
```

### 3. Configure
```bash
# Copy template
cp infra/cdk.context.template.json infra/cdk.context.json

# Edit with your AWS account, region, domain, etc.
```

### 4. Deploy
```bash
cd infra
cdk synth    # Preview
cdk deploy   # Deploy all stacks
```

### 5. Post-Deployment
- Verify SES email (check inbox for verification link)
- Test API endpoints
- Validate Cognito user flows

## Code Conventions

### Naming
- **Stacks**: `{Service}NestedStack` (e.g., `AuthNestedStack`)
- **Configs**: `{Service}Conf` (e.g., `AuthConf`)
- **Records**: Use `record` keyword, not classes
- **IDs**: Use `Format.id()` from cdk-common

### Logging
```java
@Slf4j
public class AuthNestedStack extends NestedStack {
  public AuthNestedStack(...) {
    log.debug("AuthNestedStack [common: {} conf: {}]", common, conf);
  }
}
```

### Template Loading
```java
var config = Template.load(
    ConfigClass.class,
    "prototype/v1/service/file.yaml",
    ctx()  // CDK context for Mustache variables
);
```

## Common Tasks

### Adding a New Lambda Function

1. Create module under `fn/api/` or `fn/auth/`
2. Add dependency on appropriate layer
3. Implement `RequestHandler<Input, Output>`
4. Configure in `resources/prototype/v1/fn/`
5. Wire up in `ApiNestedStack`

### Adding a DynamoDB Table

1. Add table config to `resources/prototype/v1/dynamodb/`
2. Update `DbConf` record with table reference
3. Load config in `DbNestedStack`
4. Create table using `DynamoDbConstruct` from cdk-common

### Adding a New Stack

1. Create `{Service}Conf` record in `stack/model/`
2. Create `{Service}NestedStack` in `stack/nested/`
3. Add to `DeploymentConf` and `DeploymentStack`
4. Add configuration templates to `resources/`
5. Write model tests in `test/stack/model/`

## Troubleshooting

### Lambda Layer Not Found
```bash
# Ensure Maven artifacts are installed
mvn -f fn/pom.xml clean install

# Check symlink
ls -la /root/.m2
```

### Template Processing Errors
- Check `cdk.context.json` has all required variables
- Verify template paths match `resources/` structure
- Use `{{variable}}` syntax for Mustache

### CDK Synth Fails
```bash
# Clean and rebuild
mvn clean install
cd infra
rm -rf cdk.out
cdk synth
```

### SES Email Not Verified
- Check email inbox for verification link
- Verify Route 53 hosted zone is correct
- Check SES service quotas

## Key Differences from cdk-common

| Aspect | cdk-common | aws-webapp-infra |
|--------|------------|------------------|
| Purpose | Library of reusable constructs | Deployable CDK app |
| Structure | Flat construct library | Nested stack architecture |
| Configuration | Per-construct configs | Hierarchical config tree |
| Testing | Construct + model tests | Model tests only |
| Deployment | N/A (library) | Full stack deployment |

## Resources

- [README.md](../README.md) - Overview and quickstart
- [USAGE.md](../USAGE.md) - Detailed usage guide
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [CHANGELOG.md](../CHANGELOG.md) - Version history
- [cdk-common](https://github.com/fast-ish/cdk-common) - Dependency library

## Version Info

- **Java**: 21+
- **AWS CDK**: 2.221.0
- **Maven**: 3.9+
- **Package**: `fasti.sh.webapp`
- **Current Version**: 1.0.0-SNAPSHOT

---

**Last Updated**: 2025-10-29
**Test Status**: 369/369 passing ✅
**Build Status**: All dependencies updated to latest versions
