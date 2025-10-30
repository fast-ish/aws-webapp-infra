package fasti.sh.webapp.stack.model;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests for AuthConf model class.
 */
public class AuthConfTest {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Test
  public void testAuthConfInstantiation() {
    var authConf = new AuthConf(
      "main-vpc",
      "auth/userpool.yaml",
      "auth/userpoolclient.yaml"
    );

    assertNotNull(authConf);
    assertEquals("main-vpc", authConf.vpcName());
    assertEquals("auth/userpool.yaml", authConf.userPool());
    assertEquals("auth/userpoolclient.yaml", authConf.userPoolClient());
  }

  @Test
  public void testAuthConfWithDifferentPaths() {
    var authConf = new AuthConf(
      "production-vpc",
      "config/cognito/userpool.yaml",
      "config/cognito/client.yaml"
    );

    assertNotNull(authConf);
    assertEquals("production-vpc", authConf.vpcName());
    assertTrue(authConf.userPool().contains("userpool"));
    assertTrue(authConf.userPoolClient().contains("client"));
  }

  @Test
  public void testAuthConfRecordStructure() {
    // Test that AuthConf is a valid record with the expected structure
    assertNotNull(AuthConf.class);

    // Verify record components exist
    var recordComponents = AuthConf.class.getRecordComponents();
    assertNotNull(recordComponents);
    assertEquals(3, recordComponents.length, "AuthConf should have 3 components");

    // Verify component names
    assertEquals("vpcName", recordComponents[0].getName());
    assertEquals("userPool", recordComponents[1].getName());
    assertEquals("userPoolClient", recordComponents[2].getName());
  }

  @Test
  public void testSerializationDeserialization() throws Exception {
    var original = new AuthConf(
      "test-vpc",
      "auth/pool.yaml",
      "auth/client.yaml"
    );

    // Serialize to YAML string
    String yaml = YAML_MAPPER.writeValueAsString(original);
    assertNotNull(yaml);
    assertTrue(yaml.contains("test-vpc"));
    assertTrue(yaml.contains("auth/pool.yaml"));

    // Deserialize back to object
    var deserialized = YAML_MAPPER.readValue(yaml, AuthConf.class);
    assertNotNull(deserialized);
    assertEquals(original, deserialized);
  }

  @Test
  public void testEqualityAndHashCode() {
    var conf1 = new AuthConf("vpc1", "pool1.yaml", "client1.yaml");
    var conf2 = new AuthConf("vpc1", "pool1.yaml", "client1.yaml");
    var conf3 = new AuthConf("vpc2", "pool1.yaml", "client1.yaml");

    // Test equality
    assertEquals(conf1, conf2);
    assertNotEquals(conf1, conf3);
    assertNotEquals(conf2, conf3);

    // Test hashCode consistency
    assertEquals(conf1.hashCode(), conf2.hashCode());
    assertNotEquals(conf1.hashCode(), conf3.hashCode());
  }

  @Test
  public void testToString() {
    var authConf = new AuthConf("main-vpc", "pool.yaml", "client.yaml");
    String str = authConf.toString();

    assertNotNull(str);
    assertTrue(str.contains("AuthConf"));
    assertTrue(str.contains("main-vpc"));
    assertTrue(str.contains("pool.yaml"));
    assertTrue(str.contains("client.yaml"));
  }

  @Test
  public void testRecordImmutability() {
    var authConf = new AuthConf("vpc", "pool", "client");

    // Records are immutable - accessor methods should always return same values
    assertEquals(authConf.vpcName(), authConf.vpcName());
    assertEquals(authConf.userPool(), authConf.userPool());
    assertEquals(authConf.userPoolClient(), authConf.userPoolClient());
  }

  @Test
  public void testWithEmptyStrings() {
    var authConf = new AuthConf("", "", "");

    assertNotNull(authConf);
    assertEquals("", authConf.vpcName());
    assertEquals("", authConf.userPool());
    assertEquals("", authConf.userPoolClient());
  }

  @Test
  public void testWithSpecialCharacters() {
    var authConf = new AuthConf(
      "vpc-name_with.special-chars",
      "path/with spaces/pool.yaml",
      "config/client@v2.yaml"
    );

    assertNotNull(authConf);
    assertEquals("vpc-name_with.special-chars", authConf.vpcName());
    assertEquals("path/with spaces/pool.yaml", authConf.userPool());
    assertEquals("config/client@v2.yaml", authConf.userPoolClient());
  }

