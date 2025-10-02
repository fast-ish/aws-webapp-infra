package ui.webapp.execute;

import com.fasterxml.jackson.databind.ObjectMapper;
import ui.webapp.Initialize;
import ui.webapp.model.CognitoEvent;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class Orchestrate {

  public static CognitoEvent onboard(
    CognitoEvent event,
    ObjectMapper mapper,
    DynamoDbAsyncClient dynamoDbClient,
    CognitoIdentityProviderAsyncClient cognitoIdentityProviderClient) {

    return new Initialize(mapper, dynamoDbClient, cognitoIdentityProviderClient).user(event);
  }
}
