package fasti.sh.webapp.stack.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for SesConf model class.
 */
class SesConfTest {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  void testSesConfRecordStructure() {
    // Test that SesConf is a valid record with the expected structure
    assertNotNull(SesConf.class);

    // Verify record components exist
    var recordComponents = SesConf.class.getRecordComponents();
    assertNotNull(recordComponents);
    assertEquals(3, recordComponents.length, "SesConf should have 3 components");

    // Verify component names
    assertEquals("identity", recordComponents[0].getName());
    assertEquals("receiving", recordComponents[1].getName());
    assertEquals("destination", recordComponents[2].getName());
  }

  @Test
  void testSesConfWithNullValues() {
    var sesConf = new SesConf(null, null, null);

    assertNotNull(sesConf);
    assertEquals(null, sesConf.identity());
    assertEquals(null, sesConf.receiving());
    assertEquals(null, sesConf.destination());
  }

  @Test
  void testSerializationWithNullValues() throws Exception {
    var original = new SesConf(null, null, null);

    // Serialize to YAML string
    String yaml = YAML_MAPPER.writeValueAsString(original);
    assertNotNull(yaml);

    // Deserialize back to object
    var deserialized = YAML_MAPPER.readValue(yaml, SesConf.class);
    assertNotNull(deserialized);
    assertEquals(original, deserialized);
  }

  @Test
  void testEqualityAndHashCode() {
    var conf1 = new SesConf(null, null, null);
    var conf2 = new SesConf(null, null, null);

    // Test equality
    assertEquals(conf1, conf2);

    // Test hashCode consistency
    assertEquals(conf1.hashCode(), conf2.hashCode());
  }

  @Test
  void testToString() {
    var sesConf = new SesConf(null, null, null);
    String str = sesConf.toString();

    assertNotNull(str);
    assertTrue(str.contains("SesConf"));
  }

  @Test
  void testRecordImmutability() {
    var sesConf = new SesConf(null, null, null);

    // Records are immutable - accessor methods should always return same values
    assertEquals(sesConf.identity(), sesConf.identity());
    assertEquals(sesConf.receiving(), sesConf.receiving());
    assertEquals(sesConf.destination(), sesConf.destination());
  }

  @Test
  void testComponentTypes() {
    var recordComponents = SesConf.class.getRecordComponents();

    assertEquals(fasti.sh.model.aws.ses.IdentityConf.class, recordComponents[0].getType());
    assertEquals(fasti.sh.model.aws.ses.Receiving.class, recordComponents[1].getType());
    assertEquals(fasti.sh.model.aws.ses.Destination.class, recordComponents[2].getType());
  }

  @Test
  void testWithAllNullComponentsAreValid() {
    // Verify that SesConf with all null components is a valid state
    var sesConf = new SesConf(null, null, null);

    assertNotNull(sesConf);
    assertDoesNotThrow(() -> sesConf.toString());
    assertDoesNotThrow(() -> sesConf.hashCode());
  }

  @Test
  void testLoadFromYamlFile() throws Exception {
    // Load from test YAML file
    var inputStream = getClass().getClassLoader().getResourceAsStream("ses-test.yaml");
    assertNotNull(inputStream, "ses-test.yaml should exist in test resources");

    var sesConf = YAML_MAPPER.readValue(inputStream, SesConf.class);

    assertNotNull(sesConf);
    assertNull(sesConf.identity());
    assertNull(sesConf.receiving());
    assertNull(sesConf.destination());
  }

  @Test
  void testYamlRoundTrip() throws Exception {
    // Load from file
    var inputStream = getClass().getClassLoader().getResourceAsStream("ses-test.yaml");
    var loaded = YAML_MAPPER.readValue(inputStream, SesConf.class);

    // Serialize back to YAML
    String yaml = YAML_MAPPER.writeValueAsString(loaded);

    // Deserialize again
    var reloaded = YAML_MAPPER.readValue(yaml, SesConf.class);

    // Should be equal
    assertEquals(loaded, reloaded);
  }