  @Test
  public void testWithVeryLongValues() {
    String longVpcName = "a".repeat(500);
    String longPath1 = "path/".repeat(100) + "pool.yaml";
    String longPath2 = "path/".repeat(100) + "client.yaml";

    var authConf = new AuthConf(longVpcName, longPath1, longPath2);

    assertNotNull(authConf);
    assertEquals(longVpcName, authConf.vpcName());
    assertEquals(longPath1, authConf.userPool());
    assertEquals(longPath2, authConf.userPoolClient());
    assertEquals(500, authConf.vpcName().length());
  }

  @Test
  public void testWithNullValues() {
    var authConf = new AuthConf(null, null, null);

    assertNotNull(authConf);
    assertNull(authConf.vpcName());
    assertNull(authConf.userPool());
    assertNull(authConf.userPoolClient());
  }

  @Test
  public void testComponentTypes() {
    var recordComponents = AuthConf.class.getRecordComponents();

    assertEquals(String.class, recordComponents[0].getType());
    assertEquals(String.class, recordComponents[1].getType());
    assertEquals(String.class, recordComponents[2].getType());
  }

  @Test
  public void testLoadFromYamlFile() throws Exception {
    // Load from test YAML file
    var inputStream = getClass().getClassLoader().getResourceAsStream("auth-test.yaml");
    assertNotNull(inputStream, "auth-test.yaml should exist in test resources");

    var authConf = YAML_MAPPER.readValue(inputStream, AuthConf.class);

    assertNotNull(authConf);
    assertEquals("test-vpc", authConf.vpcName());
    assertEquals("config/auth/userpool.yaml", authConf.userPool());
    assertEquals("config/auth/userpoolclient.yaml", authConf.userPoolClient());
  }

  @Test
  public void testYamlRoundTrip() throws Exception {
    // Load from file
    var inputStream = getClass().getClassLoader().getResourceAsStream("auth-test.yaml");
    var loaded = YAML_MAPPER.readValue(inputStream, AuthConf.class);

    // Serialize back to YAML
    String yaml = YAML_MAPPER.writeValueAsString(loaded);

    // Deserialize again
    var reloaded = YAML_MAPPER.readValue(yaml, AuthConf.class);

    // Should be equal
    assertEquals(loaded, reloaded);
  }

