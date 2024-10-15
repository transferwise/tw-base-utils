package com.transferwise.common.baseutils.clock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class TestClockTest {

  // Plus operation is used in Groovy based tests.
  @Test
  void plusOperationWorks() {
    var testClock = new TestClock();
    var startInstant = testClock.instant();

    testClock.plus("P1D");

    assertThat(testClock.instant(), equalTo(startInstant.plus(Duration.ofDays(1))));
  }
}
