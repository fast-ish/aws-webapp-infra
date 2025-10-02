package ui.webapp.model;

import java.util.Map;

public record CognitoEvent(
  int version,
  String region,
  String userPoolId,
  String triggerSource,
  String userName,
  CognitoContext callerContext,
  CognitoRequest request,
  Map<String, Object> response
) {

  public static CognitoEvent from(CognitoEvent event, Map<String, Object> response) {
    return new CognitoEvent(
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