package com.transferwise.common.baseutils.meters.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Pair;

public class MeterCache implements IMeterCache {

  private final Map<Pair<String, TagsSet>, Meter> metersMap = new ConcurrentHashMap<>();

  private final Map<Pair<String, TagsSet>, Object> metersContainersMap = new ConcurrentHashMap<>();

  /*
     Following 2 are meant to avoid creating linkToTargetMethod instances on every lambda invocation.
   */
  private Function<Pair<String, TagsSet>, Counter> counterCreaterFunction;
  private Function<Pair<String, TagsSet>, Timer> timerCreaterFunction;

  private final MeterRegistry meterRegistry;

  public MeterCache(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.counterCreaterFunction =  k -> meterRegistry.counter(k.getKey(), k.getValue().getMicrometerTags());
    this.timerCreaterFunction =  k -> meterRegistry.timer(k.getKey(), k.getValue().getMicrometerTags());
  }

  @Override
  public boolean contains(String name, TagsSet tags) {
    return metersMap.containsKey(Pair.of(name, tags));
  }

  @Override
  public void clear() {
    metersMap.clear();
    metersContainersMap.clear();
  }

  @Override
  public int size() {
    return metersMap.size() + metersContainersMap.size();
  }

  @Override
  public Meter removeMeter(String name, TagsSet tags) {
    return metersMap.remove(Pair.of(name, tags));
  }

  @Override
  public Object removeMetersContainer(String name, TagsSet tags) {
    return metersContainersMap.remove(Pair.of(name, tags));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T metersContainer(String name, TagsSet tags, Supplier<T> containerCreator) {
    return (T) metersContainersMap.computeIfAbsent(Pair.of(name, tags), k -> containerCreator.get());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T metersContainer(String name, TagsSet tags, MeterContainerCreator<T> meterContainerCreator) {
    return (T) metersContainersMap.computeIfAbsent(Pair.of(name, tags), k -> meterContainerCreator.create(k.getKey(), k.getValue()));
  }

  @Override
  public DistributionSummary summary(String name, TagsSet tags) {
    return (DistributionSummary) metersMap.computeIfAbsent(Pair.of(name, tags),
        k -> meterRegistry.summary(k.getKey(), k.getValue().getMicrometerTags()));
  }

  @Override
  @Deprecated(forRemoval = true)
  public DistributionSummary summary(String name, TagsSet tags, Supplier<DistributionSummary> metricCreator) {
    return (DistributionSummary) metersMap.computeIfAbsent(Pair.of(name, tags), k -> metricCreator.get());
  }

  @Override
  public DistributionSummary summary(String name, TagsSet tags, MeterCreator<DistributionSummary> meterCreator){
    return (DistributionSummary) metersMap.computeIfAbsent(Pair.of(name, tags), k->meterCreator.create(k.getKey(), k.getValue()));
  }

  @Override
  public Timer timer(String name, TagsSet tags) {
    return (Timer) metersMap.computeIfAbsent(Pair.of(name, tags), timerCreaterFunction);
  }

  @Override
  @Deprecated(forRemoval = true)
  public Timer timer(String name, TagsSet tags, Supplier<Timer> metricCreator) {
    return (Timer) metersMap.computeIfAbsent(Pair.of(name, tags), k -> metricCreator.get());
  }

  @Override
  public Timer timer(String name, TagsSet tags, MeterCreator<Timer> meterCreator) {
    return (Timer)metersMap.computeIfAbsent(Pair.of(name, tags), k-> meterCreator.create(k.getKey(), k.getValue()));
  }

  @Override
  public Counter counter(String name, TagsSet tagsSet) {
    return (Counter) metersMap.computeIfAbsent(Pair.of(name, tagsSet), counterCreaterFunction);
  }

  @Override
  @Deprecated(forRemoval = true)
  public Counter counter(String name, TagsSet tags, Supplier<Counter> metricCreator) {
    return (Counter) metersMap.computeIfAbsent(Pair.of(name, tags), k -> metricCreator.get());
  }

  @Override
  public Counter counter(String name, TagsSet tags, MeterCreator<Counter> meterCreator) {
    return (Counter)metersMap.computeIfAbsent(Pair.of(name, tags), k-> meterCreator.create(k.getKey(), k.getValue()));
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }
}
