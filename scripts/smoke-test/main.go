package main

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/apigateway"
	"github.com/aws/aws-sdk-go-v2/service/cloudwatchlogs"
	"github.com/aws/aws-sdk-go-v2/service/cognitoidentityprovider"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/ec2"
	ec2types "github.com/aws/aws-sdk-go-v2/service/ec2/types"
	"github.com/aws/aws-sdk-go-v2/service/lambda"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/sesv2"
	"github.com/aws/aws-sdk-go-v2/service/sns"
)

const (
	colorReset  = "\033[0m"
	colorRed    = "\033[31m"
	colorGreen  = "\033[32m"
	colorYellow = "\033[33m"
	colorBlue   = "\033[34m"
	colorCyan   = "\033[36m"
)

type TestResult struct {
	Name    string
	Passed  bool
	Warning bool
	Message string
}

type TestSuite struct {
	deploymentID string
	region       string
	domain       string

	ec2Client       *ec2.Client
	sesClient       *sesv2.Client
	cognitoClient   *cognitoidentityprovider.Client
	dynamoClient    *dynamodb.Client
	apiGwClient     *apigateway.Client
	lambdaClient    *lambda.Client
	s3Client        *s3.Client
	snsClient       *sns.Client
	cloudwatchClient *cloudwatchlogs.Client

	results  []TestResult
	passed   int
	failed   int
	warnings int
}

func main() {
	printBanner()

	deploymentID := os.Getenv("DEPLOYMENT_ID")
	if deploymentID == "" {
		fmt.Printf("%s✗ DEPLOYMENT_ID environment variable is required%s\n", colorRed, colorReset)
		fmt.Println("  Example: DEPLOYMENT_ID=fastish-production go run main.go")
		os.Exit(1)
	}

	domain := os.Getenv("DOMAIN")
	if domain == "" {
		domain = "fasti.sh"
	}

	suite, err := NewTestSuite(deploymentID, domain)
	if err != nil {
		fmt.Printf("%s✗ Failed to initialize: %v%s\n", colorRed, err, colorReset)
		os.Exit(1)
	}

	suite.RunAll()
	suite.PrintSummary()

	if suite.failed > 0 {
		os.Exit(1)
	}
}

func printBanner() {
	fmt.Printf("%s", colorCyan)
	fmt.Println("╔═══════════════════════════════════════════════════════════════╗")
	fmt.Println("║        WEBAPP SMOKE TEST - Infrastructure Validation          ║")
	fmt.Println("╚═══════════════════════════════════════════════════════════════╝")
	fmt.Printf("%s\n", colorReset)
}

func NewTestSuite(deploymentID, domain string) (*TestSuite, error) {
	ctx := context.Background()
	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to load AWS config: %w", err)
	}

	return &TestSuite{
		deploymentID:    deploymentID,
		region:          cfg.Region,
		domain:          domain,
		ec2Client:       ec2.NewFromConfig(cfg),
		sesClient:       sesv2.NewFromConfig(cfg),
		cognitoClient:   cognitoidentityprovider.NewFromConfig(cfg),
		dynamoClient:    dynamodb.NewFromConfig(cfg),
		apiGwClient:     apigateway.NewFromConfig(cfg),
		lambdaClient:    lambda.NewFromConfig(cfg),
		s3Client:        s3.NewFromConfig(cfg),
		snsClient:       sns.NewFromConfig(cfg),
		cloudwatchClient: cloudwatchlogs.NewFromConfig(cfg),
	}, nil
}

func (s *TestSuite) RunAll() {
	fmt.Printf("  Deployment ID: %s%s%s\n", colorCyan, s.deploymentID, colorReset)
	fmt.Printf("  Region: %s%s%s\n", colorCyan, s.region, colorReset)
	fmt.Printf("  Domain: %s%s%s\n", colorCyan, s.domain, colorReset)

	s.testVPC()
	s.testSES()
	s.testCognito()
	s.testDynamoDB()
	s.testAPIGateway()
	s.testLambda()
	s.testSNS()
	s.testCloudWatch()
}

