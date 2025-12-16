package ui.webapp;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;

public class AuthDependencyFactory {

  private static final CognitoIdentityProviderAsyncClient COGNITO_CLIENT = CognitoIdentityProviderAsyncClient.builder()
    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
    .region(Region.of(System.getenv("AWS_DEFAULT_REGION").toLowerCase()))
    .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
    .build();

  private AuthDependencyFactory() {}

  public static CognitoIdentityProviderAsyncClient cognitoIdentityProviderClient() {
    return COGNITO_CLIENT;
  }
}
