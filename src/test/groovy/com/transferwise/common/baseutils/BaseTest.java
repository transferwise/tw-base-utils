package com.transferwise.common.baseutils;

import com.transferwise.common.baseutils.clock.TestClock;
import org.junit.jupiter.api.AfterEach;

public class BaseTest {

  @AfterEach
  public void baseTestTearDown() {
    TestClock.reset();
  }
}
