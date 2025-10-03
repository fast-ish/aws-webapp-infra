package ui.webapp.execute;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import ui.webapp.Env;
import ui.webapp.Logging;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import ui.webapp.model.User;
import ui.webapp.model.request.UpdateUserRequest;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UserClient {
  private final Logger log = Logging.handler();
  private final DynamoDbAsyncClient dynamoDbClient;
  private final CognitoIdentityProviderAsyncClient cognitoClient;

  public UserClient(DynamoDbAsyncClient dynamoDbClient, CognitoIdentityProviderAsyncClient cognitoClient) {
    this.dynamoDbClient = dynamoDbClient;
    this.cognitoClient = cognitoClient;
  }

  @SneakyThrows
  public CompletableFuture<User> update(String subscriberId, UpdateUserRequest request) {
    return dynamoDbClient.updateItem(
        UpdateItemRequest.builder()
          .tableName(System.getenv(Env.DYNAMODB_USER_TABLE.name()))
          .key(Map.of("id", AttributeValue.builder().s(subscriberId).build()))
          .updateExpression("SET phone = :phone, username = :username, settings = :settings, updated = :updated")
          .expressionAttributeValues(Map.of(
            ":phone", AttributeValue.builder().s(request.phone()).build(),
            ":username", AttributeValue.builder().s(request.username()).build(),
            ":settings", AttributeValue.builder().m(request.settings().attributeValue()).build(),
            ":updated", AttributeValue.builder().s(Instant.now().toString()).build()))
          .returnValues(ReturnValue.ALL_NEW)
          .build())
      .thenApply(response -> {
        log.debug("updated subscriber {}", response);
        var attributes = response.attributes();
        return User.from(attributes);
      })
      .exceptionally(e -> {
        log.error("error updating subscriber {} {} {}", subscriberId, request, e.getMessage());
        return null;
      });
  }

  @SneakyThrows
  public CompletableFuture<User> get(String subscriberId) {
    return dynamoDbClient.getItem(
        GetItemRequest.builder()
          .tableName(System.getenv(Env.DYNAMODB_USER_TABLE.name()))
          .key(Map.of("id", AttributeValue.builder().s(subscriberId).build()))
          .consistentRead(true)
          .build())
      .thenApply(response -> {
        log.debug("get subscriber {}", response);
        return User.from(response.item());
      })
      .exceptionally(e -> {
        log.error("error reading subscriber {} {}", subscriberId, e.getMessage());
        return null;
      });
  }

  @SneakyThrows
  public CompletableFuture<Boolean> unsubscribe(String subscriberId) {
    return get(subscriberId)
      .thenCompose(subscriber -> CompletableFuture.allOf(
          deleteUserFromCognito(subscriber.id()),
          delete(subscriberId))
        .thenApply(ok -> true))
      .exceptionally(e -> {
        log.error("error unsubscribing subscriber {} {}", subscriberId, e.getMessage());
        return null;
      });
  }

  private CompletableFuture<Boolean> delete(String subscriberId) {
    return dynamoDbClient.deleteItem(
        DeleteItemRequest.builder()
          .tableName(System.getenv(Env.DYNAMODB_USER_TABLE.name()))
          .key(Map.of("id", AttributeValue.builder().s(subscriberId).build()))
          .returnValues(ReturnValue.NONE)
          .build())
      .thenApply(response -> {
        log.debug("deleted subscriber {}", response);
        return true;
      })
      .exceptionally(e -> {
        log.error("error deleting subscriber {} {}", subscriberId, e.getMessage());
        return false;
      });
  }

  private CompletableFuture<Boolean> deleteUserFromCognito(String username) {
    var userPoolName = System.getenv(Env.USER_POOL_NAME.name());
    return cognitoClient.listUserPools(ListUserPoolsRequest.builder().build())
      .thenCompose(response -> {
        var maybeUserPool = response.userPools().stream()
          .filter(pool -> userPoolName.equals(pool.name()))
          .findFirst();

        if (maybeUserPool.isPresent()) {
          var userPool = maybeUserPool.get();
          return cognitoClient.adminDeleteUser(
              AdminDeleteUserRequest.builder()
                .userPoolId(userPool.id())
                .username(username)
                .build()
            )
            .thenApply(deleteResponse -> true)
            .exceptionally(e -> {
              log.error("error deleting user from cognito {} {}", username, e.getMessage());
              return false;
            });
        } else {
          log.error("user pool {} not found", Env.USER_POOL_NAME.name());
          return CompletableFuture.completedFuture(false);
        }
      })
      .exceptionally(e -> {
        log.error("error listing user pools for deletion {} {}", username, e.getMessage());
        return false;
      });
  }
}
