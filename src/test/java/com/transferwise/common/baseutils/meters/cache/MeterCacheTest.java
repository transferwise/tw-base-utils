package com.transferwise.common.baseutils.meters.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MeterCacheTest {

  private MeterRegistry meterRegistry;
  private MeterCache meterCache;

  @BeforeEach
  public void setup() {
    meterRegistry = new SimpleMeterRegistry();
    meterCache = new MeterCache(meterRegistry);
  }

  @Test
  public void counterCacheWorksWithNoTags() {
    var counterName = "my.test.counter";
    var counter0 = meterCache.counter(counterName, TagsSet.empty());
    var counter1 = meterCache.counter(counterName, TagsSet.empty());
    var counter2 = meterRegistry.counter(counterName);

    assertThat(counter0).isSameAs(counter1);
    assertThat(counter0).isSameAs(counter2);

    meterCache.clear();
    assertThat(meterCache.size()).isEqualTo(0);

    var counter3 = meterCache.counter(counterName, TagsSet.empty());
    assertThat(counter0).isSameAs(counter3);

    // Important to understand, that clearing the meterRegistry does not clear the cache.
    meterRegistry.clear();
    var counter4 = meterCache.counter(counterName, TagsSet.empty());
    assertThat(counter0).isSameAs(counter4);

    meterRegistry.clear();
    meterCache.clear();

    var counter5 = meterCache.counter(counterName, TagsSet.empty());
    assertThat(counter0).isNotSameAs(counter5);
  }

  @Test
  public void counterCacheWorksWithTags() {
    var tag0 = Tag.of("tag0", "tagValue0");
    var tag1 = Tag.of("tag1", "tagValue1");
    var tag2 = Tag.of("tag2", "tagValue2");
    var counterName = "my.test.counter";

    var counter0 = meterCache.counter(counterName, TagsSet.of(tag0, tag1));
    var counter1 = meterCache.counter(counterName, TagsSet.of(tag0, tag1));

    assertThat(counter0).isSameAs(counter1);
    assertThat(meterCache.size()).isEqualTo(1);

    var counter2 = meterCache.counter(counterName, TagsSet.of(tag1, tag0));
    assertThat(counter0).as("Even when cache is different, the underlying meterRegistry provides same counter.").isSameAs(counter2);
    assertThat(meterCache.size()).isEqualTo(2);

    var counter3 = meterCache.counter(counterName, TagsSet.of(tag0, tag1, tag2));
    assertThat(counter0).isNotSameAs(counter3);
    assertThat(meterCache.size()).isEqualTo(3);

    var counter4 = meterCache.counter(counterName, TagsSet.of("tag0", "tagValue0", "tag1", "tagValue1"));
    assertThat(counter0).as("Even when cache is different, the underlying meterRegistry provides same counter.").isSameAs(counter4);
    assertThat(meterCache.size()).isEqualTo(4);

    var counter5 = meterCache.counter(counterName, TagsSet.of("tag0", "tagValue0", "tag1", "tagValue1"));
    assertThat(counter4).isSameAs(counter5);
    assertThat(meterCache.size()).isEqualTo(4);
  }

  @Test
  public void distributionSummaryCanBeUsed() {
    meterCache.summary("my.summary", TagsSet.of("a", "av")).record(10);
    assertThat(meterRegistry.summary("my.summary", "a", "av").count()).isEqualTo(1);
  }

  @Test
  public void timerCanBeUsed() {
    meterCache.timer("my.timer", TagsSet.of("a", "av")).record(Duration.ZERO);
    assertThat(meterRegistry.timer("my.timer", "a", "av").count()).isEqualTo(1);
  }

  @Test
  public void meterContainersCanBeUsed() {
    meterCache.metersContainer("name", TagsSet.of("a", "av"), () -> "Hello World");
    assertThat(meterCache.metersContainer("name", TagsSet.of("a", "av"), () -> "Hello World 1")).isEqualTo("Hello World");
    assertThat(meterCache.size()).isEqualTo(1);

    meterCache.clear();
    String metersContainer = meterCache.metersContainer("name", TagsSet.of("a", "av"), () -> "Hello World 1");
    assertThat(metersContainer).isEqualTo("Hello World 1");

    assertThat(meterCache.removeMetersContainer("name", TagsSet.of("a", "av"))).isSameAs(metersContainer);
    assertThat(meterCache.size()).isEqualTo(0);
  }

  @Test
  public void specificMeterCanBeRemovedFromCache() {
    Timer timer0 = meterCache.timer("my.timer.0", TagsSet.of("a", "av0"));
    meterCache.timer("my.timer.0", TagsSet.of("a", "av1"));
    assertThat(meterCache.size()).isEqualTo(2);
    assertThat(meterCache.removeMeter("my.timer.0", TagsSet.of("a", "av0"))).isSameAs(timer0);
    assertThat(meterCache.size()).isEqualTo(1);
    assertThat(meterCache.removeMeter("my.timer.0", TagsSet.of("a", "av0"))).isNull();

    assertThat(meterCache.timer("my.timer.0", TagsSet.of("a", "av0"))).as("Underlying meter is still present in meter registry").isSameAs(timer0);
  }

}
