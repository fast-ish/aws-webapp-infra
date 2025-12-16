package fasti.sh.webapp;

import static fasti.sh.execute.serialization.Format.describe;
import static fasti.sh.execute.serialization.Format.name;

import com.fasterxml.jackson.core.type.TypeReference;
import fasti.sh.execute.util.TemplateUtils;
import fasti.sh.model.main.Common;
import fasti.sh.model.main.Release;
import fasti.sh.webapp.stack.WebappReleaseConf;
import fasti.sh.webapp.stack.WebappStack;
import java.util.Map;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Launch {

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
                "WebApp release [%s/%s] - Lambda and API Gateway",
                conf.release().common().name(),
                conf.release().common().alias())))
        .tags(Common.Maps.from(conf.platform().tags(), conf.release().common().tags()))
        .build());

    app.synth();
  }

  private static Release<WebappReleaseConf> get(App app) {
    var mappings = Map.<String, Object>ofEntries(
        Map.entry("deployment:ses:hosted:zone", app.getNode().getContext("deployment:ses:hosted:zone")),
        Map.entry("deployment:ses:email", app.getNode().getContext("deployment:ses:email")));
    var type = new TypeReference<Release<WebappReleaseConf>>() {};
    return TemplateUtils.parseAs(app, "conf.mustache", mappings, type);
  }
}
