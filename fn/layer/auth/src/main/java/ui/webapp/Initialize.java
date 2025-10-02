package ui.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import ui.webapp.execute.GroupUser;
import ui.webapp.execute.RememberUser;
import ui.webapp.model.CognitoEvent;

public class Initialize {
  private final Logger log = Logging.handler();
  private final ObjectMapper mapper;
  private final DynamoDbAsyncClient dynamoDbClient;
  private final CognitoIdentityProviderAsyncClient cognitoIdentityProviderClient;

  public Initialize(
    ObjectMapper mapper,
    DynamoDbAsyncClient dynamoDbClient,
    CognitoIdentityProviderAsyncClient cognitoIdentityProviderClient) {

    this.mapper = mapper;
    this.dynamoDbClient = dynamoDbClient;
    this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
  }

  public CognitoEvent user(CognitoEvent event) {
    var initialize = RememberUser.with(event, mapper, dynamoDbClient)
      .exceptionally(e -> {
        log.error("error remembering user {} {}", event, e.getMessage());
        return null;
      })
      .thenComposeAsync(result -> GroupUser.with(event, cognitoIdentityProviderClient))
      .exceptionally(e -> {
        log.error("error grouping user {} {}", event, e.getMessage());
        return null;
      });

    initialize.join();

    return event;
  }
}
