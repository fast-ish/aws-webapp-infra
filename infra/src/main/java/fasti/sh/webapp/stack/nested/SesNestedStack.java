package fasti.sh.webapp.stack.nested;

import fasti.sh.execute.aws.s3.BucketConstruct;
import fasti.sh.execute.aws.ses.IdentityConstruct;
import fasti.sh.webapp.stack.model.SesConf;
import fasti.sh.model.main.Common;
import fasti.sh.model.aws.ses.Rule;
import lombok.Getter;
import lombok.SneakyThrows;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.NestedStack;
import software.amazon.awscdk.NestedStackProps;
import software.amazon.awscdk.services.ses.ConfigurationSetEventDestination;
import software.amazon.awscdk.services.ses.ConfigurationSetEventDestinationOptions;
import software.amazon.awscdk.services.ses.EmailSendingEvent;
import software.amazon.awscdk.services.ses.EventDestination;
import software.amazon.awscdk.services.ses.IReceiptRuleAction;
import software.amazon.awscdk.services.ses.ReceiptRuleOptions;
import software.amazon.awscdk.services.ses.ReceiptRuleSet;
import software.amazon.awscdk.services.ses.actions.Lambda;
import software.amazon.awscdk.services.ses.actions.LambdaInvocationType;
import software.amazon.awscdk.services.ses.actions.S3;
import software.amazon.awscdk.services.ses.actions.Sns;
import software.amazon.awscdk.services.sns.Topic;
import software.constructs.Construct;

import java.util.List;
import java.util.stream.Stream;

import static fasti.sh.execute.serialization.Format.*;

@Getter
public class SesNestedStack extends NestedStack {
  private final IdentityConstruct identity;
  private final ReceiptRuleSet receiptRuleSet;
  private final ConfigurationSetEventDestination bounceDestination;
  private final ConfigurationSetEventDestination rejectDestination;
  private final ConfigurationSetEventDestination complaintDestination;

  @SneakyThrows
  public SesNestedStack(Construct scope, Common common, SesConf conf, NestedStackProps props) {
    super(scope, "webapp.ses", props);

    this.identity = new IdentityConstruct(this, common, conf.identity());
    this.receiptRuleSet = receiptRuleSet(common, conf);
    this.bounceDestination = bounceDestination(common, conf);
    this.rejectDestination = rejectDestination(common, conf);
    this.complaintDestination = complaintDestination(common, conf);

    CfnOutput.Builder
      .create(this, id(common.id(), "ses.identity.arn"))
      .exportName(exported(scope, "webappsesidentityarn"))
      .value(this.identity().hostedZoneIdentity().getEmailIdentityArn())
      .description(describe(common, "ses identity identity arn reference"))
      .build();

    CfnOutput.Builder
      .create(this, id(common.id(), "ses.identity.receiving.ruleset.name"))
      .exportName(exported(scope, "webappsesidentityreceivingruleset"))
      .value(this.receiptRuleSet().getReceiptRuleSetName())
      .description(describe(common, "ses receiving ruleset"))
      .build();
  }

  private ReceiptRuleSet receiptRuleSet(Common common, SesConf conf) {
    var rules = conf.receiving().rules().stream()
      .map(rule -> ReceiptRuleOptions.builder()
        .receiptRuleName(rule.name())
        .enabled(rule.enabled())
        .scanEnabled(rule.scanEnabled())
        .recipients(rule.recipients())
        .actions(actions(common, conf, rule))
        .build())
      .toList();

    return ReceiptRuleSet.Builder.create(this, "receipt.rules")
      .receiptRuleSetName(conf.receiving().name())
      .dropSpam(conf.receiving().dropSpam())
      .rules(rules)
      .build();
  }

  private List<? extends IReceiptRuleAction> actions(Common common, SesConf conf, Rule rule) {
    var bucket = new BucketConstruct(this, common, conf.receiving().bucket()).bucket();

    var s3Actions = rule.s3Actions()
      .stream()
      .map(action -> S3.Builder.create()
        .bucket(bucket)
        .objectKeyPrefix(action.prefix())
        .topic(new Topic(this, action.topic()))
        .build())
      .toList();

    var lambdaActions = rule.lambdaActions()
      .stream()
      .map(action -> Lambda.Builder.create()
        .topic(new Topic(this, action.topic()))
        .function(null)
        .invocationType(LambdaInvocationType.valueOf(action.invocationType()))
        .build())
      .toList();

    var snsActions = rule.snsActions()
      .stream()
      .map(action -> Sns.Builder.create()
        .topic(new Topic(this, action.topic()))
        .build())
      .toList();

    return Stream.of(s3Actions, lambdaActions, snsActions)
      .flatMap(List::stream)
      .toList();
  }

  private ConfigurationSetEventDestination bounceDestination(Common common, SesConf conf) {
    return this.identity().configurationSet().addEventDestination(
      conf.destination().bounce().topic(),
      ConfigurationSetEventDestinationOptions.builder()
        .enabled(conf.destination().bounce().enabled())
        .destination(EventDestination.snsTopic(new Topic(this, conf.destination().bounce().topic())))
        .configurationSetEventDestinationName(conf.destination().bounce().topic())
        .events(List.of(EmailSendingEvent.BOUNCE))
        .build());
  }

  private ConfigurationSetEventDestination rejectDestination(Common common, SesConf conf) {
    return this.identity().configurationSet().addEventDestination(
      conf.destination().reject().topic(),
      ConfigurationSetEventDestinationOptions.builder()
        .enabled(conf.destination().reject().enabled())
        .destination(EventDestination.snsTopic(new Topic(this, conf.destination().reject().topic())))
        .configurationSetEventDestinationName(conf.destination().reject().topic())
        .events(List.of(EmailSendingEvent.REJECT))
        .build());
  }

  private ConfigurationSetEventDestination complaintDestination(Common common, SesConf conf) {
    return this.identity().configurationSet().addEventDestination(
      conf.destination().complaint().topic(),
      ConfigurationSetEventDestinationOptions.builder()
        .enabled(conf.destination().complaint().enabled())
        .destination(EventDestination.snsTopic(new Topic(this, conf.destination().complaint().topic())))
        .configurationSetEventDestinationName(conf.destination().complaint().topic())
        .events(List.of(EmailSendingEvent.COMPLAINT))
        .build());
  }
}
