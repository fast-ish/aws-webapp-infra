package ui.webapp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import ui.webapp.model.CognitoMessageEvent;
import ui.webapp.response.ForgotPasswordResponse;
import ui.webapp.response.ResendCodeResponse;
import ui.webapp.response.SignUpResponse;
import ui.webapp.response.UpdateUserAttributeResponse;
import ui.webapp.response.VerifyUserAttributeResponse;

public class Handler implements RequestHandler<CognitoMessageEvent, CognitoMessageEvent> {
  @Override
  public CognitoMessageEvent handleRequest(CognitoMessageEvent event, Context context) {
    if (event.triggerSource().equals(TriggerSource.CustomMessage_SignUp.name())) {
      context.getLogger().log("custom message signup");
      return new SignUpResponse(event).get();
    }

    if (event.triggerSource().equals(TriggerSource.CustomMessage_ResendCode.name())) {
      context.getLogger().log("custom message resend code");
      return new ResendCodeResponse(event).get();
    }

    if (event.triggerSource().equals(TriggerSource.CustomMessage_ForgotPassword.name())) {
      context.getLogger().log("custom message forgot password");
      return new ForgotPasswordResponse(event).get();
    }

    if (event.triggerSource().equals(TriggerSource.CustomMessage_UpdateUserAttribute.name())) {
      context.getLogger().log("custom message update attribute");
      return new UpdateUserAttributeResponse(event).get();
    }

    if (event.triggerSource().equals(TriggerSource.CustomMessage_VerifyUserAttribute.name())) {
      context.getLogger().log("custom message verify attribute");
      return new VerifyUserAttributeResponse(event).get();
    }

    return event;
  }
}
