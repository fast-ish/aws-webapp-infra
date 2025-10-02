package ui.webapp.model;

public record MessageResponse(
  String emailSubject,
  String emailMessage,
  String smsMessage
) {}
