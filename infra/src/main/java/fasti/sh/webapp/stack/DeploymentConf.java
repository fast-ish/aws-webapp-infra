package fasti.sh.webapp.stack;

import fasti.sh.model.aws.vpc.NetworkConf;
import fasti.sh.model.main.Common;
import fasti.sh.webapp.stack.model.ApiConf;
import fasti.sh.webapp.stack.model.AuthConf;
import fasti.sh.webapp.stack.model.DbConf;
import fasti.sh.webapp.stack.model.SesConf;

public record DeploymentConf(
  Common common,
  NetworkConf vpc,
  SesConf ses,
  DbConf db,
  AuthConf auth,
  ApiConf api
) {}
