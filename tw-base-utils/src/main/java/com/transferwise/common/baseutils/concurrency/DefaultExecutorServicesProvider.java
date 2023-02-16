package com.transferwise.common.baseutils.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class DefaultExecutorServicesProvider implements IExecutorServicesProvider {

  private ExecutorService executorService;
  private ScheduledTaskExecutor scheduledTaskExecutor;

  private boolean initialized = false;

  @PostConstruct
  public synchronized void init() {
    if (!initialized) {
      executorService = Executors.newCachedThreadPool(new CountingThreadFactory("tw-base"));
      scheduledTaskExecutor = new SimpleScheduledTaskExecutor(null, executorService);
      scheduledTaskExecutor.start();
      initialized = true;
    }
  }

  @PreDestroy
  public void destroy() {
    scheduledTaskExecutor.stop();
    executorService.shutdownNow();
  }

  @Override
  public ExecutorService getGlobalExecutorService() {
    return executorService;
  }

  @Override
  public ScheduledTaskExecutor getGlobalScheduledTaskExecutor() {
    return scheduledTaskExecutor;
  }
}
