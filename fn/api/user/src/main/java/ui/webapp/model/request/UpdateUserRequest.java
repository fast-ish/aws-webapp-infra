package ui.webapp.model.request;

import ui.webapp.model.Settings;

public record UpdateUserRequest(
  String phone,
  String username,
  Settings settings
) {}
