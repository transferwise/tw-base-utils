package com.transferwise.common.baseutils.concurrency;

import java.util.concurrent.ExecutorService;

public interface IExecutorServicesProvider {

  /**
   * Threads are not bounded. Concurrency control has to be done by other means.
   */
  ExecutorService getGlobalExecutorService();

  /**
   * Threads are not bounded. Concurrency control has to be done by other means.
   */
  ScheduledTaskExecutor getGlobalScheduledTaskExecutor();
}
