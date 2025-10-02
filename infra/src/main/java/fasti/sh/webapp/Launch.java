package fasti.sh.webapp;

import static fasti.sh.execute.serialization.Format.describe;
import static fasti.sh.execute.serialization.Format.name;

import com.fasterxml.jackson.core.type.TypeReference;
import fasti.sh.execute.serialization.Mapper;
import fasti.sh.execute.serialization.Template;
import fasti.sh.model.aws.cdk.Synthesizer;
import fasti.sh.model.main.Common;
import fasti.sh.model.main.Hosted;
import fasti.sh.model.main.common.Bare;
import fasti.sh.webapp.stack.DeploymentConf;
import fasti.sh.webapp.stack.DeploymentStack;
import java.util.Map;
import lombok.SneakyThrows;
import software.amazon.awscdk.App;
import software.amazon.awscdk.DefaultStackSynthesizer;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StackSynthesizer;

public class Launch {

  @SneakyThrows
  public static void main(String[] args) {
    var app = new App();

    var conf = get(app);

    new DeploymentStack(
        app, conf.hosted(),
        StackProps.builder()
            .stackName(name(conf.hosted().common().id(), "webapp"))
            .env(Environment.builder()
                .account(conf.hosted().common().account())
                .region(conf.hosted().common().region())
                .build())
            .description(describe(conf.host().common(),
                String.format("%s %s webapp",
                    conf.hosted().common().name(), conf.hosted().common().alias())))
            .synthesizer(synthesizer(app))
            .tags(Common.Maps.from(conf.host().common().tags(), conf.hosted().common().tags()))
            .build());

    app.synth();
  }

  @SneakyThrows
  private static Hosted<Bare, DeploymentConf> get(App app) {
    var parsed = Template.parse(
        app, "conf.mustache",
        Map.ofEntries(
            Map.entry("hosted:ses:hosted:zone", app.getNode().getContext("hosted:ses:hosted:zone")),
            Map.entry("hosted:ses:email", app.getNode().getContext("hosted:ses:email"))));

    var type = new TypeReference<Hosted<Bare, DeploymentConf>>() {};
    return Mapper.get().readValue(parsed, type);
  }

  @SneakyThrows
  private static StackSynthesizer synthesizer(App app) {
    var customSynthesizer = Boolean.parseBoolean(app.getNode().getContext("synthesizer:custom").toString());

    if (customSynthesizer) {
      var key = app.getNode().getContext("hosted:api:key");
      var synthesizerName = app.getNode().getContext("hosted:synthesizer:name");
      var synthesizerAlias = app.getNode().getContext("hosted:synthesizer:alias");

      var parsed = Template.parse(app, "synthesizer.mustache",
          Map.ofEntries(
              Map.entry("synthesizer:role:exec", app.getNode().getContext("hosted:cdk:role:exec").toString()),
              Map.entry("synthesizer:role:deploy", app.getNode().getContext("hosted:cdk:role:deploy").toString()),
              Map.entry("synthesizer:role:lookup", app.getNode().getContext("hosted:cdk:role:lookup").toString()),
              Map.entry("synthesizer:role:assets", app.getNode().getContext("hosted:cdk:role:assets").toString()),
              Map.entry("synthesizer:role:images", app.getNode().getContext("hosted:cdk:role:images").toString()),
              Map.entry("synthesizer:ssm", app.getNode().getContext("hosted:cdk:keys:ssm").toString()),
              Map.entry("synthesizer:storage:assets", app.getNode().getContext("hosted:cdk:storage:assets").toString()),
              Map.entry("synthesizer:storage:images", app.getNode().getContext("hosted:cdk:storage:images").toString()),
              Map.entry("synthesizer:qualifier", String.format("%s-%s", synthesizerName, synthesizerAlias)),
              Map.entry("synthesizer:assets:prefix", String.format("%s-%s", synthesizerName, synthesizerAlias)),
              Map.entry("synthesizer:images:tag", String.format("%s-%s", synthesizerName, synthesizerAlias)),
              Map.entry("synthesizer:externalid", key)));

      var synthesizer = Mapper.get().readValue(parsed, Synthesizer.class);
      return DefaultStackSynthesizer.Builder.create()
          .qualifier(synthesizer.qualifier())
          .cloudFormationExecutionRole(synthesizer.cloudFormationExecutionRole())
          .deployRoleArn(synthesizer.deployRoleArn())
          .lookupRoleArn(synthesizer.lookupRoleArn())
          .fileAssetPublishingRoleArn(synthesizer.fileAssetPublishingRoleArn())
          .imageAssetPublishingRoleArn(synthesizer.imageAssetPublishingRoleArn())
          .bootstrapStackVersionSsmParameter(synthesizer.bootstrapStackVersionSsmParameter())
          .fileAssetsBucketName(arnToBucketName(synthesizer.fileAssetsBucketName()))
          .imageAssetsRepositoryName(synthesizer.imageAssetsRepositoryName())
          .generateBootstrapVersionRule(synthesizer.generateBootstrapVersionRule())
          .useLookupRoleForStackOperations(synthesizer.useLookupRoleForStackOperations())
          .bucketPrefix(synthesizer.bucketPrefix())
          .dockerTagPrefix(synthesizer.dockerTagPrefix())
          .lookupRoleExternalId(synthesizer.lookupRoleExternalId())
          .fileAssetPublishingExternalId(synthesizer.fileAssetPublishingExternalId())
          .imageAssetPublishingExternalId(synthesizer.imageAssetPublishingExternalId())
          .deployRoleExternalId(synthesizer.deployRoleExternalId())
          .build();
    } else {
      return DefaultStackSynthesizer.Builder.create().build();
    }
  }

  public static String arnToBucketName(String arn) {
    if (arn != null && arn.startsWith("arn:aws:s3::")) {
      var parts = arn.split(":");
      var resourcePart = parts[parts.length - 1];
      if (resourcePart.startsWith("/")) {
        resourcePart = resourcePart.substring(1);
      }
      var resourceParts = resourcePart.split("/");
      return resourceParts[0];
    }
    return null;
  }
}