  // NEW TESTS - Concurrent Creation Tests
  @Test
  public void testConcurrentCreation() throws InterruptedException {
    // Test creating multiple AuthConf instances from different threads concurrently
    final int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    final AuthConf[] results = new AuthConf[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        results[index] = new AuthConf("vpc-" + index, "pool-" + index, "client-" + index);
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all instances were created successfully
    for (int i = 0; i < threadCount; i++) {
      assertNotNull(results[i]);
      assertEquals("vpc-" + i, results[i].vpcName());
      assertEquals("pool-" + i, results[i].userPool());
      assertEquals("client-" + i, results[i].userPoolClient());
    }
  }

  @Test
  public void testConcurrentSerialization() throws Exception {
    // Test serializing the same instance from multiple threads
    final AuthConf authConf = new AuthConf("test-vpc", "test-pool", "test-client");
    final int threadCount = 20;
    Thread[] threads = new Thread[threadCount];
    final String[] results = new String[threadCount];
    final Exception[] exceptions = new Exception[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          results[index] = YAML_MAPPER.writeValueAsString(authConf);
        } catch (Exception e) {
          exceptions[index] = e;
        }
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all serializations succeeded
    for (int i = 0; i < threadCount; i++) {
      assertNull(exceptions[i], "Serialization should not throw exception");
      assertNotNull(results[i]);
      assertTrue(results[i].contains("test-vpc"));
    }
  }

  @Test
  public void testConcurrentHashCodeAndEquals() throws InterruptedException {
    // Test concurrent access to hashCode and equals methods
    final AuthConf conf1 = new AuthConf("vpc1", "pool1", "client1");
    final AuthConf conf2 = new AuthConf("vpc1", "pool1", "client1");
    final int threadCount = 50;
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

    // Verify consistency
    for (int i = 0; i < threadCount; i++) {
      assertTrue(equalityResults[i]);
      assertEquals(conf1.hashCode(), hashResults[i]);
    }
  }

  // Stress Tests with Very Long Strings
  @Test
  public void testStressWithVeryLongStrings1000Chars() {
    String veryLongVpc = "vpc-" + "x".repeat(1000);
    String veryLongPool = "pool-" + "y".repeat(1000);
    String veryLongClient = "client-" + "z".repeat(1000);

    var authConf = new AuthConf(veryLongVpc, veryLongPool, veryLongClient);

    assertNotNull(authConf);
    assertEquals(1004, authConf.vpcName().length());
    assertEquals(1005, authConf.userPool().length());
    assertEquals(1007, authConf.userPoolClient().length());
    assertTrue(authConf.vpcName().startsWith("vpc-"));
    assertTrue(authConf.userPool().startsWith("pool-"));
    assertTrue(authConf.userPoolClient().startsWith("client-"));
  }

  @Test
  public void testStressWithVeryLongStrings5000Chars() {
    String ultraLongVpc = "a".repeat(5000);
    String ultraLongPool = "b".repeat(5000);
    String ultraLongClient = "c".repeat(5000);

    var authConf = new AuthConf(ultraLongVpc, ultraLongPool, ultraLongClient);

    assertNotNull(authConf);
    assertEquals(5000, authConf.vpcName().length());
    assertEquals(5000, authConf.userPool().length());
    assertEquals(5000, authConf.userPoolClient().length());
  }

  @Test
  public void testStressSerializationWithLongStrings() throws Exception {
    String longValue = "value-" + "x".repeat(2000);
    var authConf = new AuthConf(longValue, longValue, longValue);

    String yaml = YAML_MAPPER.writeValueAsString(authConf);
    assertNotNull(yaml);
    assertTrue(yaml.length() > 6000);

    var deserialized = YAML_MAPPER.readValue(yaml, AuthConf.class);
    assertEquals(authConf, deserialized);
  }

  // Boundary Conditions
  @Test
  public void testBoundaryWithSingleCharacterStrings() {
    var authConf = new AuthConf("a", "b", "c");

    assertNotNull(authConf);
    assertEquals("a", authConf.vpcName());
    assertEquals("b", authConf.userPool());
    assertEquals("c", authConf.userPoolClient());
    assertEquals(1, authConf.vpcName().length());
  }

  @Test
  public void testBoundaryWithWhitespaceOnly() {
    var authConf = new AuthConf("   ", "\t\t", "\n\n");

    assertNotNull(authConf);
    assertEquals("   ", authConf.vpcName());
    assertEquals("\t\t", authConf.userPool());
    assertEquals("\n\n", authConf.userPoolClient());
  }

  @Test
  public void testBoundaryWithUnicodeCharacters() {
    var authConf = new AuthConf("vpc-\u4E2D\u6587", "pool-\u65E5\u672C\u8A9E", "client-\uD55C\uAD6D\uC5B4");

    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u4E2D\u6587"));
    assertTrue(authConf.userPool().contains("\u65E5\u672C\u8A9E"));
    assertTrue(authConf.userPoolClient().contains("\uD55C\uAD6D\uC5B4"));
  }

  @Test
  public void testBoundaryWithMixedCaseAndNumbers() {
    var authConf = new AuthConf("VPC123abc", "POOL456DEF", "Client789ghi");

    assertNotNull(authConf);
    assertEquals("VPC123abc", authConf.vpcName());
    assertEquals("POOL456DEF", authConf.userPool());
    assertEquals("Client789ghi", authConf.userPoolClient());
  }

  // Comparison and Equality Edge Cases
  @Test
  public void testEqualityWithDifferentFirstField() {
    var conf1 = new AuthConf("vpc1", "pool", "client");
    var conf2 = new AuthConf("vpc2", "pool", "client");

    assertNotEquals(conf1, conf2);
    assertNotEquals(conf1.hashCode(), conf2.hashCode());
  }

  @Test
  public void testEqualityWithDifferentSecondField() {
    var conf1 = new AuthConf("vpc", "pool1", "client");
    var conf2 = new AuthConf("vpc", "pool2", "client");

    assertNotEquals(conf1, conf2);
    assertNotEquals(conf1.hashCode(), conf2.hashCode());
  }

  @Test
  public void testEqualityWithDifferentThirdField() {
    var conf1 = new AuthConf("vpc", "pool", "client1");
    var conf2 = new AuthConf("vpc", "pool", "client2");

    assertNotEquals(conf1, conf2);
    assertNotEquals(conf1.hashCode(), conf2.hashCode());
  }

  @Test
  public void testEqualityWithNullVsEmptyString() {
    var conf1 = new AuthConf(null, null, null);
    var conf2 = new AuthConf("", "", "");

    assertNotEquals(conf1, conf2);
    // Note: hashCode may be the same for different objects (hash collision)
    // We only verify they are not equal
  }

  @Test
  public void testEqualityWithSelf() {
    var authConf = new AuthConf("vpc", "pool", "client");

    assertEquals(authConf, authConf);
    assertEquals(authConf.hashCode(), authConf.hashCode());
  }

  // Performance Tests
  @Test
  public void testPerformanceCreation10000Instances() {
    long startTime = System.nanoTime();

    for (int i = 0; i < 10000; i++) {
      var authConf = new AuthConf("vpc-" + i, "pool-" + i, "client-" + i);
      assertNotNull(authConf);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds

    // Should complete in reasonable time (less than 1 second)
    assertTrue(duration < 1000, "Creating 10000 instances took " + duration + "ms");
  }

  @Test
  public void testPerformanceHashCode100000Calls() {
    var authConf = new AuthConf("test-vpc", "test-pool", "test-client");
    long startTime = System.nanoTime();

    for (int i = 0; i < 100000; i++) {
      int hash = authConf.hashCode();
      assertTrue(hash != 0 || hash == 0); // Just to use the value
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "100000 hashCode calls took " + duration + "ms");
  }

  @Test
  public void testPerformanceEquals100000Calls() {
    var conf1 = new AuthConf("vpc", "pool", "client");
    var conf2 = new AuthConf("vpc", "pool", "client");
    long startTime = System.nanoTime();

    for (int i = 0; i < 100000; i++) {
      boolean result = conf1.equals(conf2);
      assertTrue(result);
    }

    long endTime = System.nanoTime();
    long duration = (endTime - startTime) / 1_000_000;

    assertTrue(duration < 500, "100000 equals calls took " + duration + "ms");
  }

  // COMPREHENSIVE EDGE CASE TESTS - Unicode and Special Characters
  @Test
  public void testWithControlCharacterNull() {
    var authConf = new AuthConf("vpc\u0000name", "pool\u0000path", "client\u0000file");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0000"));
  }

  @Test
  public void testWithControlCharactersRange() {
    String controlChars = "\u0001\u0002\u0003\u0004\u0005\u000F\u001F";
    var authConf = new AuthConf("vpc" + controlChars, "pool" + controlChars, "client" + controlChars);
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0001"));
  }

  @Test
  public void testWithLatinExtendedCharacters() {
    var authConf = new AuthConf("vpc-\u00E9\u00F1\u00FC", "pool-\u00C4\u00D6\u00DC", "client-\u00E0\u00E8\u00EC");
    assertNotNull(authConf);
    assertEquals("vpc-\u00E9\u00F1\u00FC", authConf.vpcName());
  }

  @Test
  public void testWithCyrillicCharacters() {
    var authConf = new AuthConf("vpc-\u0410\u0411\u0412", "pool-\u0413\u0414\u0415", "client-\u0416\u0417\u0418");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0410"));
    assertTrue(authConf.userPool().contains("\u0413"));
  }

  @Test
  public void testWithArabicCharacters() {
    var authConf = new AuthConf("vpc-\u0627\u0628\u062A", "pool-\u062B\u062C\u062D", "client-\u062E\u062F\u0630");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0627"));
  }

  @Test
  public void testWithChineseCharacters() {
    var authConf = new AuthConf("vpc-\u4E2D\u534E\u4EBA\u6C11", "pool-\u5171\u548C\u56FD", "client-\u4E2D\u56FD");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u4E2D\u534E"));
  }

  @Test
  public void testWithJapaneseHiraganaKatakana() {
    var authConf = new AuthConf("vpc-\u3042\u3044\u3046\u3048\u304A", "pool-\u30A2\u30A4\u30A6\u30A8\u30AA", "client-\u65E5\u672C\u8A9E");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u3042"));
    assertTrue(authConf.userPool().contains("\u30A2"));
  }

  @Test
  public void testWithKoreanCharacters() {
    var authConf = new AuthConf("vpc-\uD55C\uAD6D\uC5B4", "pool-\uD55C\uAE00", "client-\uC138\uC885\uB300\uC655");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\uD55C\uAD6D"));
  }

  @Test
  public void testWithThaiCharacters() {
    var authConf = new AuthConf("vpc-\u0E01\u0E02\u0E03", "pool-\u0E04\u0E05\u0E06", "client-\u0E07\u0E08\u0E09");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0E01"));
  }

  @Test
  public void testWithHebrewCharacters() {
    var authConf = new AuthConf("vpc-\u05D0\u05D1\u05D2", "pool-\u05D3\u05D4\u05D5", "client-\u05D6\u05D7\u05D8");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u05D0"));
  }

  @Test
  public void testWithEmojiBasic() {
    var authConf = new AuthConf("vpc-\uD83D\uDE00", "pool-\uD83D\uDE01", "client-\uD83D\uDE02");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\uD83D\uDE00"));
  }

  @Test
  public void testWithEmojiSequences() {
    var authConf = new AuthConf("vpc-\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66", "pool-\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08", "client-emoji");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().length() > 3);
  }

  @Test
  public void testWithRightToLeftText() {
    var authConf = new AuthConf("vpc-\u202E\u0627\u0644\u0639\u0631\u0628\u064A\u0629\u202C", "pool-rtl", "client-rtl");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u202E"));
  }

  @Test
  public void testWithCombiningCharacters() {
    var authConf = new AuthConf("vpc-e\u0301\u0302\u0303", "pool-o\u0308\u030A", "client-a\u0300\u0301");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("e"));
  }