func (s *TestSuite) printHeader(title string) {
	fmt.Printf("\n%s━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━%s\n", colorBlue, colorReset)
	fmt.Printf("%s  %s%s\n", colorBlue, title, colorReset)
	fmt.Printf("%s━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━%s\n", colorBlue, colorReset)
}

func (s *TestSuite) printSection(title string) {
	fmt.Printf("\n%s▶ %s%s\n", colorYellow, title, colorReset)
}

func (s *TestSuite) pass(name string) {
	fmt.Printf("  %s✓%s %s\n", colorGreen, colorReset, name)
	s.passed++
	s.results = append(s.results, TestResult{Name: name, Passed: true})
}

func (s *TestSuite) fail(name string) {
	fmt.Printf("  %s✗%s %s\n", colorRed, colorReset, name)
	s.failed++
	s.results = append(s.results, TestResult{Name: name, Passed: false})
}

func (s *TestSuite) warn(name string) {
	fmt.Printf("  %s⚠%s %s\n", colorYellow, colorReset, name)
	s.warnings++
	s.results = append(s.results, TestResult{Name: name, Warning: true})
}

// -----------------------------------------------------------------------------
// VPC Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testVPC() {
	s.printHeader("VPC AND NETWORKING")
	ctx := context.Background()

	vpcName := fmt.Sprintf("%s-webapp-vpc", s.deploymentID)

	s.printSection("VPC")
	vpcs, err := s.ec2Client.DescribeVpcs(ctx, &ec2.DescribeVpcsInput{
		Filters: []ec2types.Filter{
			{Name: strPtr("tag:Name"), Values: []string{vpcName}},
		},
	})
	if err != nil {
		s.fail(fmt.Sprintf("VPC lookup: %v", err))
		return
	}

	if len(vpcs.Vpcs) == 0 {
		s.fail(fmt.Sprintf("VPC '%s' not found", vpcName))
		return
	}

	vpc := vpcs.Vpcs[0]
	s.pass(fmt.Sprintf("VPC '%s' exists (CIDR: %s)", vpcName, *vpc.CidrBlock))

	if vpc.State == ec2types.VpcStateAvailable {
		s.pass("VPC state: available")
	} else {
		s.fail(fmt.Sprintf("VPC state: %s", vpc.State))
	}

	// Check DNS settings
	dnsSupport, err := s.ec2Client.DescribeVpcAttribute(ctx, &ec2.DescribeVpcAttributeInput{
		VpcId:     vpc.VpcId,
		Attribute: ec2types.VpcAttributeNameEnableDnsSupport,
	})
	if err == nil && dnsSupport.EnableDnsSupport != nil && *dnsSupport.EnableDnsSupport.Value {
		s.pass("DNS support enabled")
	} else {
		s.warn("DNS support not enabled")
	}

	dnsHostnames, err := s.ec2Client.DescribeVpcAttribute(ctx, &ec2.DescribeVpcAttributeInput{
		VpcId:     vpc.VpcId,
		Attribute: ec2types.VpcAttributeNameEnableDnsHostnames,
	})
	if err == nil && dnsHostnames.EnableDnsHostnames != nil && *dnsHostnames.EnableDnsHostnames.Value {
		s.pass("DNS hostnames enabled")
	} else {
		s.warn("DNS hostnames not enabled")
	}

	s.printSection("Subnets")
	subnets, err := s.ec2Client.DescribeSubnets(ctx, &ec2.DescribeSubnetsInput{
		Filters: []ec2types.Filter{
			{Name: strPtr("vpc-id"), Values: []string{*vpc.VpcId}},
		},
	})
	if err != nil {
		s.fail(fmt.Sprintf("Subnet lookup: %v", err))
		return
	}

	publicCount := 0
	privateCount := 0
	for _, subnet := range subnets.Subnets {
		for _, tag := range subnet.Tags {
			if *tag.Key == "aws-cdk:subnet-type" {
				if *tag.Value == "Public" {
					publicCount++
				} else if strings.Contains(*tag.Value, "Private") {
					privateCount++
				}
			}
		}
	}
	s.pass(fmt.Sprintf("Subnets: %d public, %d private", publicCount, privateCount))

	s.printSection("Internet Gateway")
	igws, err := s.ec2Client.DescribeInternetGateways(ctx, &ec2.DescribeInternetGatewaysInput{
		Filters: []ec2types.Filter{
			{Name: strPtr("attachment.vpc-id"), Values: []string{*vpc.VpcId}},
		},
	})
	if err == nil && len(igws.InternetGateways) > 0 {
		s.pass("Internet Gateway attached")
	} else {
		s.warn("No Internet Gateway found")
	}

	s.printSection("NAT Gateways")
	natGws, err := s.ec2Client.DescribeNatGateways(ctx, &ec2.DescribeNatGatewaysInput{
		Filter: []ec2types.Filter{
			{Name: strPtr("vpc-id"), Values: []string{*vpc.VpcId}},
			{Name: strPtr("state"), Values: []string{"available"}},
		},
	})
	if err == nil && len(natGws.NatGateways) > 0 {
		s.pass(fmt.Sprintf("NAT Gateways: %d available", len(natGws.NatGateways)))
	} else {
		s.warn("No NAT Gateways found")
	}
}

