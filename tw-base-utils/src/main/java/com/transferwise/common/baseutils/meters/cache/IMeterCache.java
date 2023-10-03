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
   *
   * @deprecated in a favor of {@link #metersContainer(String, TagsSet, MeterContainerCreator)}.
   */
  @Deprecated(forRemoval = true)
  <T> T metersContainer(String name, TagsSet tags, Supplier<T> collectionCreator);

  <T> T metersContainer(String name, TagsSet tags, MeterContainerCreator<T> meterContainerCreator);

  DistributionSummary summary(String name, TagsSet tags);

  /**
   * Returns a DistributionSummary.
   *
   * @deprecated in a favor of {@link #summary(String, TagsSet, MeterCreator)} .
   */
  @Deprecated(forRemoval = true)
  DistributionSummary summary(String name, TagsSet tags, Supplier<DistributionSummary> supplier);

  DistributionSummary summary(String name, TagsSet tags, MeterCreator<DistributionSummary> meterCreator);

  Timer timer(String name, TagsSet tags);

  /**
   * Returns a counter.
   *
   * @deprecated in a favor of {@link #timer(String, TagsSet, MeterCreator)} .
   */
  @Deprecated(forRemoval = true)
  Timer timer(String name, TagsSet tags, Supplier<Timer> supplier);

  Timer timer(String name, TagsSet tags, MeterCreator<Timer> meterCreator);


  Counter counter(String name, TagsSet tags);

  /**
   * Returns a counter.
   *
   * @deprecated in a favor of {@link #counter(String, TagsSet, MeterCreator)} .
   */
  @Deprecated(forRemoval = true)
  Counter counter(String name, TagsSet tags, Supplier<Counter> supplier);

  Counter counter(String name, TagsSet tags, MeterCreator<Counter> meterCreator);

  MeterRegistry getMeterRegistry();

  interface MeterCreator<M extends Meter>{
    M create(String name, TagsSet tags);
  }

  interface MeterContainerCreator<T> {
    T create(String name, TagsSet tags);
  }
}
