package fasti.sh.webapp.stack.nested;

import fasti.sh.execute.aws.dynamodb.DynamoDbConstruct;
import fasti.sh.webapp.stack.model.DbConf;
import fasti.sh.model.main.Common;
import lombok.Getter;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.constructs.Construct;

import static fasti.sh.execute.serialization.Format.*;


@Getter
public class DbNestedStack extends NestedStack {
  private final DynamoDbConstruct dynamoDbConstruct;

  public DbNestedStack(Construct scope, Common common, DbConf conf, NestedStackProps props) {
    super(scope, "webapp.db", props);

    this.dynamoDbConstruct = new DynamoDbConstruct(this, common, conf.user());

    CfnOutput.Builder
      .create(this, id(common.id(), "user.table.arn"))
      .exportName(exported(scope, "webappusertablearn"))
      .value(this.dynamoDbConstruct().table().getTableArn())
      .description(describe(common))
      .build();

    CfnOutput.Builder
      .create(this, id(common.id(), "user.table.id"))
      .exportName(exported(scope, "webappusertableid"))
      .value(this.dynamoDbConstruct().table().getTableId())
      .description(describe(common))
      .build();
  }
}
