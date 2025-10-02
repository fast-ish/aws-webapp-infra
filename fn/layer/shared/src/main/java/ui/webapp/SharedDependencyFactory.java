package ui.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class SharedDependencyFactory {

  private SharedDependencyFactory() {}

  public static ObjectMapper objectMapper() {
    return JsonMapper.builder()
      .build();
  }

  public static DynamoDbAsyncClient dynamoDbAsyncClient() {
    return DynamoDbAsyncClient.builder()
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .region(Region.of(System.getenv("AWS_DEFAULT_REGION")))
      .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
      .build();
  }
}
