package ui.webapp.response;

import ui.webapp.model.CognitoMessageEvent;
import ui.webapp.model.MessageResponse;

public class VerifyUserAttributeResponse {
  private final CognitoMessageEvent event;

  public VerifyUserAttributeResponse(CognitoMessageEvent event) {
    var response = new MessageResponse(
      "webapp verification",
      "Please use verification code '{####}' to validate your email.",
      "Please use verification code '{####}' to validate your phone number.");

    this.event = CognitoMessageEvent.from(event, response);
  }

  public CognitoMessageEvent get() {
    return event;
  }
}