// -----------------------------------------------------------------------------
// SES Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testSES() {
	s.printHeader("SES EMAIL SERVICE")
	ctx := context.Background()

	s.printSection("Email Identity")
	identities, err := s.sesClient.ListEmailIdentities(ctx, &sesv2.ListEmailIdentitiesInput{})
	if err != nil {
		s.fail(fmt.Sprintf("List email identities: %v", err))
		return
	}

	found := false
	for _, identity := range identities.EmailIdentities {
		if *identity.IdentityName == s.domain {
			found = true
			// Get identity details
			details, err := s.sesClient.GetEmailIdentity(ctx, &sesv2.GetEmailIdentityInput{
				EmailIdentity: identity.IdentityName,
			})
			if err == nil {
				if details.VerifiedForSendingStatus {
					s.pass(fmt.Sprintf("Email identity '%s' verified for sending", s.domain))
				} else {
					s.warn(fmt.Sprintf("Email identity '%s' not verified for sending", s.domain))
				}

				if details.DkimAttributes != nil {
					switch details.DkimAttributes.Status {
					case "SUCCESS":
						s.pass("DKIM: verified")
					case "PENDING":
						s.warn("DKIM: pending verification")
					default:
						s.warn(fmt.Sprintf("DKIM status: %s", details.DkimAttributes.Status))
					}
				}
			}
			break
		}
	}

	if !found {
		s.fail(fmt.Sprintf("Email identity '%s' not found", s.domain))
	}

	s.printSection("Configuration Sets")
	configSetName := fmt.Sprintf("%s-webapp-configuration-set", s.deploymentID)
	configSet, err := s.sesClient.GetConfigurationSet(ctx, &sesv2.GetConfigurationSetInput{
		ConfigurationSetName: &configSetName,
	})
	if err != nil {
		s.fail(fmt.Sprintf("Configuration set '%s' not found", configSetName))
	} else {
		s.pass(fmt.Sprintf("Configuration set '%s' exists", configSetName))

		if configSet.ReputationOptions != nil && configSet.ReputationOptions.ReputationMetricsEnabled {
			s.pass("Reputation metrics enabled")
		} else {
			s.warn("Reputation metrics disabled")
		}

		if configSet.SendingOptions != nil && configSet.SendingOptions.SendingEnabled {
			s.pass("Sending enabled")
		} else {
			s.warn("Sending disabled")
		}
	}

	// Check event destinations
	eventDests, err := s.sesClient.GetConfigurationSetEventDestinations(ctx, &sesv2.GetConfigurationSetEventDestinationsInput{
		ConfigurationSetName: &configSetName,
	})
	if err == nil && len(eventDests.EventDestinations) > 0 {
		s.pass(fmt.Sprintf("Event destinations configured: %d", len(eventDests.EventDestinations)))
		for _, dest := range eventDests.EventDestinations {
			s.pass(fmt.Sprintf("  - %s (enabled: %t)", *dest.Name, dest.Enabled))
		}
	}

	s.printSection("S3 Email Storage")
	bucketName := fmt.Sprintf("%s-webapp-ses-received-emails", s.deploymentID)
	_, err = s.s3Client.HeadBucket(ctx, &s3.HeadBucketInput{
		Bucket: &bucketName,
	})
	if err != nil {
		s.warn(fmt.Sprintf("S3 bucket '%s' not accessible", bucketName))
	} else {
		s.pass(fmt.Sprintf("S3 bucket '%s' exists", bucketName))

		// Check lifecycle rules
		lifecycle, err := s.s3Client.GetBucketLifecycleConfiguration(ctx, &s3.GetBucketLifecycleConfigurationInput{
			Bucket: &bucketName,
		})
		if err == nil && len(lifecycle.Rules) > 0 {
			s.pass(fmt.Sprintf("S3 lifecycle rules: %d configured", len(lifecycle.Rules)))
		}
	}
}

