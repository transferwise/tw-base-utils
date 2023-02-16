package com.transferwise.common.baseutils.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ThreadNamingScheduledExecutorServiceWrapper extends ThreadNamingExecutorServiceWrapper implements ScheduledExecutorService {

  private ScheduledExecutorService delegate;

  public ThreadNamingScheduledExecutorServiceWrapper(String threadName, ScheduledExecutorService delegate) {
    super(threadName, delegate);
    this.delegate = delegate;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return delegate.schedule(wrap(command), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return delegate.schedule(wrap(callable), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return delegate.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return delegate.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
  }
}
