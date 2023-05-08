package com.transferwise.common.baseutils.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.InitializingBean;

public class DefaultExecutorServicesProvider implements IExecutorServicesProvider, InitializingBean {

  private ExecutorService executorService;
  private ScheduledTaskExecutor scheduledTaskExecutor;

  private boolean initialized = false;

  private Lock initializationLock = new ReentrantLock();

  public void afterPropertiesSet() {
    initializationLock.lock();
    try {
      if (!initialized) {
        executorService = Executors.newCachedThreadPool(new CountingThreadFactory("tw-base"));
        scheduledTaskExecutor = new SimpleScheduledTaskExecutor(null, executorService);
        scheduledTaskExecutor.start();
        initialized = true;
      }
    } finally {
      initializationLock.unlock();
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