// -----------------------------------------------------------------------------
// Cognito Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testCognito() {
	s.printHeader("COGNITO AUTHENTICATION")
	ctx := context.Background()

	s.printSection("User Pool")
	userPoolName := fmt.Sprintf("%s-webapp-userpool", s.deploymentID)

	maxResults := int32(60)
	pools, err := s.cognitoClient.ListUserPools(ctx, &cognitoidentityprovider.ListUserPoolsInput{
		MaxResults: &maxResults,
	})
	if err != nil {
		s.fail(fmt.Sprintf("List user pools: %v", err))
		return
	}

	var userPoolID *string
	for _, pool := range pools.UserPools {
		if *pool.Name == userPoolName {
			userPoolID = pool.Id
			s.pass(fmt.Sprintf("User Pool '%s' exists (ID: %s)", userPoolName, *pool.Id))
			break
		}
	}

	if userPoolID == nil {
		s.fail(fmt.Sprintf("User Pool '%s' not found", userPoolName))
		return
	}

	// Get detailed pool info
	poolDetails, err := s.cognitoClient.DescribeUserPool(ctx, &cognitoidentityprovider.DescribeUserPoolInput{
		UserPoolId: userPoolID,
	})
	if err != nil {
		s.fail(fmt.Sprintf("Describe user pool: %v", err))
	} else {
		pool := poolDetails.UserPool

		// Check MFA configuration
		if pool.MfaConfiguration != "" {
			s.pass(fmt.Sprintf("MFA configuration: %s", pool.MfaConfiguration))
		}

		// Check password policy
		if pool.Policies != nil && pool.Policies.PasswordPolicy != nil {
			pp := pool.Policies.PasswordPolicy
			s.pass(fmt.Sprintf("Password policy: min length %d, require numbers=%t, symbols=%t",
				pp.MinimumLength, pp.RequireNumbers, pp.RequireSymbols))
		}

		// Check email configuration
		if pool.EmailConfiguration != nil && pool.EmailConfiguration.EmailSendingAccount != "" {
			s.pass(fmt.Sprintf("Email sending: %s", pool.EmailConfiguration.EmailSendingAccount))
		}

		// Check Lambda triggers
		if pool.LambdaConfig != nil {
			triggerCount := 0
			if pool.LambdaConfig.PreSignUp != nil {
				triggerCount++
			}
			if pool.LambdaConfig.PostConfirmation != nil {
				triggerCount++
			}
			if pool.LambdaConfig.PreAuthentication != nil {
				triggerCount++
			}
			if pool.LambdaConfig.PostAuthentication != nil {
				triggerCount++
			}
			if pool.LambdaConfig.CustomMessage != nil {
				triggerCount++
			}
			if triggerCount > 0 {
				s.pass(fmt.Sprintf("Lambda triggers: %d configured", triggerCount))
			}
		}

		// Check user count
		s.pass(fmt.Sprintf("Estimated users: %d", pool.EstimatedNumberOfUsers))
	}

	s.printSection("User Pool Clients")
	clientMaxResults := int32(60)
	clients, err := s.cognitoClient.ListUserPoolClients(ctx, &cognitoidentityprovider.ListUserPoolClientsInput{
		UserPoolId: userPoolID,
		MaxResults: &clientMaxResults,
	})
	if err != nil {
		s.fail(fmt.Sprintf("List user pool clients: %v", err))
		return
	}

	if len(clients.UserPoolClients) == 0 {
		s.fail("No User Pool clients found")
	} else {
		for _, client := range clients.UserPoolClients {
			// Get client details
			clientDetails, err := s.cognitoClient.DescribeUserPoolClient(ctx, &cognitoidentityprovider.DescribeUserPoolClientInput{
				UserPoolId: userPoolID,
				ClientId:   client.ClientId,
			})
			if err == nil {
				c := clientDetails.UserPoolClient
				s.pass(fmt.Sprintf("Client '%s' (ID: %s)", *c.ClientName, *c.ClientId))

				// Check OAuth configuration
				if len(c.AllowedOAuthFlows) > 0 {
					s.pass(fmt.Sprintf("  OAuth flows: %v", c.AllowedOAuthFlows))
				}
				if len(c.AllowedOAuthScopes) > 0 {
					s.pass(fmt.Sprintf("  OAuth scopes: %v", c.AllowedOAuthScopes))
				}
				if len(c.ExplicitAuthFlows) > 0 {
					s.pass(fmt.Sprintf("  Auth flows: %v", c.ExplicitAuthFlows))
				}
			}
		}
	}
}

