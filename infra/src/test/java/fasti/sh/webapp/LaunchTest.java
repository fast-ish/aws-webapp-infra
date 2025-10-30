package fasti.sh.webapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for Launch class - focused on arnToBucketName execution tests.
 */
public class LaunchTest {

  // ============================================================
  // CORE EXECUTION TESTS
  // ============================================================

  @Test
  public void testArnToBucketNameWithValidArn() {
    String arn = "arn:aws:s3:::my-bucket-name";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket-name", result);
  }

  @Test
  public void testArnToBucketNameWithValidArnAndPath() {
    String arn = "arn:aws:s3:::my-bucket/path/to/object";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket", result);
  }

  @Test
  public void testArnToBucketNameWithValidArnAndLeadingSlash() {
    String arn = "arn:aws:s3:::/my-bucket-name";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket-name", result);
  }

  @Test
  public void testArnToBucketNameWithInvalidArn() {
    String arn = "arn:aws:ec2:us-east-1:123456789012:instance/i-1234567890abcdef0";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithNull() {
    String result = Launch.arnToBucketName(null);
    assertNull(result);
  }

  // ============================================================
  // EDGE CASE TESTS
  // ============================================================

  @Test
  public void testArnToBucketNameWithEmptyString() {
    String result = Launch.arnToBucketName("");
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithComplexPath() {
    String arn = "arn:aws:s3:::my-bucket/path/to/deeply/nested/object.txt";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket", result);
  }

  @Test
  public void testArnToBucketNameWithDotsInBucket() {
    String arn = "arn:aws:s3:::my.bucket.name.com";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my.bucket.name.com", result);
  }

  @Test
  public void testArnToBucketNameWithHyphens() {
    String arn = "arn:aws:s3:::my-bucket-with-hyphens-123";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket-with-hyphens-123", result);
  }

  @Test
  public void testArnToBucketNameWithMultipleSlashes() {
    String arn = "arn:aws:s3:::my-bucket///multiple///slashes";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket", result);
  }

  @Test
  public void testArnToBucketNameWithTrailingSlash() {
    String arn = "arn:aws:s3:::my-bucket/";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my-bucket", result);
  }

  @Test
  public void testArnToBucketNameWithNumericBucket() {
    String arn = "arn:aws:s3:::123456789012";
    String result = Launch.arnToBucketName(arn);
    assertEquals("123456789012", result);
  }

  @Test
  public void testArnToBucketNameCasePreservation() {
    String arn = "arn:aws:s3:::MyBucketName-WithCase";
    String result = Launch.arnToBucketName(arn);
    assertEquals("MyBucketName-WithCase", result);
  }

  @Test
  public void testArnToBucketNameWithMinimalBucket() {
    String arn = "arn:aws:s3:::a";
    String result = Launch.arnToBucketName(arn);
    assertEquals("a", result);
  }

  @Test
  public void testArnToBucketNameWithMaxLengthBucket() {
    String longBucket = "a".repeat(63);
    String arn = "arn:aws:s3:::" + longBucket;
    String result = Launch.arnToBucketName(arn);
    assertEquals(longBucket, result);
  }

  @Test
  public void testArnToBucketNameWithAwsCnPartition() {
    String arn = "arn:aws-cn:s3:::china-bucket";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithAwsGovPartition() {
    String arn = "arn:aws-us-gov:s3:::gov-bucket";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithDifferentService() {
    String arn = "arn:aws:dynamodb:us-east-1:123456789012:table/MyTable";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameConsistency() {
    String arn = "arn:aws:s3:::consistency-test-bucket/path";
    String result1 = Launch.arnToBucketName(arn);
    String result2 = Launch.arnToBucketName(arn);
    assertEquals(result1, result2);
  }

  @Test
  public void testArnToBucketNameWithWhitespace() {
    String arn = "   arn:aws:s3:::bucket   ";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithUnderscore() {
    String arn = "arn:aws:s3:::bucket_with_underscore";
    String result = Launch.arnToBucketName(arn);
    assertEquals("bucket_with_underscore", result);
  }

  @Test
  public void testArnToBucketNameWithMixedSpecialChars() {
    String arn = "arn:aws:s3:::my.bucket-name_2024";
    String result = Launch.arnToBucketName(arn);
    assertEquals("my.bucket-name_2024", result);
  }

  @Test
  public void testArnToBucketNameWithS3ObjectLambdaArn() {
    String arn = "arn:aws:s3-object-lambda:us-east-1:123456789012:accesspoint/my-ap";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithS3OutpostsArn() {
    String arn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost/op-123/bucket/my-bucket";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithAccessPointArn() {
    String arn = "arn:aws:s3:us-east-1:123456789012:accesspoint/my-access-point";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithMalformedArnMissingParts() {
    String arn = "arn:aws:s3";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithOnlyColons() {
    String arn = "::::";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithIamArn() {
    String arn = "arn:aws:iam::123456789012:role/MyRole";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }

  @Test
  public void testArnToBucketNameWithLambdaArn() {
    String arn = "arn:aws:lambda:us-east-1:123456789012:function:MyFunction";
    String result = Launch.arnToBucketName(arn);
    assertNull(result);
  }
}
