package fasti.sh.webapp.stack;

import static fasti.sh.execute.serialization.Format.describe;
import static fasti.sh.execute.serialization.Format.id;

import fasti.sh.execute.aws.vpc.NetworkNestedStack;
import fasti.sh.webapp.stack.nested.ApiNestedStack;
import fasti.sh.webapp.stack.nested.AuthNestedStack;
import fasti.sh.webapp.stack.nested.DbNestedStack;
import fasti.sh.webapp.stack.nested.SesNestedStack;
import lombok.Getter;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

@Getter
public class WebappStack extends Stack {
  private final NetworkNestedStack network;
  private final SesNestedStack ses;
  private final AuthNestedStack auth;
  private final DbNestedStack db;
  private final ApiNestedStack api;

  public WebappStack(Construct scope, WebappReleaseConf conf, StackProps props) {
    super(scope, id("webapp", conf.common().version()), props);

    this.network = new NetworkNestedStack(
      this, conf.common(), conf.vpc(),
      NestedStackProps
        .builder()
        .description(describe(conf.common(), "webapp::network"))
        .build());

    this.ses = new SesNestedStack(
      this, conf.common(), conf.ses(),
      NestedStackProps
        .builder()
        .description(describe(conf.common(), "webapp::ses"))
        .build());

    this.auth = new AuthNestedStack(
      this, conf.common(), conf.auth(), this.network().vpc(),
      NestedStackProps
        .builder()
        .description(describe(conf.common(), "webapp::auth"))
        .build());

    this.auth().addDependency(this.ses());

    this.db = new DbNestedStack(
      this, conf.common(), conf.db(),
      NestedStackProps
        .builder()
        .description(describe(conf.common(), "webapp::db"))
        .build());

    this.api = new ApiNestedStack(
      this, conf.common(), conf.api(), this.network().vpc(),
      NestedStackProps
        .builder()
        .description(describe(conf.common(), "webapp::api"))
        .build());

    this.api().addDependency(this.auth());
  }
}
