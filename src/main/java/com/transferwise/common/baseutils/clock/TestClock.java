package com.transferwise.common.baseutils.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock {

  private Instant instant;
  private ZoneId zoneId;

  public static void reset() {
    ClockHolder.setClock(Clock.systemDefaultZone());
  }

  public static TestClock createAndRegister() {
    return createAndRegister(Instant.now(ClockHolder.getClock()));
  }

  public static TestClock createAndRegister(Instant instant) {
    return createAndRegister(instant, ZoneId.systemDefault());
  }

  public static TestClock createAndRegister(Instant instant, ZoneId zoneId) {
    TestClock testClock = new TestClock(instant, zoneId);
    ClockHolder.setClock(testClock);
    return testClock;
  }

  public TestClock() {
    this(Instant.now(ClockHolder.getClock()));
  }

  public TestClock(Instant instant) {
    this(instant, ZoneId.systemDefault());
  }

  public TestClock(Instant instant, ZoneId zoneId) {
    this.instant = instant;
    this.zoneId = zoneId;
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Clock withZone(ZoneId zoneId) {
    return new TestClock(instant, zoneId);
  }

  @Override
  public Instant instant() {
    return instant;
  }

  public void set(Instant instant) {
    this.instant = instant;
  }

  public void tick(Duration duration) {
    instant = instant.plus(duration);
  }
}
