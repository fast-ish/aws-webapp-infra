package fasti.sh.webapp.stack.model;

public record AuthConf(
  String vpcName,
  String userPool,
  String userPoolClient
) {}
