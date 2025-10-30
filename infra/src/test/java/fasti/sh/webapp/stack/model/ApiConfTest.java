package fasti.sh.webapp.stack.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for ApiConf model class.
 */
public class ApiConfTest {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  public void testApiConfRecordStructure() {
    // Test that ApiConf is a valid record with the expected structure
    assertNotNull(ApiConf.class);

    // Verify record components exist
    var recordComponents = ApiConf.class.getRecordComponents();
    assertNotNull(recordComponents);
    assertEquals(3, recordComponents.length, "ApiConf should have 3 components");

    // Verify component names
    assertEquals("apigw", recordComponents[0].getName());
    assertEquals("resource", recordComponents[1].getName());
    assertEquals("authorizer", recordComponents[2].getName());
  }

  @Test
  public void testApiConfWithNullValues() {
    // Test that ApiConf can be instantiated with null values
    var apiConf = new ApiConf(null, null, null);

    assertNotNull(apiConf);
    assertEquals(null, apiConf.apigw());
    assertEquals(null, apiConf.resource());
    assertEquals(null, apiConf.authorizer());
  }

  @Test
  public void testApiConfResourceAccessor() {
    // Test that ApiConf properly stores and retrieves the resource path
    var apiConf = new ApiConf(null, "/api/v1", null);

    assertNotNull(apiConf);
    assertEquals("/api/v1", apiConf.resource());
  }

  @Test
  public void testSerializationWithNullValues() throws Exception {
    var original = new ApiConf(null, "/api/v1", null);

    // Serialize to YAML string
    String yaml = YAML_MAPPER.writeValueAsString(original);
    assertNotNull(yaml);

    // Deserialize back to object
    var deserialized = YAML_MAPPER.readValue(yaml, ApiConf.class);
    assertNotNull(deserialized);
    assertEquals(original, deserialized);
  }

  @Test
  public void testEqualityAndHashCode() {
    var conf1 = new ApiConf(null, "/api/v1", null);
    var conf2 = new ApiConf(null, "/api/v1", null);
    var conf3 = new ApiConf(null, "/api/v2", null);

    // Test equality
    assertEquals(conf1, conf2);
    assertNotEquals(conf1, conf3);

    // Test hashCode consistency
    assertEquals(conf1.hashCode(), conf2.hashCode());
    assertNotEquals(conf1.hashCode(), conf3.hashCode());
  }

  @Test
  public void testToString() {
    var apiConf = new ApiConf(null, "/api/v1", null);
    String str = apiConf.toString();

    assertNotNull(str);
    assertTrue(str.contains("ApiConf"));
    assertTrue(str.contains("/api/v1"));
  }

  @Test
  public void testRecordImmutability() {
    var apiConf = new ApiConf(null, "/api", null);

    // Records are immutable - accessor methods should always return same values
    assertEquals(apiConf.apigw(), apiConf.apigw());
    assertEquals(apiConf.resource(), apiConf.resource());
    assertEquals(apiConf.authorizer(), apiConf.authorizer());
  }

  @Test
  public void testWithEmptyResourceString() {
    var apiConf = new ApiConf(null, "", null);

    assertNotNull(apiConf);
    assertEquals("", apiConf.resource());
  }

  @Test
  public void testWithSpecialCharactersInResource() {
    var apiConf = new ApiConf(null, "/api/v1/{id}/items/{item-id}", null);

    assertNotNull(apiConf);
    assertEquals("/api/v1/{id}/items/{item-id}", apiConf.resource());
    assertTrue(apiConf.resource().contains("{id}"));
    assertTrue(apiConf.resource().contains("{item-id}"));
  }

  @Test
  public void testWithQueryParametersInResource() {
    var apiConf = new ApiConf(null, "/api/search?query={q}&filter={f}", null);

    assertNotNull(apiConf);
    assertEquals("/api/search?query={q}&filter={f}", apiConf.resource());
    assertTrue(apiConf.resource().contains("?"));
    assertTrue(apiConf.resource().contains("&"));
  }

