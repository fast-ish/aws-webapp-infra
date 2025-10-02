package ui.webapp.model;

import java.util.Map;

public record CognitoRequest(
    Map<String, Object> userAttributes,
    Map<String, Object> clientMetadata,
    Map<String, Object> validationData
) {
}
