package com.transferwise.common.baseutils.concurrency;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultExecutorServicesProvider implements IExecutorServicesProvider {
    private ExecutorService executorService;
    private ScheduledTaskExecutor scheduledTaskExecutor;

    @PostConstruct
    public void init() {
        executorService = Executors.newCachedThreadPool(new CountingThreadFactory("tw-base"));
        scheduledTaskExecutor = new SimpleScheduledTaskExecutor(null, executorService);
        scheduledTaskExecutor.start();
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
