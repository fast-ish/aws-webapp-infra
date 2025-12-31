package ui.webapp.response;

import ui.webapp.model.CognitoMessageEvent;
import ui.webapp.model.MessageResponse;

public class ForgotPasswordResponse {
  private final CognitoMessageEvent event;
  private final String smsMessage = "Please use verification code '{####}' to validate your phone number.";
  private final String emailSubject = "webapp verification";
  private final String emailMessage;

  {
    emailMessage = """
      <body style="user-select: none; background-color: #f9f9f9; max-width: 64rem; margin-left: auto; margin-right: auto; font-family: 'Source Sans Pro',monospace;">
      <h1 style="color: #ffcce0;">webapp</h1>
      <div>
          <p>
              Whoops. Here ya go.
          </p>
      </div>
      <div>
          <p>
              You can use this verification code <code style="user-select: text; font-weight: 800; font-size: 1.5rem; line-height: 2rem;">{####}</code> to reset your password.
          </p>
      </div>
      </body>
      """;
  }

  public ForgotPasswordResponse(CognitoMessageEvent event) {
    var response = new MessageResponse(emailSubject, emailMessage, smsMessage);
    this.event = CognitoMessageEvent.from(event, response);
  }

  public CognitoMessageEvent get() {
    return event;
  }
}
