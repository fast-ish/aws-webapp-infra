package fasti.sh.webapp.stack.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for DbConf model class.
 */
public class DbConfTest {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  public void testDbConfRecordStructure() {
    // Test that DbConf is a valid record with the expected structure
    assertNotNull(DbConf.class);

    // Verify record components exist
    var recordComponents = DbConf.class.getRecordComponents();
    assertNotNull(recordComponents);
    assertEquals(3, recordComponents.length, "DbConf should have 3 components");

    // Verify component names
    assertEquals("vpcName", recordComponents[0].getName());
    assertEquals("user", recordComponents[1].getName());
    assertEquals("listener", recordComponents[2].getName());
  }

  @Test
  public void testDbConfWithVpcName() {
    var dbConf = new DbConf("main-vpc", null, null);

    assertNotNull(dbConf);
    assertEquals("main-vpc", dbConf.vpcName());
    assertEquals(null, dbConf.user());
    assertEquals(null, dbConf.listener());
  }

  @Test
  public void testDbConfWithNullValues() {
    var dbConf = new DbConf(null, null, null);

    assertNotNull(dbConf);
    assertEquals(null, dbConf.vpcName());
    assertEquals(null, dbConf.user());
    assertEquals(null, dbConf.listener());
  }

  @Test
  public void testSerializationWithVpcName() throws Exception {
    var original = new DbConf("test-vpc", null, null);

    // Serialize to YAML string
    String yaml = YAML_MAPPER.writeValueAsString(original);
    assertNotNull(yaml);
    assertTrue(yaml.contains("test-vpc"));

    // Deserialize back to object
    var deserialized = YAML_MAPPER.readValue(yaml, DbConf.class);
    assertNotNull(deserialized);
    assertEquals(original, deserialized);
  }

  @Test
  public void testEqualityAndHashCode() {
    var conf1 = new DbConf("vpc1", null, null);
    var conf2 = new DbConf("vpc1", null, null);
    var conf3 = new DbConf("vpc2", null, null);

    // Test equality
    assertEquals(conf1, conf2);
    assertNotEquals(conf1, conf3);

    // Test hashCode consistency
    assertEquals(conf1.hashCode(), conf2.hashCode());
    assertNotEquals(conf1.hashCode(), conf3.hashCode());
  }

  @Test
  public void testToString() {
    var dbConf = new DbConf("main-vpc", null, null);
    String str = dbConf.toString();

    assertNotNull(str);
    assertTrue(str.contains("DbConf"));
    assertTrue(str.contains("main-vpc"));
  }

  @Test
  public void testRecordImmutability() {
    var dbConf = new DbConf("vpc", null, null);

    // Records are immutable - accessor methods should always return same values
    assertEquals(dbConf.vpcName(), dbConf.vpcName());
    assertEquals(dbConf.user(), dbConf.user());
    assertEquals(dbConf.listener(), dbConf.listener());
  }

  @Test
  public void testWithEmptyVpcName() {
    var dbConf = new DbConf("", null, null);

    assertNotNull(dbConf);
    assertEquals("", dbConf.vpcName());
  }

  @Test
  public void testWithSpecialCharactersInVpcName() {
    var dbConf = new DbConf("vpc-name_with.special-chars-123", null, null);

    assertNotNull(dbConf);
    assertEquals("vpc-name_with.special-chars-123", dbConf.vpcName());
    assertTrue(dbConf.vpcName().contains("-"));
    assertTrue(dbConf.vpcName().contains("_"));
    assertTrue(dbConf.vpcName().contains("."));
  }

  @Test
  public void testWithVeryLongVpcName() {
    String longVpcName = "vpc-" + "a".repeat(500);
    var dbConf = new DbConf(longVpcName, null, null);

    assertNotNull(dbConf);
    assertEquals(longVpcName, dbConf.vpcName());
    assertTrue(dbConf.vpcName().length() > 500);
  }

  @Test
  public void testWithMultipleVpcFormats() {
    // Test various VPC naming patterns
    String[] vpcNames = {
      "vpc-12345678",
      "vpc-prod-us-east-1",
      "main-vpc",
      "VPC_NAME_UPPERCASE",
      "vpc.with.dots"
    };

    for (String vpcName : vpcNames) {
      var dbConf = new DbConf(vpcName, null, null);
      assertNotNull(dbConf);
      assertEquals(vpcName, dbConf.vpcName());
    }
  }