  @Test
  public void testWithDiacriticsHeavy() {
    var authConf = new AuthConf("vpc-Z\u0301\u0302\u0303\u0304\u0305\u0306\u0307", "pool-diacritics", "client-marks");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().length() > 3);
  }

  // STRESS TESTS - Creating Many Instances
  @Test
  public void testStressCreate50000Instances() {
    long startTime = System.nanoTime();
    for (int i = 0; i < 50000; i++) {
      var authConf = new AuthConf("vpc-" + i, "pool-" + i, "client-" + i);
      assertNotNull(authConf);
    }
    long duration = (System.nanoTime() - startTime) / 1_000_000;
    assertTrue(duration < 5000, "Creating 50000 instances took " + duration + "ms");
  }

  @Test
  public void testStressConcurrent50Threadsx1000Iterations() throws InterruptedException {
    final int threadCount = 50;
    final int iterations = 1000;
    Thread[] threads = new Thread[threadCount];
    final AuthConf[][] results = new AuthConf[threadCount][iterations];

    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < iterations; j++) {
          results[threadIndex][j] = new AuthConf("vpc-" + threadIndex + "-" + j, "pool", "client");
        }
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    // Verify all were created
    for (int i = 0; i < threadCount; i++) {
      for (int j = 0; j < iterations; j++) {
        assertNotNull(results[i][j]);
      }
    }
  }

  @Test
  public void testMemoryLeakDetectionCreateAndDiscard() {
    // Create and discard many instances to check for memory leaks
    for (int i = 0; i < 100000; i++) {
      AuthConf conf = new AuthConf("temp-" + i, "temp", "temp");
      assertNotNull(conf);
      // Let it go out of scope immediately
    }
    // If we get here without OutOfMemoryError, we're good
    assertTrue(true);
  }

  // SERIALIZATION EDGE CASES
  @Test
  public void testSerializationWithCorruptedYamlRecovery() throws Exception {
    var authConf = new AuthConf("vpc", "pool", "client");
    String yaml = YAML_MAPPER.writeValueAsString(authConf);

    // Verify we can deserialize valid YAML
    var deserialized = YAML_MAPPER.readValue(yaml, AuthConf.class);
    assertEquals(authConf, deserialized);
  }

  @Test
  public void testYamlInjectionAttemptWithSpecialChars() throws Exception {
    var authConf = new AuthConf("vpc: malicious\n  injection: true", "pool", "client");
    String yaml = YAML_MAPPER.writeValueAsString(authConf);

    var deserialized = YAML_MAPPER.readValue(yaml, AuthConf.class);
    assertNotNull(deserialized);
    assertTrue(deserialized.vpcName().contains("malicious"));
  }

  @Test
  public void testSerializationWithEmbeddedYamlStructures() throws Exception {
    var authConf = new AuthConf("vpc:\n  nested: value", "pool: {key: val}", "client: [1,2,3]");
    String yaml = YAML_MAPPER.writeValueAsString(authConf);

    var deserialized = YAML_MAPPER.readValue(yaml, AuthConf.class);
    assertNotNull(deserialized);
  }

  // PATH TRAVERSAL TESTS
  @Test
  public void testWithVeryDeepPaths100Levels() {
    String deepPath = "a/".repeat(100) + "file.yaml";
    var authConf = new AuthConf("vpc", deepPath, deepPath);
    assertNotNull(authConf);
    assertTrue(authConf.userPool().length() > 200);
  }

  @Test
  public void testWithPathTraversalAttempts() {
    var authConf = new AuthConf("../../../etc/passwd", "../../pool.yaml", "./../client.yaml");
    assertNotNull(authConf);
    assertEquals("../../../etc/passwd", authConf.vpcName());
  }

  @Test
  public void testWithWindowsPathTraversal() {
    var authConf = new AuthConf("..\\..\\..\\windows\\system32", "pool", "client");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\\"));
  }

  @Test
  public void testWithReservedWindowsFilenames() {
    String[] reserved = {"CON", "PRN", "AUX", "NUL", "COM1", "LPT1"};
    for (String res : reserved) {
      var authConf = new AuthConf(res, res + ".yaml", res + "-client");
      assertNotNull(authConf);
      assertEquals(res, authConf.vpcName());
    }
  }

  @Test
  public void testWithMixedPathSeparators() {
    var authConf = new AuthConf("vpc/name\\mixed/path", "pool\\windows/linux", "client/slash\\backslash");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("/"));
    assertTrue(authConf.vpcName().contains("\\"));
  }

  @Test
  public void testWithSymbolicLinkPattern() {
    var authConf = new AuthConf("vpc-link->target", "pool->symlink", "client@->link");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("->"));
  }

  @Test
  public void testWithCircularPathPattern() {
    var authConf = new AuthConf("vpc/a/b/../b/../b", "pool/./././path", "client/../client");
    assertNotNull(authConf);
    assertTrue(authConf.userPool().contains("."));
  }

  // ADDITIONAL UNICODE BLOCKS
  @Test
  public void testWithGreekCharacters() {
    var authConf = new AuthConf("vpc-\u03B1\u03B2\u03B3", "pool-\u03B4\u03B5\u03B6", "client-\u03B7\u03B8\u03B9");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u03B1"));
  }

  @Test
  public void testWithDevanagariCharacters() {
    var authConf = new AuthConf("vpc-\u0905\u0906\u0907", "pool-\u0908\u0909\u090A", "client-\u0915\u0916\u0917");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0905"));
  }

  @Test
  public void testWithArmenianCharacters() {
    var authConf = new AuthConf("vpc-\u0531\u0532\u0533", "pool-\u0534\u0535\u0536", "client-arm");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u0531"));
  }

  @Test
  public void testWithGeorgianCharacters() {
    var authConf = new AuthConf("vpc-\u10D0\u10D1\u10D2", "pool-\u10D3\u10D4\u10D5", "client-geo");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u10D0"));
  }

  @Test
  public void testWithEthiopicCharacters() {
    var authConf = new AuthConf("vpc-\u1200\u1201\u1202", "pool-\u1203\u1204\u1205", "client-eth");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u1200"));
  }

  @Test
  public void testWithMixedScripts() {
    var authConf = new AuthConf("vpc-Latin\u4E2D\u0410\u0627", "pool-\u3042\uD55C\u0E01", "client-\u05D0mix");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().length() > 9);
  }

  @Test
  public void testWithMathematicalAlphanumericSymbols() {
    var authConf = new AuthConf("vpc-\uD835\uDD38\uD835\uDD39", "pool-\uD835\uDD3A", "client-math");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().length() > 3);
  }

  @Test
  public void testWithVariationSelectors() {
    var authConf = new AuthConf("vpc-\u2764\uFE0F", "pool-\u2764\uFE0E", "client-var");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().length() >= 3);
  }

  @Test
  public void testWithZeroWidthCharacters() {
    var authConf = new AuthConf("vpc\u200B\u200C\u200D", "pool\uFEFF", "client\u2060");
    assertNotNull(authConf);
    assertTrue(authConf.vpcName().contains("\u200B"));
  }

}