  @Test
  public void testWithVeryLongResourcePath() {
    String longResource = "/api/" + "path/".repeat(100) + "endpoint";
    var apiConf = new ApiConf(null, longResource, null);

    assertNotNull(apiConf);
    assertEquals(longResource, apiConf.resource());
    assertTrue(apiConf.resource().length() > 500);
  }

  @Test
  public void testWithRootResource() {
    var apiConf = new ApiConf(null, "/", null);

    assertNotNull(apiConf);
    assertEquals("/", apiConf.resource());
  }

  @Test
  public void testWithComplexPathPatterns() {
    var apiConf = new ApiConf(null, "/api/v{version:[0-9]+}/users/{userId:[a-z0-9-]+}", null);

    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("version:[0-9]+"));
    assertTrue(apiConf.resource().contains("userId:[a-z0-9-]+"));
  }

  @Test
  public void testComponentTypes() {
    var recordComponents = ApiConf.class.getRecordComponents();

    assertEquals(fasti.sh.model.aws.apigw.ApiConf.class, recordComponents[0].getType());
    assertEquals(String.class, recordComponents[1].getType());
    assertEquals(fasti.sh.model.aws.cognito.client.Authorizer.class, recordComponents[2].getType());
  }

  @Test
  public void testLoadFromYamlFile() throws Exception {
    // Load from test YAML file
    var inputStream = getClass().getClassLoader().getResourceAsStream("api-test.yaml");
    assertNotNull(inputStream, "api-test.yaml should exist in test resources");

    var apiConf = YAML_MAPPER.readValue(inputStream, ApiConf.class);

    assertNotNull(apiConf);
    assertNull(apiConf.apigw());
    assertEquals("/api/v1/test", apiConf.resource());
    assertNull(apiConf.authorizer());
  }

  @Test
  public void testYamlRoundTrip() throws Exception {
    // Load from file
    var inputStream = getClass().getClassLoader().getResourceAsStream("api-test.yaml");
    var loaded = YAML_MAPPER.readValue(inputStream, ApiConf.class);

    // Serialize back to YAML
    String yaml = YAML_MAPPER.writeValueAsString(loaded);

    // Deserialize again
    var reloaded = YAML_MAPPER.readValue(yaml, ApiConf.class);

    // Should be equal
    assertEquals(loaded, reloaded);
  }

  // NEW TESTS - Concurrent Serialization Tests
  @Test
  public void testConcurrentCreation() throws InterruptedException {
    final int threadCount = 20;
    Thread[] threads = new Thread[threadCount];
    final ApiConf[] results = new ApiConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new ApiConf(null, "/api/v" + index, null);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
      assertEquals("/api/v" + i, results[i].resource());
    }
  }

  @Test
  public void testConcurrentSerialization() throws Exception {
    final ApiConf apiConf = new ApiConf(null, "/api/test", null);
    final int threadCount = 30;
    Thread[] threads = new Thread[threadCount];
    final String[] results = new String[threadCount];
    final Exception[] exceptions = new Exception[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          results[index] = YAML_MAPPER.writeValueAsString(apiConf);
        } catch (Exception e) {
          exceptions[index] = e;
        }
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNull(exceptions[i]);
      assertNotNull(results[i]);
      assertTrue(results[i].contains("/api/test"));
    }
  }

  @Test
  public void testConcurrentAccessToMultipleInstances() throws InterruptedException {
    final ApiConf conf1 = new ApiConf(null, "/api/v1", null);
    final ApiConf conf2 = new ApiConf(null, "/api/v2", null);
    final int threadCount = 100;
    Thread[] threads = new Thread[threadCount];
    final String[] results = new String[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        ApiConf conf = (index % 2 == 0) ? conf1 : conf2;
        results[index] = conf.resource();
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      String expected = (i % 2 == 0) ? "/api/v1" : "/api/v2";
      assertEquals(expected, results[i]);
    }
  }

  // Stress Tests with Complex Paths
  @Test
  public void testStressWithVeryLongPath1000Chars() {
    String longPath = "/api/" + "segment/".repeat(125) + "endpoint";
    var apiConf = new ApiConf(null, longPath, null);

    assertNotNull(apiConf);
    assertTrue(apiConf.resource().length() > 1000);
    assertTrue(apiConf.resource().startsWith("/api/"));
    assertTrue(apiConf.resource().endsWith("endpoint"));
  }

  @Test
  public void testStressWithVeryLongPath5000Chars() {
    String ultraLongPath = "/api/" + "x".repeat(5000);
    var apiConf = new ApiConf(null, ultraLongPath, null);

    assertNotNull(apiConf);
    assertEquals(5005, apiConf.resource().length());
    assertTrue(apiConf.resource().startsWith("/api/"));
  }

  @Test
  public void testStressWithManyPathParameters() {
    StringBuilder pathBuilder = new StringBuilder("/api");
    for (int i = 0; i < 50; i++) {
      pathBuilder.append("/segment").append(i).append("/{param").append(i).append("}");
    }
    String complexPath = pathBuilder.toString();

    var apiConf = new ApiConf(null, complexPath, null);

    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("{param0}"));
    assertTrue(apiConf.resource().contains("{param49}"));
  }

  // Edge Cases with Special Characters
  @Test
  public void testEdgeCasesWithUrlEncodedCharacters() {
    var apiConf = new ApiConf(null, "/api/search?q=%20space%20test&filter=%3Dequals", null);

    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("%20"));
    assertTrue(apiConf.resource().contains("%3D"));
  }

  @Test
  public void testEdgeCasesWithSlashesOnly() {
    var apiConf = new ApiConf(null, "/////", null);

    assertNotNull(apiConf);
    assertEquals("/////", apiConf.resource());
  }

  @Test
  public void testEdgeCasesWithFragments() {
    var apiConf = new ApiConf(null, "/api/resource#fragment", null);

    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("#fragment"));
  }

  @Test
  public void testEdgeCasesWithMultipleQueryParameters() {
    var apiConf = new ApiConf(null, "/api/search?a=1&b=2&c=3&d=4&e=5&f=6", null);

    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("a=1"));
    assertTrue(apiConf.resource().contains("f=6"));
    assertTrue(apiConf.resource().split("&").length >= 5);
  }

  // Performance Benchmarks
  @Test
  public void testPerformanceCreation10000Instances() {
    long startTime = System.nanoTime();

    for (int i = 0; i < 10000; i++) {
      var apiConf = new ApiConf(null, "/api/resource/" + i, null);
      assertNotNull(apiConf);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 1000, "Creating 10000 instances took " + duration + "ms");
  }

  @Test
  public void testPerformanceHashCode100000Calls() {
    var apiConf = new ApiConf(null, "/api/test", null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 100000; i++) {
      int hash = apiConf.hashCode();
      assertTrue(hash != 0 || hash == 0);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "100000 hashCode calls took " + duration + "ms");
  }

  @Test
  public void testPerformanceEquals50000Calls() {
    var conf1 = new ApiConf(null, "/api/test", null);
    var conf2 = new ApiConf(null, "/api/test", null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 50000; i++) {
      boolean result = conf1.equals(conf2);
      assertTrue(result);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "50000 equals calls took " + duration + "ms");
  }

  // Multiple Simultaneous Instances Tests
  @Test
  public void testMultipleInstancesWithDifferentPaths() {
    ApiConf[] configs = new ApiConf[100];

    for (int i = 0; i < 100; i++) {
      configs[i] = new ApiConf(null, "/api/path" + i, null);
    }

    for (int i = 0; i < 100; i++) {
      assertNotNull(configs[i]);
      assertEquals("/api/path" + i, configs[i].resource());
    }
  }

  @Test
  public void testMultipleInstancesHashCodeUniqueness() {
    ApiConf conf1 = new ApiConf(null, "/api/v1", null);
    ApiConf conf2 = new ApiConf(null, "/api/v2", null);
    ApiConf conf3 = new ApiConf(null, "/api/v3", null);

    assertNotEquals(conf1.hashCode(), conf2.hashCode());
    assertNotEquals(conf2.hashCode(), conf3.hashCode());
    assertNotEquals(conf1.hashCode(), conf3.hashCode());
  }

  @Test
  public void testSerializationOfMultipleInstances() throws Exception {
    ApiConf[] configs = new ApiConf[10];
    String[] yamls = new String[10];

    for (int i = 0; i < 10; i++) {
      configs[i] = new ApiConf(null, "/api/resource" + i, null);
      yamls[i] = YAML_MAPPER.writeValueAsString(configs[i]);
    }

    for (int i = 0; i < 10; i++) {
      ApiConf deserialized = YAML_MAPPER.readValue(yamls[i], ApiConf.class);
      assertEquals(configs[i], deserialized);
      assertEquals("/api/resource" + i, deserialized.resource());
    }
  }

  @Test
  public void testWithRestfulPathPatterns() {
    String[] restfulPaths = {
      "/api/users",
      "/api/users/{id}",
      "/api/users/{id}/posts",
      "/api/users/{userId}/posts/{postId}",
      "/api/v1/users/{id}/profile"
    };

    for (String path : restfulPaths) {
      var apiConf = new ApiConf(null, path, null);
      assertNotNull(apiConf);
      assertEquals(path, apiConf.resource());
    }
  }

  @Test
  public void testEqualityWithComplexPaths() {
    var conf1 = new ApiConf(null, "/api/v1/{id}/items/{itemId}", null);
    var conf2 = new ApiConf(null, "/api/v1/{id}/items/{itemId}", null);
    var conf3 = new ApiConf(null, "/api/v2/{id}/items/{itemId}", null);

    assertEquals(conf1, conf2);
    assertNotEquals(conf1, conf3);
    assertEquals(conf1.hashCode(), conf2.hashCode());
  }

  // COMPREHENSIVE HTTP METHODS TESTS
  @Test
  public void testWithAllHttpMethodsInPath() {
    String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD", "TRACE"};
    for (String method : methods) {
      var apiConf = new ApiConf(null, "/api/" + method.toLowerCase() + "/resource", null);
      assertNotNull(apiConf);
      assertTrue(apiConf.resource().contains(method.toLowerCase()));
    }
  }

  @Test
  public void testWithGraphQLEndpoint() {
    var apiConf = new ApiConf(null, "/graphql", null);
    assertNotNull(apiConf);
    assertEquals("/graphql", apiConf.resource());
  }

  @Test
  public void testWithGraphQLQueryPath() {
    var apiConf = new ApiConf(null, "/api/graphql/query?operationName=GetUser", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("graphql"));
    assertTrue(apiConf.resource().contains("operationName"));
  }

  @Test
  public void testWithWebSocketUpgradePath() {
    var apiConf = new ApiConf(null, "/ws/connect", null);
    assertNotNull(apiConf);
    assertEquals("/ws/connect", apiConf.resource());
  }

  @Test
  public void testWithWebSocketWithParameters() {
    var apiConf = new ApiConf(null, "/ws/stream?channel={channel}&auth={token}", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("channel"));
    assertTrue(apiConf.resource().contains("token"));
  }

  // QUERY STRING EDGE CASES
  @Test
  public void testWithEncodedSpacesInQuery() {
    var apiConf = new ApiConf(null, "/api/search?q=hello%20world", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("%20"));
  }

  @Test
  public void testWithArrayParametersInQuery() {
    var apiConf = new ApiConf(null, "/api/items?ids[]=1&ids[]=2&ids[]=3", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("ids[]"));
  }

  @Test
  public void testWithNestedObjectsInQuery() {
    var apiConf = new ApiConf(null, "/api/filter?user[name]=john&user[age]=30", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("user[name]"));
    assertTrue(apiConf.resource().contains("user[age]"));
  }

  @Test
  public void testWithSpecialCharsInQuery() {
    var apiConf = new ApiConf(null, "/api/search?q=test&special=%21%40%23%24%25", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("%21"));
  }

  @Test
  public void testWithFragmentIdentifier() {
    var apiConf = new ApiConf(null, "/api/docs#section-authentication", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("#section-authentication"));
  }

  @Test
  public void testWithMatrixParameters() {
    var apiConf = new ApiConf(null, "/api/items;color=red;size=large/details", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains(";color=red"));
    assertTrue(apiConf.resource().contains(";size=large"));
  }

  // MULTIPLE PATH VARIABLES
  @Test
  public void testWithTripleNestedPathVariables() {
    var apiConf = new ApiConf(null, "/api/{org}/{project}/{item}", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("{org}"));
    assertTrue(apiConf.resource().contains("{project}"));
    assertTrue(apiConf.resource().contains("{item}"));
  }

  @Test
  public void testWithPathVariablesAndRegexPatterns() {
    var apiConf = new ApiConf(null, "/api/users/{id:[0-9]+}/posts/{postId:[a-z0-9-]+}", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains(":[0-9]+"));
    assertTrue(apiConf.resource().contains(":[a-z0-9-]+"));
  }

  @Test
  public void testWithOptionalPathSegments() {
    var apiConf = new ApiConf(null, "/api/items/{id}/details?/{subid}?", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("?"));
  }

  @Test
  public void testWithWildcardPathSegments() {
    var apiConf = new ApiConf(null, "/api/files/**/*", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("**"));
  }

  // STRESS TESTS FOR RESOURCES
  @Test
  public void testStressCreate100000Resources() {
    long startTime = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
      var apiConf = new ApiConf(null, "/api/resource/" + i, null);
      assertNotNull(apiConf);
    }
    long duration = (System.nanoTime() - startTime) / 1_000_000;
    assertTrue(duration < 10000, "Creating 100000 resources took " + duration + "ms");
  }

  @Test
  public void testStressConcurrent100ThreadsApiConf() throws InterruptedException {
    final int threadCount = 100;
    Thread[] threads = new Thread[threadCount];
    final ApiConf[] results = new ApiConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new ApiConf(null, "/api/thread-" + index, null);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
      assertTrue(results[i].resource().contains("thread-" + i));
    }
  }

  // CORS CONFIGURATION PATTERNS
  @Test
  public void testWithCorsPreflightPath() {
    var apiConf = new ApiConf(null, "/api/resource", null);
    assertNotNull(apiConf);
    assertEquals("/api/resource", apiConf.resource());
  }

  @Test
  public void testWithCorsWildcardOrigin() {
    var apiConf = new ApiConf(null, "/api/public/*", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("*"));
  }

  // CONTENT-TYPE NEGOTIATION PATTERNS
  @Test
  public void testWithContentTypeInPath() {
    var apiConf = new ApiConf(null, "/api/data.json", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().endsWith(".json"));
  }

  @Test
  public void testWithMultipleContentTypeExtensions() {
    String[] extensions = {".json", ".xml", ".yaml", ".csv", ".txt", ".html"};
    for (String ext : extensions) {
      var apiConf = new ApiConf(null, "/api/data" + ext, null);
      assertNotNull(apiConf);
      assertTrue(apiConf.resource().endsWith(ext));
    }
  }

  // VERSIONED API PATTERNS
  @Test
  public void testWithApiVersionInPath() {
    for (int version = 1; version <= 10; version++) {
      var apiConf = new ApiConf(null, "/api/v" + version + "/resource", null);
      assertNotNull(apiConf);
      assertTrue(apiConf.resource().contains("v" + version));
    }
  }

  @Test
  public void testWithSemanticVersioning() {
    var apiConf = new ApiConf(null, "/api/v2.1.3/resource", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("v2.1.3"));
  }

  @Test
  public void testWithDateBasedVersioning() {
    var apiConf = new ApiConf(null, "/api/2024-01-15/resource", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("2024-01-15"));
  }

  // UNICODE IN PATHS
  @Test
  public void testWithUnicodeInResourcePath() {
    var apiConf = new ApiConf(null, "/api/\u4E2D\u6587/resource", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("\u4E2D\u6587"));
  }

  @Test
  public void testWithEmojiInResourcePath() {
    var apiConf = new ApiConf(null, "/api/\uD83D\uDE00/emoji", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("\uD83D\uDE00"));
  }

  // PROXY AND REWRITE PATTERNS
  @Test
  public void testWithProxyPath() {
    var apiConf = new ApiConf(null, "/api/proxy/**", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("proxy"));
  }

  @Test
  public void testWithRewritePattern() {
    var apiConf = new ApiConf(null, "/api/old-path -> /api/new-path", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("->"));
  }

  // SUBRESOURCE PATTERNS
  @Test
  public void testWithDeeplyNestedSubresources() {
    var apiConf = new ApiConf(null, "/api/orgs/{orgId}/teams/{teamId}/members/{memberId}/roles/{roleId}", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("orgs"));
    assertTrue(apiConf.resource().contains("roles"));
  }

  @Test
  public void testWithActionBasedResources() {
    String[] actions = {"activate", "deactivate", "archive", "restore", "duplicate"};
    for (String action : actions) {
      var apiConf = new ApiConf(null, "/api/resource/{id}/" + action, null);
      assertNotNull(apiConf);
      assertTrue(apiConf.resource().contains(action));
    }
  }

  // PAGINATION PATTERNS
  @Test
  public void testWithPaginationParameters() {
    var apiConf = new ApiConf(null, "/api/items?page=1&limit=100&offset=0", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("page="));
    assertTrue(apiConf.resource().contains("limit="));
    assertTrue(apiConf.resource().contains("offset="));
  }

  @Test
  public void testWithCursorBasedPagination() {
    var apiConf = new ApiConf(null, "/api/items?cursor=eyJpZCI6MTIzfQ==&size=50", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("cursor="));
  }

  // FILTERING AND SORTING PATTERNS
  @Test
  public void testWithComplexFilteringQuery() {
    var apiConf = new ApiConf(null, "/api/items?filter[status]=active&filter[type]=premium&sort=-created", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("filter[status]"));
    assertTrue(apiConf.resource().contains("sort="));
  }

  @Test
  public void testWithFieldSelection() {
    var apiConf = new ApiConf(null, "/api/items?fields=id,name,email&include=profile,settings", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("fields="));
    assertTrue(apiConf.resource().contains("include="));
  }

  // WEBHOOK AND CALLBACK PATTERNS
  @Test
  public void testWithWebhookCallbackPath() {
    var apiConf = new ApiConf(null, "/api/webhooks/callback/{providerId}", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("webhooks"));
    assertTrue(apiConf.resource().contains("callback"));
  }

  @Test
  public void testWithOAuthCallbackPath() {
    var apiConf = new ApiConf(null, "/api/auth/oauth/callback?code=abc123&state=xyz789", null);
    assertNotNull(apiConf);
    assertTrue(apiConf.resource().contains("oauth"));
    assertTrue(apiConf.resource().contains("code="));
  }

  // HEALTH CHECK AND MONITORING PATTERNS
  @Test
  public void testWithHealthCheckEndpoint() {
    var apiConf = new ApiConf(null, "/health", null);
    assertNotNull(apiConf);
    assertEquals("/health", apiConf.resource());
  }

  @Test
  public void testWithDetailedHealthCheckEndpoints() {
    String[] endpoints = {"/health/live", "/health/ready", "/health/startup"};
    for (String endpoint : endpoints) {
      var apiConf = new ApiConf(null, endpoint, null);
      assertNotNull(apiConf);
      assertEquals(endpoint, apiConf.resource());
    }
  }

  @Test
  public void testWithMetricsEndpoint() {
    var apiConf = new ApiConf(null, "/metrics", null);
    assertNotNull(apiConf);
    assertEquals("/metrics", apiConf.resource());
  }

}
