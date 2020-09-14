package com.transferwise.common.baseutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.transferwise.common.baseutils.clock.TestClock;
import java.time.Duration;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

public class UuidUtilsTest extends BaseTest {

  @Test
  public void defaultPrefixCombUuidIsGrowingOverTime() {
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
        assertThat(previousTime).isLessThan(time);
      }
      previousTime = time;
    }

    assertThat(uuids).isSorted();
    assertThat(Sets.newTreeSet(uuids).size()).isEqualTo(n);
  }

  @Test
  public void convertingFromUuidAndBackToBytesEndWithTheSameResult() {
    UUID expected = UUID.randomUUID();

    byte[] bytes = UuidUtils.toBytes(expected);
    UUID result = UuidUtils.toUuid(bytes);

    assertEquals(expected, result);
  }

  @Test
  public void convertingFromBytesAndBackToUuidEndWithTheSameResult() {
    byte[] expected = RandomUtils.nextBytes(16);

    UUID uuid = UuidUtils.toUuid(expected);
    byte[] result = UuidUtils.toBytes(uuid);

    assertArrayEquals(expected, result);
  }

}
