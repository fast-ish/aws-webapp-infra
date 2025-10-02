package ui.webapp.execute;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import ui.webapp.Env;
import ui.webapp.Logging;
import ui.webapp.model.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RememberUser {
  private final static Logger log = Logging.handler();

  @SneakyThrows
  public static CompletableFuture<Boolean> with(CognitoEvent event, ObjectMapper mapper, DynamoDbAsyncClient dynamoDbClient) {
    log.info("check if user exists {}", event.userName());

    return dynamoDbClient.getItem(
        GetItemRequest.builder()
          .tableName(System.getenv(Env.DYNAMODB_USER_TABLE.name()))
          .key(Map.of("id", AttributeValue.builder().s(event.userName()).build()))
          .build())
      .thenCompose(item -> {
        if (!item.hasItem()) {
          return create(event, mapper, dynamoDbClient);
        } else {
          log.info("user already exists {}", event.userName());
          return CompletableFuture.completedFuture(true);
        }
      }).exceptionally(e -> {
        log.error("error grouping user {} {}", event.userName(), e);
        return false;
      });
  }

  @SneakyThrows
  public static CompletableFuture<Boolean> create(CognitoEvent event, ObjectMapper mapper, DynamoDbAsyncClient dynamoDbClient) {
    log.info("create user {}", event);

    var user = User.builder()
      .id(event.userName())
      .email((String) event.request().userAttributes().get("email"))
      .phone((String) event.request().userAttributes().get("phone_number"))
      .username((String) event.request().userAttributes().get("preferred_username"))
      .verification(Verification.builder()
        .terms(from(event.request().userAttributes().get("custom:terms")))
        .email(from(event.request().userAttributes().get("email_verified")))
        .phone(from(event.request().userAttributes().get("phone_number_verified")))
        .status((String) event.request().userAttributes().get("cognito:user_status"))
        .build())
      .settings(Settings.builder()
        .mfa(Mfa.from(mapper, event.request().userAttributes().get("custom:mfa")))
        .theme(theme(event))
        .subscription(Subscription.FREE)
        .build())
      .updated(Instant.now().toString())
      .build();

    return dynamoDbClient.putItem(
        PutItemRequest.builder()
          .tableName(System.getenv(Env.DYNAMODB_USER_TABLE.name()))
          .item(user.attributeValue())
          .build())
      .thenApply(remembered -> true)
      .exceptionally(e -> {
        log.error("error creating user {} {}", event, e);
        return false;
      });
  }

  private static String theme(CognitoEvent event) {
    return Optional
      .ofNullable(event.request().clientMetadata())
      .map(metadata -> (String) metadata.get("theme"))
      .orElse("light");
  }

  private static boolean from(Object o) {
    if (o instanceof Boolean) {
      return (Boolean) o;
    }

    if (o instanceof String s) {
      if ("true".equalsIgnoreCase(s)) {
        return Boolean.TRUE;
      } else if ("false".equalsIgnoreCase(s)) {
        return Boolean.FALSE;
      }
    }

    return false;
  }
}
