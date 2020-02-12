package com.transferwise.common.baseutils.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ClockUtils {

  public static void setFrom(ZonedDateTime time) {
    ClockHolder.setClock(Clock.fixed(time.toInstant(), time.getZone()));
  }

  public static void setOffset(Duration duration) {
    ClockHolder.setClock(Clock.offset(ClockHolder.getClock(), duration));
  }

  public static void setFromDate(int year, int month, int day, ZoneId zoneId) {
    setFrom(ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zoneId));
  }

  public static void setFromDate(int year, int month, int day) {
    setFromDate(year, month, day, ZoneId.systemDefault());
  }

  public static void setFromUtcDate(int year, int month, int day) {
    setFromDate(year, month, day, ZoneOffset.UTC);
  }
}
