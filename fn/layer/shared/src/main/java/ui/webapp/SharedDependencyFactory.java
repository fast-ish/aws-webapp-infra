package ui.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class SharedDependencyFactory {

  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  private static final DynamoDbAsyncClient DYNAMODB_CLIENT = DynamoDbAsyncClient.builder()
    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
    .region(Region.of(System.getenv("AWS_DEFAULT_REGION")))
    .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
    .build();

  private SharedDependencyFactory() {}

  public static ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

  public static DynamoDbAsyncClient dynamoDbAsyncClient() {
    return DYNAMODB_CLIENT;
  }
}