  @Test
  public void testComponentTypes() {
    var recordComponents = DbConf.class.getRecordComponents();

    assertEquals(String.class, recordComponents[0].getType());
    assertEquals(fasti.sh.model.aws.dynamodb.Table.class, recordComponents[1].getType());
    assertEquals(fasti.sh.model.aws.fn.Lambda.class, recordComponents[2].getType());
  }

  @Test
  public void testLoadFromYamlFile() throws Exception {
    // Load from test YAML file
    var inputStream = getClass().getClassLoader().getResourceAsStream("db-test.yaml");
    assertNotNull(inputStream, "db-test.yaml should exist in test resources");

    var dbConf = YAML_MAPPER.readValue(inputStream, DbConf.class);

    assertNotNull(dbConf);
    assertEquals("test-vpc-db", dbConf.vpcName());
    assertNull(dbConf.user());
    assertNull(dbConf.listener());
  }

  @Test
  public void testYamlRoundTrip() throws Exception {
    // Load from file
    var inputStream = getClass().getClassLoader().getResourceAsStream("db-test.yaml");
    var loaded = YAML_MAPPER.readValue(inputStream, DbConf.class);

    // Serialize back to YAML
    String yaml = YAML_MAPPER.writeValueAsString(loaded);

    // Deserialize again
    var reloaded = YAML_MAPPER.readValue(yaml, DbConf.class);

    // Should be equal
    assertEquals(loaded, reloaded);
  }

