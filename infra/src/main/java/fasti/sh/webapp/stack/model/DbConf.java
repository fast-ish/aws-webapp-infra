package fasti.sh.webapp.stack.model;

import fasti.sh.model.aws.dynamodb.Table;
import fasti.sh.model.aws.fn.Lambda;

public record DbConf(
  String vpcName,
  Table user,
  Lambda listener
) {}
