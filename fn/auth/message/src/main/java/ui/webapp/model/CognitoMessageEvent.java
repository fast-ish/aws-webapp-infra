package ui.webapp.model;

public record CognitoMessageEvent(
  String version,
  String triggerSource,
  String region,
  String userPoolId,
  String userName,
  CallerContext callerContext,
  MessageRequest request,
  MessageResponse response
) {

  public static CognitoMessageEvent from(CognitoMessageEvent event, MessageResponse response) {
    return new CognitoMessageEvent(
      event.version(),
      event.triggerSource(),
      event.region(),
      event.userPoolId(),
      event.userName(),
      event.callerContext(),
      event.request(),
      response
    );
  }
}
