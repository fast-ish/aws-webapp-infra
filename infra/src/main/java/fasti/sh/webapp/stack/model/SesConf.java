package fasti.sh.webapp.stack.model;

import fasti.sh.model.aws.ses.Destination;
import fasti.sh.model.aws.ses.IdentityConf;
import fasti.sh.model.aws.ses.Receiving;

public record SesConf(
  IdentityConf identity,
  Receiving receiving,
  Destination destination
) {}
