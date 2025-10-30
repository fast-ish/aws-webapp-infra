package fasti.sh.webapp.stack.nested;

import static fasti.sh.execute.serialization.Format.describe;
import static fasti.sh.execute.serialization.Format.exported;
import static fasti.sh.execute.serialization.Format.id;

import fasti.sh.execute.aws.apigw.LambdaIntegrationConstruct;
import fasti.sh.execute.aws.apigw.RestApiConstruct;
import fasti.sh.model.main.Common;
import fasti.sh.webapp.stack.model.ApiConf;
import java.util.List;
import lombok.Getter;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.services.apigateway.Authorizer;
import software.amazon.awscdk.services.apigateway.CognitoUserPoolsAuthorizer;
import software.amazon.awscdk.services.apigateway.IResource;
import software.amazon.awscdk.services.apigateway.JsonSchema;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.ILogGroup;
import software.constructs.Construct;

@Getter
public class ApiNestedStack extends NestedStack {
  private final RestApi api;
  private final List<IResource> integrations;
  private final ILogGroup logGroup;
  private final LayerVersion baseLayer;

  public ApiNestedStack(Construct scope, Common common, ApiConf conf, Vpc vpc, NestedStackProps props) {
    super(scope, "webapp.api", props);

    var userPoolArn = Fn.importValue(exported(scope, "webappuserpoolarn"));
    var userPool = UserPool.fromUserPoolArn(this, "userpool.lookup", userPoolArn);

    var authorizer = CognitoUserPoolsAuthorizer.Builder
      .create(this, "cognito.authorizer")
      .authorizerName(conf.authorizer().name())
      .cognitoUserPools(List.of(userPool))
      .build();

    var stack = new RestApiConstruct(this, common, conf.apigw(), authorizer, s -> JsonSchema.builder().build());

    this.baseLayer = LayerVersion.Builder
      .create(this, id("layer", conf.apigw().baseLayer().name()))
      .layerVersionName(conf.apigw().baseLayer().name())
      .code(Code.fromAsset(conf.apigw().baseLayer().asset()))
      .removalPolicy(conf.apigw().baseLayer().removalPolicy())
      .compatibleArchitectures(List.of(Architecture.X86_64))
      .compatibleRuntimes(
        conf
          .apigw()
          .baseLayer()
          .runtimes()
          .stream()
          .map(r -> Runtime.Builder.create(r).build())
          .toList())
      .build();

    this.api = stack.api();
    this.logGroup = stack.logGroup();
    this.integrations = integrate(common, conf, vpc, authorizer, stack, baseLayer);

    CfnOutput.Builder
      .create(this, id(common.id(), "apigw.id"))
      .exportName(exported(scope, "webappapigwid"))
      .value(this.api().getRestApiId())
      .description(describe(common, "api gateway id"))
      .build();
  }

  private List<IResource> integrate(Common common, ApiConf conf, Vpc vpc, Authorizer authorizer, RestApiConstruct stack,
    LayerVersion baseLayer) {
    return List
      .of(
        new LambdaIntegrationConstruct(this, common, authorizer, conf.resource(), vpc, stack, this.api().getRoot(),
          stack.requestModels(), baseLayer).resource());
  }
}