// -----------------------------------------------------------------------------
// DynamoDB Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testDynamoDB() {
	s.printHeader("DYNAMODB DATABASE")
	ctx := context.Background()

	s.printSection("User Table")
	tableName := fmt.Sprintf("%s-webapp-db-user", s.deploymentID)

	table, err := s.dynamoClient.DescribeTable(ctx, &dynamodb.DescribeTableInput{
		TableName: &tableName,
	})
	if err != nil {
		s.fail(fmt.Sprintf("Table '%s' not found", tableName))
		return
	}

	t := table.Table
	s.pass(fmt.Sprintf("Table '%s' exists", tableName))

	// Check table status
	if t.TableStatus == "ACTIVE" {
		s.pass("Table status: ACTIVE")
	} else {
		s.warn(fmt.Sprintf("Table status: %s", t.TableStatus))
	}

	// Check key schema
	for _, key := range t.KeySchema {
		s.pass(fmt.Sprintf("Key: %s (%s)", *key.AttributeName, key.KeyType))
	}

	// Check billing mode
	if t.BillingModeSummary != nil {
		s.pass(fmt.Sprintf("Billing mode: %s", t.BillingModeSummary.BillingMode))
	}

	// Check encryption
	if t.SSEDescription != nil {
		s.pass(fmt.Sprintf("Encryption: %s (%s)", t.SSEDescription.Status, t.SSEDescription.SSEType))
	}

	// Check deletion protection
	if t.DeletionProtectionEnabled != nil && *t.DeletionProtectionEnabled {
		s.pass("Deletion protection: enabled")
	} else {
		s.warn("Deletion protection: disabled")
	}

	// Check item count
	s.pass(fmt.Sprintf("Item count: %d", t.ItemCount))

	// Check contributor insights
	ci, err := s.dynamoClient.DescribeContributorInsights(ctx, &dynamodb.DescribeContributorInsightsInput{
		TableName: &tableName,
	})
	if err == nil {
		s.pass(fmt.Sprintf("Contributor insights: %s", ci.ContributorInsightsStatus))
	}
}

