package com.transferwise.common.baseutils.meters.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;

public interface IMeterCache {

  boolean contains(String name, TagsSet tags);

  void clear();

  int size();

  Meter removeMeter(String name, TagsSet tags);

  Object removeMetersContainer(String name, TagsSet tags);

  /**
   * Allows to also cache Objects, which contain multiple meters.
   *
   * <p>Even when those can be cached by other means, it is convenient, that meterCache.clear() will also clear those.
   */
  <T> T metersContainer(String name, TagsSet tags, Supplier<T> collectionCreator);

  DistributionSummary summary(String name, TagsSet tags);

  DistributionSummary summary(String name, TagsSet tags, Supplier<DistributionSummary> supplier);

  Timer timer(String name, TagsSet tags);

  Timer timer(String name, TagsSet tags, Supplier<Timer> supplier);

  Counter counter(String name, TagsSet tags);

  Counter counter(String name, TagsSet tags, Supplier<Counter> supplier);

  MeterRegistry getMeterRegistry();
}
