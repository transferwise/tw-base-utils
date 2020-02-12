package com.transferwise.common.baseutils.concurrency;

import com.transferwise.common.baseutils.clock.ClockHolder;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors(chain = true)
public class DiscardingQueueProcessor<T, K> {

  private static final int SMALL_TIME_INTERVAL_S = 5;

  /**
   * If returns `true`, the soft limit will be applied.
   */
  @Setter
  private Predicate<T> softLimitPredicate;
  /**
   * Transforms `T` to `K`, before element `K` is added to queue.
   */
  @Setter
  private Function<T, K> dataTransformer;
  @Setter
  private ExecutorService executorService;
  @Setter
  private Consumer<Payload<K>> processor;
  @Setter
  private Consumer<Throwable> errorProcessor;
  @Setter
  private int maxConcurrency = Runtime.getRuntime().availableProcessors();
  @Setter
  @SuppressWarnings("checkstyle:MagicNumber")
  private int hardQueueLimit = 2000;
  @Setter
  @SuppressWarnings("checkstyle:MagicNumber")
  private int softQueueLimit = 500;
  @Setter
  private Duration queueTimeout;
  @Setter
  private Consumer<Payload<K>> timeoutsHander;

  private ConcurrentLinkedQueue<Payload<K>> queue = new ConcurrentLinkedQueue<>();
  private AtomicInteger queueSize = new AtomicInteger(0);
  private AtomicInteger concurrency = new AtomicInteger(0);

  private AtomicBoolean stopRequested = new AtomicBoolean();
  private AtomicBoolean started = new AtomicBoolean();

  private Lock genericLock = new ReentrantLock();
  private Condition genericCondition = genericLock.newCondition();
  private Runnable onStop;

  public DiscardingQueueProcessor(ExecutorService executorService, Consumer<Payload<K>> processor) {
    this.executorService = executorService;
    this.processor = processor;
  }

  @SuppressWarnings("unchecked")
  public ScheduleResult schedule(T data) {
    genericLock.lock();
    try {
      if (queueSize.get() >= hardQueueLimit) {
        return new ScheduleResult().setScheduled(false).setDiscardReason(DiscardReason.HARD_LIMIT);
      } else if (queueSize.get() >= softQueueLimit) {
        if (softLimitPredicate != null && Boolean.TRUE.equals(softLimitPredicate.test(data))) {
          return new ScheduleResult().setScheduled(false).setDiscardReason(DiscardReason.SOFT_LIMIT);
        }
      }

      K transformedData;
      if (dataTransformer != null) {
        transformedData = dataTransformer.apply(data);
      } else {
        transformedData = (K) data;
      }

      queueSize.incrementAndGet();
      Payload<K> payload = new Payload<>();
      payload.setData(transformedData);
      queue.add(payload);
      genericCondition.signalAll();

      return new ScheduleResult().setScheduled(true);
    } finally {
      genericLock.unlock();
    }
  }

  public void start() {
    if (!started.compareAndSet(false, true)) {
      throw new IllegalStateException("Can not start. Already started.");
    }
    stopRequested.set(false);
    executorService.submit(() -> {
      AtomicBoolean shouldStop = new AtomicBoolean();
      while (!shouldStop.get()) {
        genericLock.lock();
        try {
          while (queue.peek() == null && !stopRequested.get()) {
            boolean ignored = genericCondition.await(SMALL_TIME_INTERVAL_S, TimeUnit.SECONDS);
          }

          Payload<K> payload = queue.poll();

          if (payload == null && stopRequested.get()) {
            shouldStop.set(true);
            return;
          }
          while (concurrency.get() >= maxConcurrency) {
            boolean ignored = genericCondition.await(SMALL_TIME_INTERVAL_S, TimeUnit.SECONDS);
          }

          concurrency.incrementAndGet();
          executorService.submit(() -> {
            try {
              if (queueTimeout != null && ClockHolder.getClock().millis() - payload.getSchedulingTimeMillis() > queueTimeout.toMillis()) {
                if (timeoutsHander != null) {
                  timeoutsHander.accept(payload);
                }
              } else {
                processor.accept(payload);
              }
            } catch (Throwable t) {
              onErrorRaw(t);
            }

            genericLock.lock();
            try {
              queueSize.decrementAndGet();
              concurrency.decrementAndGet();
              genericCondition.signalAll();
            } finally {
              genericLock.unlock();
            }
          });
        } catch (Throwable t) {
          onErrorRaw(t);
        } finally {
          genericLock.unlock();
        }
      }
      if (onStop != null) {
        try {
          onStop.run();
        } catch (Throwable t) {
          onErrorRaw(t);
        }
      }
      started.set(false);
    });
  }

  protected void onErrorRaw(Throwable t) {
    if (t instanceof UndeclaredThrowableException) {
      onError(((UndeclaredThrowableException) t).getUndeclaredThrowable());
    } else {
      onError(t);
    }
  }

  protected void onError(Throwable t) {
    if (errorProcessor == null) {
      log.error(t.getMessage(), t);
    } else {
      errorProcessor.accept(t);
    }
  }

  public void stop(Runnable onStop) {
    genericLock.lock();
    try {
      this.onStop = onStop;
      stop();
    } finally {
      genericLock.unlock();
    }
  }

  public void stop() {
    genericLock.lock();
    try {
      stopRequested.set(true);
      genericCondition.signalAll();
    } finally {
      genericLock.unlock();
    }
  }

  public boolean hasStopped() {
    genericLock.lock();
    try {
      return stopRequested.get() && queueSize.get() == 0;
    } finally {
      genericLock.unlock();
    }
  }

  public int getQueueSize() {
    return queueSize.get();
  }

  public int getConcurrency() {
    return concurrency.get();
  }

  @Data
  @Accessors(chain = true)
  public class Payload<T> {

    private T data;
    private long schedulingTimeMillis = ClockHolder.getClock().millis();
  }

  public enum DiscardReason {
    HARD_LIMIT,
    SOFT_LIMIT
  }

  @Data
  @Accessors(chain = true)
  public static class ScheduleResult {

    private boolean scheduled;
    private DiscardReason discardReason;
  }

}
