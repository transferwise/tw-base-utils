package com.transferwise.common.baseutils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Ordering;
import com.transferwise.common.baseutils.clock.TestClock;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

class UuidUtilsTest extends BaseTest {

  @Test
  void defaultPrefixCombUuidIsGrowingOverTime() {
    TestClock clock = TestClock.createAndRegister();

    int n = 100;

    UUID[] uuids = new UUID[n];
    for (int i = 0; i < n; i++) {
      uuids[i] = UuidUtils.generatePrefixCombUuid();
      clock.tick(Duration.ofMillis(2));
    }

    long previousTime = -1;
    for (int i = 0; i < n; i++) {
      System.out.println("Testing " + uuids[i]);
      assertEquals(4, uuids[i].version());
      long time = uuids[i].getMostSignificantBits() >>> (64 - 38);

      if (previousTime != -1) {
        assertTrue(previousTime < time);
      }
      previousTime = time;
    }

    assertTrue(Ordering.natural().isOrdered(Arrays.asList(uuids)));
    assertEquals(n, Set.of(uuids).size());
  }

  @Test
  void timePrefixedUuidIsVersion7() {
    UUID uuid = UuidUtils.generateTimePrefixedUuid();
    assertEquals(7, uuid.version());
  }

  @Test
  void timePrefixedUuidIsGrowingOverTime() {
    TestClock clock = TestClock.createAndRegister();

    int n = 100;

    UUID[] uuids = new UUID[n];
    for (int i = 0; i < n; i++) {
      uuids[i] = UuidUtils.generateTimePrefixedUuid();
      clock.tick(Duration.ofMillis(2));
    }

    long previousTime = -1;
    for (int i = 0; i < n; i++) {
      System.out.println("Testing " + uuids[i]);
      long time = uuids[i].getMostSignificantBits() >>> (64 - 48);

      if (previousTime != -1) {
        assertTrue(previousTime < time);
      }
      previousTime = time;
    }

    assertTrue(Ordering.natural().isOrdered(Arrays.asList(uuids)));
    assertEquals(n, Set.of(uuids).size());
  }

  @Test
  void deterministicTimePrefixedUuidIsVersion8() {
    UUID uuid = UuidUtils.generateDeterministicTimePrefixedUuid(new byte[0]);
    System.out.println(uuid);
    assertEquals(8, uuid.version());
  }

  @Test
  void deterministicTimePrefixedUuidIsGrowingOverTime() {
    TestClock clock = TestClock.createAndRegister();

    int n = 100;

    UUID[] uuids = new UUID[n];
    for (int i = 0; i < n; i++) {
      uuids[i] = UuidUtils.generateDeterministicTimePrefixedUuid(new byte[0]);
      clock.tick(Duration.ofMillis(2));
    }

    long previousTime = -1;
    for (int i = 0; i < n; i++) {
      System.out.println("Testing " + uuids[i]);
      long time = uuids[i].getMostSignificantBits() >>> (64 - 48);

      if (previousTime != -1) {
        assertTrue(previousTime < time);
      }
      previousTime = time;
    }

    assertTrue(Ordering.natural().isOrdered(Arrays.asList(uuids)));
    assertEquals(n, Set.of(uuids).size());
  }

  @Test
  void deterministicTimePrefixedUuidIsFullyDeterministic() {
    long timestamp = System.currentTimeMillis();
    byte[] seed = "Ben was here".getBytes(StandardCharsets.UTF_8);
    UUID uuid1 = UuidUtils.generateDeterministicTimePrefixedUuid(timestamp, seed);
    UUID uuid2 = UuidUtils.generateDeterministicTimePrefixedUuid(timestamp, seed);
    assertEquals(uuid1, uuid2);
  }

  @Test
  void deterministicTimePrefixedUuidChangesWithDifferentData() {
    long timestamp = System.currentTimeMillis();
    UUID uuid1 = UuidUtils.generateDeterministicTimePrefixedUuid(timestamp, "Ben was here".getBytes(StandardCharsets.UTF_8));
    UUID uuid2 = UuidUtils.generateDeterministicTimePrefixedUuid(timestamp, "Ben was not here".getBytes(StandardCharsets.UTF_8));
    assertNotEquals(uuid1, uuid2);
  }

  @Test
  void constantCanBeAddedToUuid() {
    var uuid = UuidUtils.generateTimePrefixedUuid();
    var uuidWithConstant = UuidUtils.add(uuid, 11111);

    assertNotEquals(uuidWithConstant, uuid);

    assertThrows(NullPointerException.class, () -> UuidUtils.add(null, 0));
  }

  @Test
  void addingConstantDoesNotChangeOrdering() {
    TestClock clock = TestClock.createAndRegister();

    int n = 100;

    UUID[] uuids = new UUID[n];
    for (int i = 0; i < n; i++) {
      uuids[i] = UuidUtils.add(UuidUtils.generateTimePrefixedUuid(), ThreadLocalRandom.current().nextInt());
      clock.tick(Duration.ofMillis(2));
    }

    long previousTime = -1;
    for (int i = 0; i < n; i++) {
      long time = uuids[i].getMostSignificantBits() >>> (64 - 48);

      if (previousTime != -1) {
        assertTrue(previousTime < time);
      }
      previousTime = time;

      System.out.println(uuids[i]);
      assertEquals(7, uuids[i].version());
    }

    assertTrue(Ordering.natural().isOrdered(Arrays.asList(uuids)));
    assertEquals(n, Set.of(uuids).size());
  }

  @Test
  void convertingFromUuidAndBackToBytesEndWithTheSameResult() {
    UUID expected = UUID.randomUUID();

    byte[] bytes = UuidUtils.toBytes(expected);
    UUID result = UuidUtils.toUuid(bytes);

    assertEquals(expected, result);
  }

  @Test
  void convertingFromBytesAndBackToUuidEndWithTheSameResult() {
    byte[] expected = RandomUtils.nextBytes(16);

    UUID uuid = UuidUtils.toUuid(expected);
    byte[] result = UuidUtils.toBytes(uuid);

    assertArrayEquals(expected, result);
  }

  @Test
  void generatingSecuredUuidWorks() {
    assertNotNull(UuidUtils.generateSecureUuid());
  }

  @Test
  void deterministicPrefixCombUuidIsGrowingOverTimeWhilePreserveLestSignificantBits() {
    int n = 100;
    UUID uuid = UUID.randomUUID();
    UUID[] uuids = new UUID[n];
    Instant now = Instant.now();
    for (int i = 0; i < n; i++) {
      now = now.plus(Duration.ofMillis(2));
      uuids[i] = UuidUtils.generatePrefixCombUuid(now.toEpochMilli(), uuid);
    }

    long previousTime = -1;
    for (int i = 0; i < n; i++) {
      long time = uuids[i].getMostSignificantBits() >>> (64 - 38);

      if (previousTime != -1) {
        assertTrue(previousTime < time);
      }
      previousTime = time;

      System.out.println(uuids[i]);
      assertEquals(4, uuids[i].version());
      assertEquals(uuid.getLeastSignificantBits(), uuids[i].getLeastSignificantBits());
    }

    assertTrue(Ordering.natural().isOrdered(Arrays.asList(uuids)));
    assertEquals(n, Set.of(uuids).size());
  }

}
