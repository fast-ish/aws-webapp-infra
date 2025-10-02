package ui.webapp.model;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.SneakyThrows;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Builder
public record Mfa(
  boolean enabled,
  boolean configured
) {

  @SneakyThrows
  public static Mfa from(ObjectMapper mapper, Object o) {
    var node = mapper.readTree((String) o);
    return mapper.treeToValue(node, Mfa.class);
  }

  public static Mfa from (Map<String, AttributeValue> attributes) {
    return Mfa.builder()
      .enabled(attributes.get("enabled").bool())
      .configured(attributes.get("configured").bool())
      .build();
  }

  public Map<String, AttributeValue> attributeValue() {
    return Map.of(
      "enabled", AttributeValue.builder().bool(enabled()).build(),
      "configured", AttributeValue.builder().bool(configured()).build()
    );
  }
}
