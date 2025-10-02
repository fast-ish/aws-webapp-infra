package ui.webapp.model;

import lombok.Builder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Builder
public record Verification(
  boolean email,
  boolean phone,
  boolean terms,
  String status
) {

  public static Verification from(Map<String, AttributeValue> attributes) {
    return Verification.builder()
      .email(attributes.get("email").bool())
      .phone(attributes.get("phone").bool())
      .terms(attributes.get("terms").bool())
      .status(attributes.get("status").s())
      .build();
  }

  public Map<String, AttributeValue> attributeValue() {
    return Map.of(
      "email", AttributeValue.builder().bool(email()).build(),
      "phone", AttributeValue.builder().bool(phone()).build(),
      "terms", AttributeValue.builder().bool(terms()).build(),
      "status", AttributeValue.builder().s(status()).build());
  }
}
