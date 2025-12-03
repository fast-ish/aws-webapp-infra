package fasti.sh.webapp;

import static fasti.sh.execute.serialization.Format.describe;
import static fasti.sh.execute.serialization.Format.name;

import com.fasterxml.jackson.core.type.TypeReference;
import fasti.sh.execute.serialization.Mapper;
import fasti.sh.execute.serialization.Template;
import fasti.sh.model.main.Common;
import fasti.sh.model.main.Release;
import fasti.sh.webapp.stack.WebappReleaseConf;
import fasti.sh.webapp.stack.WebappStack;
import java.util.Map;
import lombok.SneakyThrows;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Launch {

  @SneakyThrows
  public static void main(String[] args) {
    var app = new App();

    var conf = get(app);

    new WebappStack(
      app, conf.release(),
      StackProps
        .builder()
        .stackName(name(conf.release().common().id(), "webapp"))
        .env(
          Environment
            .builder()
            .account(conf.release().common().account())
            .region(conf.release().common().region())
            .build())
        .description(
          describe(
            conf.platform(),
            String
              .format(
                "%s %s webapp",
                conf.release().common().name(),
                conf.release().common().alias())))
        .tags(Common.Maps.from(conf.platform().tags(), conf.release().common().tags()))
        .build());

    app.synth();
  }

  @SneakyThrows
  private static Release<WebappReleaseConf> get(App app) {
    var parsed = Template
      .parse(
        app,
        "conf.mustache",
        Map.ofEntries(
            Map.entry("deployment:ses:hosted:zone", app.getNode().getContext("deployment:ses:hosted:zone")),
            Map.entry("deployment:ses:email", app.getNode().getContext("deployment:ses:email"))));

    var type = new TypeReference<Release<WebappReleaseConf>>() {};
    return Mapper.get().readValue(parsed, type);
  }
}
