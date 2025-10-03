package ui.webapp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import ui.webapp.execute.Orchestrate;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private final Logger log;
  private final ObjectMapper mapper;
  private final DynamoDbAsyncClient dynamoDbClient;
  private final CognitoIdentityProviderAsyncClient cognitoClient;

  public Handler() {
    log = Logging.handler();
    mapper = DependencyFactory.objectMapper();
    dynamoDbClient = DependencyFactory.dynamoDbClient();
    cognitoClient = DependencyFactory.cognitoIdentityClient();
  }

  @SneakyThrows
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    log.info("received user request {} {}", request, context);
    return new Orchestrate(mapper, dynamoDbClient, cognitoClient)
      .response(request)
      .join();
  }
}