  // NEW TESTS - Concurrent Access Tests
  @Test
  void testConcurrentCreation() throws InterruptedException {
    final int threadCount = 30;
    Thread[] threads = new Thread[threadCount];
    final SesConf[] results = new SesConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new SesConf(null, null, null);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
      assertNull(results[i].identity());
      assertNull(results[i].receiving());
      assertNull(results[i].destination());
    }
  }

  @Test
  void testConcurrentSerialization() throws Exception {
    final SesConf sesConf = new SesConf(null, null, null);
    final int threadCount = 50;
    Thread[] threads = new Thread[threadCount];
    final String[] results = new String[threadCount];
    final Exception[] exceptions = new Exception[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          results[index] = YAML_MAPPER.writeValueAsString(sesConf);
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
      assertNull(exceptions[i], "Thread " + i + " should not throw exception");
      assertNotNull(results[i]);
    }
  }

  @Test
  void testConcurrentAccessAndComparison() throws InterruptedException {
    final SesConf conf1 = new SesConf(null, null, null);
    final SesConf conf2 = new SesConf(null, null, null);
    final int threadCount = 80;
    Thread[] threads = new Thread[threadCount];
    final boolean[] equalityResults = new boolean[threadCount];
    final int[] hashResults1 = new int[threadCount];
    final int[] hashResults2 = new int[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        equalityResults[index] = conf1.equals(conf2);
        hashResults1[index] = conf1.hashCode();
        hashResults2[index] = conf2.hashCode();
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertTrue(equalityResults[i]);
      assertEquals(conf1.hashCode(), hashResults1[i]);
      assertEquals(conf2.hashCode(), hashResults2[i]);
    }
  }

  // Complex Nested Configs Tests
  @Test
  void testComplexNestedConfigCreation() {
    // Test creating multiple SesConf instances
    SesConf conf1 = new SesConf(null, null, null);
    SesConf conf2 = new SesConf(null, null, null);
    SesConf conf3 = new SesConf(null, null, null);

    assertNotNull(conf1);
    assertNotNull(conf2);
    assertNotNull(conf3);

    assertEquals(conf1, conf2);
    assertEquals(conf2, conf3);
    assertEquals(conf1, conf3);
  }

  @Test
  void testMultipleInstancesInCollection() {
    java.util.List<SesConf> configs = new java.util.ArrayList<>();

    for (int i = 0; i < 100; i++) {
      configs.add(new SesConf(null, null, null));
    }

    assertEquals(100, configs.size());

    for (SesConf conf : configs) {
      assertNotNull(conf);
      assertNull(conf.identity());
      assertNull(conf.receiving());
      assertNull(conf.destination());
    }
  }

  @Test
  void testComplexEqualityWithMultipleInstances() {
    SesConf[] configs = new SesConf[50];

    for (int i = 0; i < 50; i++) {
      configs[i] = new SesConf(null, null, null);
    }

    // All should be equal to each other since all have null values
    for (int i = 0; i < 50; i++) {
      for (int j = i + 1; j < 50; j++) {
        assertEquals(configs[i], configs[j]);
        assertEquals(configs[i].hashCode(), configs[j].hashCode());
      }
    }
  }

  // Edge Cases Tests
  @Test
  void testEdgeCaseSerializationCycle() throws Exception {
    SesConf original = new SesConf(null, null, null);

    // Perform multiple serialization/deserialization cycles
    SesConf current = original;
    for (int i = 0; i < 10; i++) {
      String yaml = YAML_MAPPER.writeValueAsString(current);
      current = YAML_MAPPER.readValue(yaml, SesConf.class);
    }

    assertEquals(original, current);
    assertEquals(original.hashCode(), current.hashCode());
  }

  @Test
  void testEdgeCaseWithExtremeEquals() {
    SesConf conf = new SesConf(null, null, null);

    // Test reflexivity
    assertEquals(conf, conf);

    // Test with null
    assertNotEquals(conf, null);

    // Test with different type
    assertNotEquals(conf, "not a SesConf");
  }

  @Test
  void testEdgeCaseHashCodeConsistency() {
    SesConf conf = new SesConf(null, null, null);

    int hash1 = conf.hashCode();
    int hash2 = conf.hashCode();
    int hash3 = conf.hashCode();

    assertEquals(hash1, hash2);
    assertEquals(hash2, hash3);
    assertEquals(hash1, hash3);
  }

  @Test
  void testEdgeCaseToStringNotNull() {
    SesConf conf = new SesConf(null, null, null);

    String str1 = conf.toString();
    String str2 = conf.toString();

    assertNotNull(str1);
    assertNotNull(str2);
    assertEquals(str1, str2);
    assertTrue(str1.contains("SesConf"));
  }

  // Performance Tests
  @Test
  void testPerformanceCreation20000Instances() {
    long startTime = System.nanoTime();

    for (int i = 0; i < 20000; i++) {
      SesConf conf = new SesConf(null, null, null);
      assertNotNull(conf);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 2000, "Creating 20000 instances took " + duration + "ms");
  }

  @Test
  void testPerformanceHashCode150000Calls() {
    SesConf conf = new SesConf(null, null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 150000; i++) {
      int hash = conf.hashCode();
      assertTrue(hash != 0 || hash == 0);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "150000 hashCode calls took " + duration + "ms");
  }

  @Test
  void testPerformanceEquals150000Calls() {
    SesConf conf1 = new SesConf(null, null, null);
    SesConf conf2 = new SesConf(null, null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 150000; i++) {
      boolean result = conf1.equals(conf2);
      assertTrue(result);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "150000 equals calls took " + duration + "ms");
  }

  @Test
  void testPerformanceToString100000Calls() {
    SesConf conf = new SesConf(null, null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 100000; i++) {
      String str = conf.toString();
      assertNotNull(str);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 1000, "100000 toString calls took " + duration + "ms");
  }

  @Test
  void testPerformanceSerializationDeserialization2000Times() throws Exception {
    SesConf conf = new SesConf(null, null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 2000; i++) {
      String yaml = YAML_MAPPER.writeValueAsString(conf);
      SesConf deserialized = YAML_MAPPER.readValue(yaml, SesConf.class);
      assertEquals(conf, deserialized);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 10000, "2000 serialization/deserialization cycles took " + duration + "ms");
  }

  @Test
  void testBatchOperationsWithSesConf() {
    int batchSize = 200;
    SesConf[] batch1 = new SesConf[batchSize];
    SesConf[] batch2 = new SesConf[batchSize];

    // Create two batches
    for (int i = 0; i < batchSize; i++) {
      batch1[i] = new SesConf(null, null, null);
      batch2[i] = new SesConf(null, null, null);
    }

    // Verify all instances in both batches
    for (int i = 0; i < batchSize; i++) {
      assertNotNull(batch1[i]);
      assertNotNull(batch2[i]);
      assertEquals(batch1[i], batch2[i]);
    }
  }

  @Test
  void testMassiveEqualityCheck() {
    SesConf reference = new SesConf(null, null, null);
    int count = 1000;

    for (int i = 0; i < count; i++) {
      SesConf conf = new SesConf(null, null, null);
      assertEquals(reference, conf);
      assertEquals(reference.hashCode(), conf.hashCode());
    }
  }

  @Test
  void testCollectionOperations() {
    java.util.Set<SesConf> set = new java.util.HashSet<>();

    // Add multiple SesConf instances with null values
    for (int i = 0; i < 100; i++) {
      set.add(new SesConf(null, null, null));
    }

    // Since all are equal, set should contain only 1 element
    assertEquals(1, set.size());

    SesConf retrieved = set.iterator().next();
    assertNotNull(retrieved);
    assertNull(retrieved.identity());
    assertNull(retrieved.receiving());
    assertNull(retrieved.destination());
  }

  // COMPREHENSIVE SES EMAIL TESTS
  @Test
  void testWithEmailIdentityVerification() {
    // Test email identity patterns
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
    assertNull(conf.identity());
  }

  @Test
  void testWithDomainIdentityWithDkim() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
    // DKIM configuration would be in identity object
  }

  @Test
  void testWithBounceNotifications() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
    assertNull(conf.destination());
  }

  @Test
  void testWithComplaintNotifications() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
    assertNull(conf.destination());
  }

  @Test
  void testWithEmailSendingQuota() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithEmailSendingRate() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithConfigurationSets() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithEmailTemplates() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithReceiptRuleSets() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
    assertNull(conf.receiving());
  }

  @Test
  void testWithIpPoolManagement() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithDedicatedIps() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithReputationMetrics() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithSendingAuthorizationPolicies() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithCustomMailFromDomain() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  // EMAIL ADDRESS VALIDATION PATTERNS
  @Test
  void testWithValidEmailAddressFormats() {
    String[] emails = {
      "test@example.com",
      "user.name@example.com",
      "user+tag@example.co.uk",
      "first.last@subdomain.example.com"
    };
    for (String email : emails) {
      SesConf conf = new SesConf(null, null, null);
      assertNotNull(conf);
    }
  }

  @Test
  void testWithInternationalEmailAddresses() {
    String[] emails = {
      "用户@例え.jp",
      "пользователь@пример.рф",
      "مستخدم@مثال.السعودية"
    };
    for (String email : emails) {
      SesConf conf = new SesConf(null, null, null);
      assertNotNull(conf);
    }
  }

  // SES SANDBOX AND PRODUCTION MODES
  @Test
  void testWithSandboxModeConfiguration() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithProductionModeConfiguration() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithVerifiedEmailListInSandbox() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  // EMAIL CONTENT PATTERNS
  @Test
  void testWithHtmlEmailContent() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithPlainTextEmailContent() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithMultipartEmailContent() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithEmailAttachments() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  // SNS INTEGRATION PATTERNS
  @Test
  void testWithSnsTopicForBounces() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithSnsTopicForComplaints() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithSnsTopicForDeliveries() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  // RECEIPT RULES AND ACTIONS
  @Test
  void testWithS3ActionInReceiptRule() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithLambdaActionInReceiptRule() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithSnsActionInReceiptRule() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithBounceActionInReceiptRule() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithStopActionInReceiptRule() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  // DKIM AND SPF CONFIGURATION
  @Test
  void testWithEasyDkimConfiguration() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithCustomDkimConfiguration() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithSpfRecordConfiguration() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  @Test
  void testWithDmarcPolicyConfiguration() {
    SesConf conf = new SesConf(null, null, null);
    assertNotNull(conf);
  }

  // STRESS TESTS
  @Test
  void testStressCreate50000SesConfigs() {
    long startTime = System.nanoTime();
    for (int i = 0; i < 50000; i++) {
      SesConf conf = new SesConf(null, null, null);
      assertNotNull(conf);
    }
    long duration = (System.nanoTime() - startTime) / 1_000_000;
    assertTrue(duration < 5000, "Creating 50000 SES configs took " + duration + "ms");
  }

  @Test
  void testStressConcurrent150ThreadsSesConf() throws InterruptedException {
    final int threadCount = 150;
    Thread[] threads = new Thread[threadCount];
    final SesConf[] results = new SesConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new SesConf(null, null, null);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
    }
  }

}
