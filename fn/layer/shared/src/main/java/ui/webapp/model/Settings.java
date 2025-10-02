package ui.webapp.model;

import lombok.Builder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Builder
public record Settings(
  Mfa mfa,
  String theme,
  Subscription subscription
) {

  public static Settings from(Map<String, AttributeValue> attributes) {
    return Settings.builder()
      .mfa(Mfa.from(attributes.get("mfa").m()))
      .theme(attributes.get("theme").s())
      .subscription(Subscription.valueOf(attributes.get("subscription").s()))
      .build();
  }

  public Map<String, AttributeValue> attributeValue() {
    return Map.of(
      "mfa", AttributeValue.builder().m(this.mfa().attributeValue()).build(),
      "theme", AttributeValue.builder().s(this.theme()).build(),
      "subscription", AttributeValue.builder().s(this.subscription().name()).build());
  }
}
