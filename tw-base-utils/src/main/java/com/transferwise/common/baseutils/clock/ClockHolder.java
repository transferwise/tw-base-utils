package com.transferwise.common.baseutils.clock;

import java.time.Clock;

public class ClockHolder {

  private static Clock clock = Clock.systemUTC();

  public static Clock getClock() {
    return clock;
  }

  public static void setClock(Clock clock) {
    ClockHolder.clock = clock;
  }
}
