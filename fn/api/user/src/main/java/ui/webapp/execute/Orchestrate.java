package ui.webapp.execute;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import ui.webapp.Logging;
import ui.webapp.model.request.UpdateUserRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ui.webapp.JsonUtil.fromJson;
import static ui.webapp.JsonUtil.toJson;

public class Orchestrate {
  private final Logger log = Logging.handler();
  private final ObjectMapper mapper;
  private final UserClient userClient;

  final static String READ_USER = "/user/{user}";
  final static String UPDATE_USER = "/user/{user}";
  final static String DELETE_USER = "/user/{user}/unsubscribe";

  public Orchestrate(ObjectMapper mapper, DynamoDbAsyncClient dynamoDbClient, CognitoIdentityProviderAsyncClient cognitoClient) {
    this.mapper = mapper;
    this.userClient = new UserClient(dynamoDbClient, cognitoClient);
  }

  public CompletableFuture<APIGatewayProxyResponseEvent> response(APIGatewayProxyRequestEvent request) {
    var parameters = request.getPathParameters();
    var userId = parameters.get("user");

    var headers = Map.of("Content-Type", "application/json");

    if (request.getHttpMethod().equals("GET") && request.getResource().equals(READ_USER)) {
      log.info("get user {}", request);

      return userClient.get(userId)
        .thenApply(response -> {
          var body = toJson(response, mapper);
          log.info("get user ok {} {}", READ_USER, body);
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(headers)
            .withBody(body);
        })
        .exceptionally(e -> {
          log.error("get user not ok {} {}", e.getMessage(), READ_USER);
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(headers);
        });
    } else if (request.getHttpMethod().equals("PUT") && request.getResource().equals(UPDATE_USER)) {
      log.info("put user {} ", request);

      var b = fromJson(mapper, request.getBody(), UpdateUserRequest.class);
      return userClient.update(userId, b)
        .thenApply(response -> {
          var body = toJson(response, mapper);
          log.info("put user ok {} {}", UPDATE_USER, body);
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(headers)
            .withBody(body);
        })
        .exceptionally(e -> {
          log.error("put user not ok {} {}", e.getMessage(), UPDATE_USER);
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(headers);
        });
    } else if (request.getHttpMethod().equals("DELETE") && request.getResource().equals(DELETE_USER)) {
      log.info("delete user {}", request);

      return userClient.unsubscribe(userId)
        .thenApply(response -> {
          var body = response.toString();
          log.info("delete user ok {} {}", DELETE_USER, body);
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(headers)
            .withBody(body);
        })
        .exceptionally(e -> {
          log.error("delete user not ok {} {}", e.getMessage(), DELETE_USER);
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(headers);
        });
    } else {
      return CompletableFuture.supplyAsync(() -> {
        log.error("user api request not ok {}", request);
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withHeaders(headers);
      });
    }
  }
}
