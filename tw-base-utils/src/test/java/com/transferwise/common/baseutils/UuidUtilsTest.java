package com.transferwise.common.baseutils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Ordering;
import com.transferwise.common.baseutils.clock.TestClock;
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
      long time = uuids[i].getMostSignificantBits() >>> (64 - 38);

      if (previousTime != -1) {
        assertTrue(previousTime < time);
      }
      previousTime = time;

      System.out.println(uuids[i]);
      assertEquals(4, uuids[i].version());
    }

    assertTrue(Ordering.natural().isOrdered(Arrays.asList(uuids)));
    assertEquals(n, Set.of(uuids).size());
  }

  @Test
  void constantCanBeAddedToUuid() {
    var uuid = UuidUtils.generatePrefixCombUuid();
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
      uuids[i] = UuidUtils.add(UuidUtils.generatePrefixCombUuid(), ThreadLocalRandom.current().nextInt());
      clock.tick(Duration.ofMillis(2));
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
