package fasti.sh.webapp.stack.model;

import fasti.sh.model.aws.cognito.client.Authorizer;

public record ApiConf(
  fasti.sh.model.aws.apigw.ApiConf apigw,
  String resource,
  Authorizer authorizer
) {}