// -----------------------------------------------------------------------------
// API Gateway Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testAPIGateway() {
	s.printHeader("API GATEWAY")
	ctx := context.Background()

	s.printSection("REST API")
	apiName := fmt.Sprintf("%s-webapp-api", s.deploymentID)

	apis, err := s.apiGwClient.GetRestApis(ctx, &apigateway.GetRestApisInput{})
	if err != nil {
		s.fail(fmt.Sprintf("List REST APIs: %v", err))
		return
	}

	var apiID *string
	for _, api := range apis.Items {
		if *api.Name == apiName {
			apiID = api.Id
			s.pass(fmt.Sprintf("REST API '%s' exists (ID: %s)", apiName, *api.Id))
			break
		}
	}

	if apiID == nil {
		s.fail(fmt.Sprintf("REST API '%s' not found", apiName))
		return
	}

	s.printSection("API Stages")
	stages, err := s.apiGwClient.GetStages(ctx, &apigateway.GetStagesInput{
		RestApiId: apiID,
	})
	if err != nil {
		s.fail(fmt.Sprintf("Get stages: %v", err))
	} else {
		for _, stage := range stages.Item {
			s.pass(fmt.Sprintf("Stage '%s' deployed", *stage.StageName))

			if stage.TracingEnabled {
				s.pass("  X-Ray tracing enabled")
			}
			if stage.CacheClusterEnabled {
				s.pass(fmt.Sprintf("  Caching enabled (size: %s)", stage.CacheClusterSize))
			}
		}
	}

	s.printSection("API Resources")
	resources, err := s.apiGwClient.GetResources(ctx, &apigateway.GetResourcesInput{
		RestApiId: apiID,
	})
	if err != nil {
		s.fail(fmt.Sprintf("Get resources: %v", err))
	} else {
		methodCount := 0
		for _, resource := range resources.Items {
			for method := range resource.ResourceMethods {
				methodCount++
				s.pass(fmt.Sprintf("  %s %s", method, *resource.Path))
			}
		}
		if methodCount == 0 {
			s.warn("No API methods found")
		}
	}

	s.printSection("Authorizers")
	authorizers, err := s.apiGwClient.GetAuthorizers(ctx, &apigateway.GetAuthorizersInput{
		RestApiId: apiID,
	})
	if err != nil {
		s.warn(fmt.Sprintf("Get authorizers: %v", err))
	} else if len(authorizers.Items) > 0 {
		for _, auth := range authorizers.Items {
			s.pass(fmt.Sprintf("Authorizer '%s' (type: %s)", *auth.Name, auth.Type))
		}
	} else {
		s.warn("No authorizers configured")
	}
}

// -----------------------------------------------------------------------------
// Lambda Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testLambda() {
	s.printHeader("LAMBDA FUNCTIONS")
	ctx := context.Background()

	s.printSection("Functions")
	prefix := fmt.Sprintf("%s-webapp", s.deploymentID)

	paginator := lambda.NewListFunctionsPaginator(s.lambdaClient, &lambda.ListFunctionsInput{})
	functionCount := 0
	for paginator.HasMorePages() {
		page, err := paginator.NextPage(ctx)
		if err != nil {
			s.fail(fmt.Sprintf("List functions: %v", err))
			return
		}

		for _, fn := range page.Functions {
			if strings.HasPrefix(*fn.FunctionName, prefix) {
				functionCount++
				state := "Unknown"
				if fn.State != "" {
					state = string(fn.State)
				}
				s.pass(fmt.Sprintf("Lambda '%s' (%s, %dMB, %s)", *fn.FunctionName, fn.Runtime, *fn.MemorySize, state))

				// Check for VPC configuration
				if fn.VpcConfig != nil && len(fn.VpcConfig.SubnetIds) > 0 {
					s.pass(fmt.Sprintf("  VPC configured: %d subnets", len(fn.VpcConfig.SubnetIds)))
				}
			}
		}
	}

	if functionCount == 0 {
		s.warn(fmt.Sprintf("No Lambda functions found with prefix '%s'", prefix))
	}

	s.printSection("Layers")
	layers, err := s.lambdaClient.ListLayers(ctx, &lambda.ListLayersInput{})
	if err != nil {
		s.warn(fmt.Sprintf("List layers: %v", err))
	} else {
		found := false
		for _, layer := range layers.Layers {
			if strings.Contains(*layer.LayerName, "base-api") || strings.Contains(*layer.LayerName, prefix) {
				found = true
				s.pass(fmt.Sprintf("Layer '%s' (latest version: %d)", *layer.LayerName, layer.LatestMatchingVersion.Version))
			}
		}
		if !found {
			s.warn("No webapp-related layers found")
		}
	}
}

