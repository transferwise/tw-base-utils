package com.transferwise.common.baseutils.meters.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeterCacheTest {

  private MeterRegistry meterRegistry;
  private MeterCache meterCache;

  @BeforeEach
  public void setup() {
    meterRegistry = new SimpleMeterRegistry();
    meterCache = new MeterCache(meterRegistry);
  }

  @Test
  void counterCacheWorksWithNoTags() {
    var counterName = "my.test.counter";
    var counter0 = meterCache.counter(counterName, TagsSet.empty());
    var counter1 = meterCache.counter(counterName, TagsSet.empty());
    var counter2 = meterRegistry.counter(counterName);

    assertSame(counter0, counter1);
    assertSame(counter0, counter2);

    meterCache.clear();
    assertEquals(0, meterCache.size());

    var counter3 = meterCache.counter(counterName, TagsSet.empty());
    assertSame(counter0, counter3);

    // Important to understand, that clearing the meterRegistry does not clear the cache.
    meterRegistry.clear();
    var counter4 = meterCache.counter(counterName, TagsSet.empty());
    assertSame(counter0, counter4);

    meterRegistry.clear();
    meterCache.clear();

    var counter5 = meterCache.counter(counterName, TagsSet.empty());
    assertNotSame(counter0, counter5);
  }

  @Test
  void counterCacheWorksWithTags() {
    final var tag0 = Tag.of("tag0", "tagValue0");
    final var tag1 = Tag.of("tag1", "tagValue1");
    final var tag2 = Tag.of("tag2", "tagValue2");
    final var counterName = "my.test.counter";

    var counter0 = meterCache.counter(counterName, TagsSet.of(tag0, tag1));
    var counter1 = meterCache.counter(counterName, TagsSet.of(tag0, tag1));

    assertSame(counter0, counter1);
    assertEquals(1, meterCache.size());

    var counter2 = meterCache.counter(counterName, TagsSet.of(tag1, tag0));
    //Even when cache is different, the underlying meterRegistry provides same counter.
    assertSame(counter0, counter2);
    assertEquals(2, meterCache.size());

    var counter3 = meterCache.counter(counterName, TagsSet.of(tag0, tag1, tag2));
    assertNotSame(counter0, counter3);
    assertEquals(3, meterCache.size());

    var counter4 = meterCache.counter(counterName, TagsSet.of("tag0", "tagValue0", "tag1", "tagValue1"));
    //Even when cache is different, the underlying meterRegistry provides same counter.
    assertSame(counter0, counter4);
    assertEquals(4, meterCache.size());

    var counter5 = meterCache.counter(counterName, TagsSet.of("tag0", "tagValue0", "tag1", "tagValue1"));
    assertSame(counter4, counter5);
    assertEquals(4, meterCache.size());
  }

  @Test
  void distributionSummaryCanBeUsed() {
    meterCache.summary("my.summary", TagsSet.of("a", "av")).record(10);
    assertEquals(1, meterRegistry.summary("my.summary", "a", "av").count());
  }

  @Test
  void timerCanBeUsed() {
    meterCache.timer("my.timer", TagsSet.of("a", "av")).record(Duration.ZERO);
    assertEquals(1, meterRegistry.timer("my.timer", "a", "av").count());
  }

  @Test
  void meterContainersCanBeUsed() {
    meterCache.metersContainer("name", TagsSet.of("a", "av"), () -> "Hello World");
    assertEquals("Hello World", meterCache.metersContainer("name", TagsSet.of("a", "av"), () -> "Hello World 1"));
    assertEquals(1, meterCache.size());

    meterCache.clear();
    String metersContainer = meterCache.metersContainer("name", TagsSet.of("a", "av"), () -> "Hello World 1");
    assertEquals("Hello World 1", metersContainer);

    assertSame(meterCache.removeMetersContainer("name", TagsSet.of("a", "av")), metersContainer);
    assertEquals(0, meterCache.size());
  }

  @Test
  void specificMeterCanBeRemovedFromCache() {
    Timer timer0 = meterCache.timer("my.timer.0", TagsSet.of("a", "av0"));
    meterCache.timer("my.timer.0", TagsSet.of("a", "av1"));
    assertEquals(2, meterCache.size());
    assertSame(meterCache.removeMeter("my.timer.0", TagsSet.of("a", "av0")), timer0);
    assertEquals(1, meterCache.size());
    assertNull(meterCache.removeMeter("my.timer.0", TagsSet.of("a", "av0")));

    //Underlying meter is still present in meter registry
    assertSame(meterCache.timer("my.timer.0", TagsSet.of("a", "av0")), timer0);
  }

}
