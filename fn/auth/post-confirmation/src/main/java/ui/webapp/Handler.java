package ui.webapp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import ui.webapp.execute.Orchestrate;
import ui.webapp.model.CognitoEvent;

public class Handler implements RequestHandler<CognitoEvent, CognitoEvent> {
  private final Logger log = Logging.handler();
  private final ObjectMapper mapper;
  private final CognitoIdentityProviderAsyncClient cognitoIdentityClient;
  private final DynamoDbAsyncClient dynamoDbClient;

  public Handler() {
    mapper = SharedDependencyFactory.objectMapper();
    cognitoIdentityClient = AuthDependencyFactory.cognitoIdentityProviderClient();
    dynamoDbClient = SharedDependencyFactory.dynamoDbAsyncClient();
  }

  public CognitoEvent handleRequest(CognitoEvent event, Context context) {
    log.info("post-confirmation cognito event {}", event);
    return Orchestrate.onboard(event, mapper, dynamoDbClient, cognitoIdentityClient);
  }
}