// -----------------------------------------------------------------------------
// SNS Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testSNS() {
	s.printHeader("SNS TOPICS")
	ctx := context.Background()

	s.printSection("SES Event Topics")
	expectedTopics := []string{
		fmt.Sprintf("%s-webapp-bounce", s.deploymentID),
		fmt.Sprintf("%s-webapp-complaint", s.deploymentID),
		fmt.Sprintf("%s-webapp-reject", s.deploymentID),
		fmt.Sprintf("%s-webapp-received-emails", s.deploymentID),
	}

	topics, err := s.snsClient.ListTopics(ctx, &sns.ListTopicsInput{})
	if err != nil {
		s.fail(fmt.Sprintf("List topics: %v", err))
		return
	}

	topicMap := make(map[string]bool)
	for _, topic := range topics.Topics {
		parts := strings.Split(*topic.TopicArn, ":")
		topicName := parts[len(parts)-1]
		topicMap[topicName] = true
	}

	for _, expected := range expectedTopics {
		if topicMap[expected] {
			s.pass(fmt.Sprintf("Topic '%s' exists", expected))
		} else {
			s.warn(fmt.Sprintf("Topic '%s' not found", expected))
		}
	}
}

// -----------------------------------------------------------------------------
// CloudWatch Tests
// -----------------------------------------------------------------------------

func (s *TestSuite) testCloudWatch() {
	s.printHeader("CLOUDWATCH LOGGING")
	ctx := context.Background()

	s.printSection("Log Groups")
	expectedLogGroups := []string{
		fmt.Sprintf("%s-webapp-apigw-logs", s.deploymentID),
	}

	for _, logGroupName := range expectedLogGroups {
		logGroups, err := s.cloudwatchClient.DescribeLogGroups(ctx, &cloudwatchlogs.DescribeLogGroupsInput{
			LogGroupNamePrefix: &logGroupName,
		})
		if err != nil {
			s.fail(fmt.Sprintf("Describe log group '%s': %v", logGroupName, err))
			continue
		}

		found := false
		for _, lg := range logGroups.LogGroups {
			if *lg.LogGroupName == logGroupName {
				found = true
				retention := "never expire"
				if lg.RetentionInDays != nil {
					retention = fmt.Sprintf("%d days", *lg.RetentionInDays)
				}
				s.pass(fmt.Sprintf("Log group '%s' (retention: %s)", logGroupName, retention))
				break
			}
		}
		if !found {
			s.warn(fmt.Sprintf("Log group '%s' not found", logGroupName))
		}
	}

	// Check for Lambda log groups
	s.printSection("Lambda Log Groups")
	prefix := fmt.Sprintf("/aws/lambda/%s-webapp", s.deploymentID)
	lambdaLogs, err := s.cloudwatchClient.DescribeLogGroups(ctx, &cloudwatchlogs.DescribeLogGroupsInput{
		LogGroupNamePrefix: &prefix,
	})
	if err != nil {
		s.warn(fmt.Sprintf("List Lambda log groups: %v", err))
	} else if len(lambdaLogs.LogGroups) > 0 {
		for _, lg := range lambdaLogs.LogGroups {
			s.pass(fmt.Sprintf("Lambda logs: %s", *lg.LogGroupName))
		}
	} else {
		s.warn("No Lambda log groups found")
	}
}

// -----------------------------------------------------------------------------
// Summary
// -----------------------------------------------------------------------------

func (s *TestSuite) PrintSummary() {
	s.printHeader("TEST SUMMARY")

	total := s.passed + s.failed + s.warnings
	fmt.Printf("\n  %s✓ Passed:%s   %d\n", colorGreen, colorReset, s.passed)
	fmt.Printf("  %s✗ Failed:%s   %d\n", colorRed, colorReset, s.failed)
	fmt.Printf("  %s⚠ Warnings:%s %d\n", colorYellow, colorReset, s.warnings)
	fmt.Printf("  ─────────────────\n")
	fmt.Printf("  Total:     %d\n", total)

	if s.failed == 0 {
		fmt.Printf("\n%s✓ All critical checks passed!%s\n\n", colorGreen, colorReset)
	} else {
		fmt.Printf("\n%s✗ Some checks failed. Review output above.%s\n\n", colorRed, colorReset)
	}
}

// -----------------------------------------------------------------------------
// Helper Functions
// -----------------------------------------------------------------------------

func strPtr(s string) *string {
	return &s
}