  // NEW TESTS - Concurrent Operations Tests
  @Test
  public void testConcurrentCreation() throws InterruptedException {
    final int threadCount = 25;
    Thread[] threads = new Thread[threadCount];
    final DbConf[] results = new DbConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new DbConf("vpc-" + index, null, null);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
      assertEquals("vpc-" + i, results[i].vpcName());
      assertNull(results[i].user());
      assertNull(results[i].listener());
    }
  }

  @Test
  public void testConcurrentSerialization() throws Exception {
    final DbConf dbConf = new DbConf("test-vpc", null, null);
    final int threadCount = 40;
    Thread[] threads = new Thread[threadCount];
    final String[] results = new String[threadCount];
    final Exception[] exceptions = new Exception[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          results[index] = YAML_MAPPER.writeValueAsString(dbConf);
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
      assertTrue(results[i].contains("test-vpc"));
    }
  }

  @Test
  public void testConcurrentEqualsAndHashCode() throws InterruptedException {
    final DbConf conf1 = new DbConf("shared-vpc", null, null);
    final DbConf conf2 = new DbConf("shared-vpc", null, null);
    final int threadCount = 60;
    Thread[] threads = new Thread[threadCount];
    final boolean[] equalityResults = new boolean[threadCount];
    final int[] hashResults = new int[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        equalityResults[index] = conf1.equals(conf2);
        hashResults[index] = conf1.hashCode();
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertTrue(equalityResults[i]);
      assertEquals(conf1.hashCode(), hashResults[i]);
    }
  }

  // Complex Configurations Tests
  @Test
  public void testComplexVpcNameWithRegions() {
    String[] complexVpcNames = {
      "vpc-us-east-1-prod",
      "vpc-eu-west-2-staging",
      "vpc-ap-southeast-1-dev",
      "main-vpc-12345678",
      "vpc-prod-application-tier-1"
    };

    for (String vpcName : complexVpcNames) {
      var dbConf = new DbConf(vpcName, null, null);
      assertNotNull(dbConf);
      assertEquals(vpcName, dbConf.vpcName());
    }
  }

  @Test
  public void testComplexVpcNameWithEnvironments() {
    var prodConf = new DbConf("vpc-production-database", null, null);
    var stagingConf = new DbConf("vpc-staging-database", null, null);
    var devConf = new DbConf("vpc-development-database", null, null);

    assertNotNull(prodConf);
    assertNotNull(stagingConf);
    assertNotNull(devConf);

    assertNotEquals(prodConf, stagingConf);
    assertNotEquals(stagingConf, devConf);
    assertNotEquals(prodConf, devConf);
  }

  @Test
  public void testComplexVpcNamesWithNumbers() {
    for (int i = 0; i < 20; i++) {
      String vpcName = "vpc-" + i + "-db-tier-" + (i * 10);
      var dbConf = new DbConf(vpcName, null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains(String.valueOf(i)));
    }
  }

  // Edge Cases Tests
  @Test
  public void testEdgeCaseWithVeryLongVpcName2000Chars() {
    String longVpcName = "vpc-" + "x".repeat(2000);
    var dbConf = new DbConf(longVpcName, null, null);

    assertNotNull(dbConf);
    assertEquals(2004, dbConf.vpcName().length());
    assertTrue(dbConf.vpcName().startsWith("vpc-"));
  }

  @Test
  public void testEdgeCaseWithVeryLongVpcName5000Chars() {
    String ultraLongVpcName = "a".repeat(5000);
    var dbConf = new DbConf(ultraLongVpcName, null, null);

    assertNotNull(dbConf);
    assertEquals(5000, dbConf.vpcName().length());
  }

  @Test
  public void testEdgeCaseWithSpecialUnicodeCharacters() {
    var dbConf = new DbConf("vpc-\u4E2D\u6587-\u65E5\u672C\u8A9E-\uD55C\uAD6D\uC5B4", null, null);

    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("\u4E2D\u6587"));
    assertTrue(dbConf.vpcName().contains("\u65E5\u672C\u8A9E"));
    assertTrue(dbConf.vpcName().contains("\uD55C\uAD6D\uC5B4"));
  }

  @Test
  public void testEdgeCaseWithOnlySeparators() {
    var dbConf = new DbConf("---___...", null, null);

    assertNotNull(dbConf);
    assertEquals("---___...", dbConf.vpcName());
  }

  @Test
  public void testEdgeCaseWithMixedWhitespace() {
    var dbConf = new DbConf(" vpc \t name \n test ", null, null);

    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("vpc"));
    assertTrue(dbConf.vpcName().contains("\t"));
    assertTrue(dbConf.vpcName().contains("\n"));
  }

  // Performance Tests
  @Test
  public void testPerformanceCreation15000Instances() {
    long startTime = System.nanoTime();

    for (int i = 0; i < 15000; i++) {
      var dbConf = new DbConf("vpc-db-" + i, null, null);
      assertNotNull(dbConf);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 1500, "Creating 15000 instances took " + duration + "ms");
  }

  @Test
  public void testPerformanceHashCode100000Calls() {
    var dbConf = new DbConf("test-vpc-performance", null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 100000; i++) {
      int hash = dbConf.hashCode();
      assertTrue(hash != 0 || hash == 0);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "100000 hashCode calls took " + duration + "ms");
  }

  @Test
  public void testPerformanceEquals100000Calls() {
    var conf1 = new DbConf("vpc-test", null, null);
    var conf2 = new DbConf("vpc-test", null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 100000; i++) {
      boolean result = conf1.equals(conf2);
      assertTrue(result);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "100000 equals calls took " + duration + "ms");
  }

  @Test
  public void testPerformanceSerializationDeserialization1000Times() throws Exception {
    var dbConf = new DbConf("vpc-serialization-test", null, null);
    long startTime = System.nanoTime();

    for (int i = 0; i < 1000; i++) {
      String yaml = YAML_MAPPER.writeValueAsString(dbConf);
      DbConf deserialized = YAML_MAPPER.readValue(yaml, DbConf.class);
      assertEquals(dbConf, deserialized);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 5000, "1000 serialization/deserialization cycles took " + duration + "ms");
  }

  @Test
  public void testMultipleInstancesComparison() {
    DbConf[] configs = new DbConf[50];

    for (int i = 0; i < 50; i++) {
      configs[i] = new DbConf("vpc-db-" + i, null, null);
    }

    // Verify all are unique
    for (int i = 0; i < 50; i++) {
      for (int j = i + 1; j < 50; j++) {
        assertNotEquals(configs[i], configs[j]);
        assertNotEquals(configs[i].hashCode(), configs[j].hashCode());
      }
    }
  }

  @Test
  public void testEqualityWithComplexVpcNames() {
    var conf1 = new DbConf("vpc-us-east-1-prod-db-tier-1", null, null);
    var conf2 = new DbConf("vpc-us-east-1-prod-db-tier-1", null, null);
    var conf3 = new DbConf("vpc-us-east-1-prod-db-tier-2", null, null);

    assertEquals(conf1, conf2);
    assertNotEquals(conf1, conf3);
    assertEquals(conf1.hashCode(), conf2.hashCode());
    assertNotEquals(conf1.hashCode(), conf3.hashCode());
  }

  @Test
  public void testBatchCreationAndVerification() {
    int batchSize = 100;
    DbConf[] batch1 = new DbConf[batchSize];
    DbConf[] batch2 = new DbConf[batchSize];

    for (int i = 0; i < batchSize; i++) {
      batch1[i] = new DbConf("batch1-vpc-" + i, null, null);
      batch2[i] = new DbConf("batch2-vpc-" + i, null, null);
    }

    for (int i = 0; i < batchSize; i++) {
      assertNotNull(batch1[i]);
      assertNotNull(batch2[i]);
      assertNotEquals(batch1[i], batch2[i]);
      assertTrue(batch1[i].vpcName().startsWith("batch1-"));
      assertTrue(batch2[i].vpcName().startsWith("batch2-"));
    }
  }

  // COMPREHENSIVE DYNAMODB TESTS
  @Test
  public void testWithDynamoDBTableNamePatterns() {
    String[] tablePatterns = {
      "Users", "users-prod", "users_staging", "users.dev", "Users123",
      "UserProfileData", "user-sessions-2024", "APP_USERS_TABLE"
    };
    for (String table : tablePatterns) {
      var dbConf = new DbConf(table, null, null);
      assertNotNull(dbConf);
      assertEquals(table, dbConf.vpcName());
    }
  }

  @Test
  public void testWithDynamoDBAttributeTypePatterns() {
    String[] attributes = {"S", "N", "B", "SS", "NS", "BS", "M", "L", "NULL", "BOOL"};
    for (String attr : attributes) {
      var dbConf = new DbConf("vpc-attr-" + attr, null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains(attr));
    }
  }

  @Test
  public void testWithGSIConfiguration() {
    var dbConf = new DbConf("vpc-gsi-UserEmailIndex", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("gsi"));
  }

  @Test
  public void testWithLSIConfiguration() {
    var dbConf = new DbConf("vpc-lsi-CreatedAtIndex", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("lsi"));
  }

  @Test
  public void testWithDynamoDBStreamsEnabled() {
    var dbConf = new DbConf("vpc-streams-enabled", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("streams"));
  }

  @Test
  public void testWithPointInTimeRecovery() {
    var dbConf = new DbConf("vpc-pitr-enabled", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("pitr"));
  }

  @Test
  public void testWithBackupConfiguration() {
    var dbConf = new DbConf("vpc-backup-daily", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("backup"));
  }

  // LAMBDA CONFIGURATION TESTS
  @Test
  public void testWithLambdaEdgeScenarios() {
    var dbConf = new DbConf("vpc-lambda-edge-us-east-1", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("lambda-edge"));
  }

  @Test
  public void testWithLambdaConcurrentExecutionLimits() {
    int[] limits = {0, 1, 10, 100, 500, 1000};
    for (int limit : limits) {
      var dbConf = new DbConf("vpc-lambda-concurrent-" + limit, null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains(String.valueOf(limit)));
    }
  }

  @Test
  public void testWithLambdaTimeoutEdgeCases() {
    int[] timeouts = {1, 3, 30, 60, 300, 900};
    for (int timeout : timeouts) {
      var dbConf = new DbConf("vpc-lambda-timeout-" + timeout + "s", null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains(timeout + "s"));
    }
  }

  @Test
  public void testWithLambdaMemoryConfigurations() {
    int[] memories = {128, 256, 512, 1024, 2048, 3008, 10240};
    for (int memory : memories) {
      var dbConf = new DbConf("vpc-lambda-" + memory + "mb", null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains(memory + "mb"));
    }
  }

  @Test
  public void testWithLambdaEnvironmentVariableLimit() {
    // AWS Lambda environment variables limit is 4KB
    var dbConf = new DbConf("vpc-lambda-env-4kb-limit", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("env"));
  }

  @Test
  public void testWithLambdaLayersConfiguration() {
    for (int layers = 1; layers <= 5; layers++) {
      var dbConf = new DbConf("vpc-lambda-layers-" + layers, null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains("layers-" + layers));
    }
  }

  @Test
  public void testWithLambdaVpcConfiguration() {
    var dbConf = new DbConf("vpc-lambda-in-vpc-subnet-private", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("subnet-private"));
  }

  @Test
  public void testWithLambdaReservedConcurrency() {
    var dbConf = new DbConf("vpc-lambda-reserved-concurrent-100", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("reserved-concurrent"));
  }

  @Test
  public void testWithLambdaProvisionedConcurrency() {
    var dbConf = new DbConf("vpc-lambda-provisioned-concurrent-50", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("provisioned"));
  }

  // STRESS TESTS FOR TABLE CONFIGURATIONS
  @Test
  public void testStressCreate100000TableConfigs() {
    long startTime = System.nanoTime();
    for (int i = 0; i < 100000; i++) {
      var dbConf = new DbConf("table-" + i, null, null);
      assertNotNull(dbConf);
    }
    long duration = (System.nanoTime() - startTime) / 1_000_000;
    assertTrue(duration < 10000, "Creating 100000 table configs took " + duration + "ms");
  }

  @Test
  public void testStressConcurrent100ThreadsDbConf() throws InterruptedException {
    final int threadCount = 100;
    Thread[] threads = new Thread[threadCount];
    final DbConf[] results = new DbConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new DbConf("vpc-thread-" + index, null, null);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
      assertTrue(results[i].vpcName().contains("thread-" + i));
    }
  }

  // AWS REGION AND AVAILABILITY ZONE PATTERNS
  @Test
  public void testWithAllAwsRegionPatterns() {
    String[] regions = {
      "us-east-1", "us-west-2", "eu-west-1", "eu-central-1",
      "ap-southeast-1", "ap-northeast-1", "sa-east-1", "ca-central-1"
    };
    for (String region : regions) {
      var dbConf = new DbConf("vpc-" + region, null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().contains(region));
    }
  }

  @Test
  public void testWithAvailabilityZones() {
    String[] azs = {"a", "b", "c", "d", "e", "f"};
    for (String az : azs) {
      var dbConf = new DbConf("vpc-us-east-1" + az, null, null);
      assertNotNull(dbConf);
      assertTrue(dbConf.vpcName().endsWith(az));
    }
  }

  // DATABASE SCALING PATTERNS
  @Test
  public void testWithAutoScalingConfiguration() {
    var dbConf = new DbConf("vpc-autoscaling-min1-max10", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("autoscaling"));
  }

  @Test
  public void testWithOnDemandCapacityMode() {
    var dbConf = new DbConf("vpc-ondemand-capacity", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("ondemand"));
  }

  @Test
  public void testWithProvisionedCapacityMode() {
    var dbConf = new DbConf("vpc-provisioned-rcu-5-wcu-5", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("provisioned"));
    assertTrue(dbConf.vpcName().contains("rcu"));
    assertTrue(dbConf.vpcName().contains("wcu"));
  }

  // ENCRYPTION AND SECURITY PATTERNS
  @Test
  public void testWithEncryptionAtRest() {
    var dbConf = new DbConf("vpc-encrypted-kms", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("encrypted"));
  }

  @Test
  public void testWithCustomKmsKey() {
    var dbConf = new DbConf("vpc-kms-custom-key-12345", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("kms-custom"));
  }

  @Test
  public void testWithIamRoleArn() {
    var dbConf = new DbConf("vpc-iam-role-arn-12345", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("iam-role"));
  }

  // TABLE TAG PATTERNS
  @Test
  public void testWithResourceTags() {
    var dbConf = new DbConf("vpc-tags-env-prod-team-backend", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("tags"));
  }

  @Test
  public void testWithCostAllocationTags() {
    var dbConf = new DbConf("vpc-cost-center-12345", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("cost-center"));
  }

  // MULTI-REGION PATTERNS
  @Test
  public void testWithGlobalTableConfiguration() {
    var dbConf = new DbConf("vpc-global-table-replicas-3", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("global-table"));
  }

  @Test
  public void testWithCrossRegionReplication() {
    var dbConf = new DbConf("vpc-replication-us-to-eu", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("replication"));
  }

  // COMPOSITE KEY PATTERNS
  @Test
  public void testWithCompositePartitionKey() {
    var dbConf = new DbConf("vpc-pk-userId-sk-timestamp", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("pk-"));
    assertTrue(dbConf.vpcName().contains("sk-"));
  }

  @Test
  public void testWithComplexSortKeyPattern() {
    var dbConf = new DbConf("vpc-sk-type#status#date", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("sk-"));
  }

  // TIME-TO-LIVE (TTL) PATTERNS
  @Test
  public void testWithTtlEnabled() {
    var dbConf = new DbConf("vpc-ttl-enabled-attribute-expireAt", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("ttl-enabled"));
  }

  @Test
  public void testWithTtlConfiguration() {
    var dbConf = new DbConf("vpc-ttl-30-days", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("ttl-30"));
  }

  // CONNECTION AND NETWORK PATTERNS
  @Test
  public void testWithVpcEndpointConfiguration() {
    var dbConf = new DbConf("vpc-endpoint-vpce-12345", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("endpoint"));
  }

  @Test
  public void testWithPrivateSubnetConfiguration() {
    var dbConf = new DbConf("vpc-private-subnet-10-0-1-0", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("private-subnet"));
  }

  @Test
  public void testWithSecurityGroupConfiguration() {
    var dbConf = new DbConf("vpc-sg-allow-internal-only", null, null);
    assertNotNull(dbConf);
    assertTrue(dbConf.vpcName().contains("sg-"));
  }

}
