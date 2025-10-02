package ui.webapp.model;

import java.util.Map;

public record MessageRequest(
  String codeParameter,
  String usernameParameter,
  Map<String, String> userAttributes,
  Map<String, String> clientMetadata
) {}
