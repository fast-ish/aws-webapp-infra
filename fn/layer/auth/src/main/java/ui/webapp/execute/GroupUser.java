package ui.webapp.execute;

import ui.webapp.Logging;
import ui.webapp.model.CognitoEvent;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import ui.webapp.model.Subscription;

import java.util.concurrent.CompletableFuture;

public class GroupUser {
  private final static Logger log = Logging.handler();

  public static CompletableFuture<Boolean> with(
    CognitoEvent event,
    CognitoIdentityProviderAsyncClient cognitoIdentityProviderClient) {

    log.info("check if user is already in group {}", event.userName());

    return cognitoIdentityProviderClient.adminListGroupsForUser(
        AdminListGroupsForUserRequest.builder()
          .username(event.userName())
          .userPoolId(event.userPoolId())
          .build())
      .thenCompose(response -> {
        var isInGroup = response.groups().stream()
          .anyMatch(group -> group.groupName().equalsIgnoreCase(Subscription.FREE.name()));

        if (isInGroup) {
          log.info("user already in group {}", event.userName());
          return CompletableFuture.completedFuture(true);
        } else {
          return addSubscriberToGroup(event, cognitoIdentityProviderClient);
        }
      })
      .exceptionally(e -> {
        log.info("error listing user groups {} {}", event.userName(), e);
        return false;
      });
  }

  private static CompletableFuture<Boolean> addSubscriberToGroup(CognitoEvent event, CognitoIdentityProviderAsyncClient cognitoIdentityProviderClient) {
    return cognitoIdentityProviderClient.adminAddUserToGroup(
        AdminAddUserToGroupRequest.builder()
          .groupName("free")
          .username(event.userName())
          .userPoolId(event.userPoolId())
          .build())
      .thenApply(response -> true)
      .exceptionally(e -> {
        log.info("error adding user to group {} {}", event.userName(), e);
        return false;
      });
  }
}
