package ui.webapp.model;

import lombok.Builder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Builder
public record User(
  String id,
  String email,
  String phone,
  String username,
  Settings settings,
  Verification verification,
  String updated) {

  public static User from(Map<String, AttributeValue> attributes) {
    if (attributes == null || attributes.isEmpty())
      return null;

    var subscriber = User.builder()
      .id(attributes.get("id").s())
      .email(attributes.get("email").s())
      .username(attributes.get("username").s())
      .settings(Settings.from(attributes.get("settings").m()))
      .verification(Verification.from(attributes.get("verification").m()))
      .updated(attributes.get("updated").s());

    Optional.ofNullable(attributes.get("phone"))
      .map(phone -> subscriber.phone(phone.s()));

    return subscriber.build();
  }

  public Map<String, AttributeValue> attributeValue() {
    var data = new java.util.HashMap<>(Map.ofEntries(
      Map.entry("id", AttributeValue.builder().s(this.id()).build()),
      Map.entry("email", AttributeValue.builder().s(this.email()).build()),
      Map.entry("username", AttributeValue.builder().s(this.username()).build()),
      Map.entry("settings", AttributeValue.builder().m(this.settings().attributeValue()).build()),
      Map.entry("verification", AttributeValue.builder().m(this.verification().attributeValue()).build()),
      Map.entry("updated", AttributeValue.builder().s(Instant.now().toString()).build())));

    if (phone() != null && !phone().isEmpty())
      data.put("phone", AttributeValue.builder().s(this.phone()).build());

    return data;
  }
}
