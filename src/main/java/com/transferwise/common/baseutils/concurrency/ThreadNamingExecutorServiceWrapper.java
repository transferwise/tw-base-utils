package com.transferwise.common.baseutils.concurrency;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class ThreadNamingExecutorServiceWrapper implements ExecutorService {
    private ExecutorService delegate;
    private String threadName;

    public ThreadNamingExecutorServiceWrapper(String threadName, ExecutorService delegate) {
        this.threadName = threadName;
        this.delegate = delegate;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrap(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrap(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrap(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrap(tasks), timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    private <T> Collection<? extends Callable<T>> wrap(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(this::wrap).collect(Collectors.toList());
    }

    protected <T> Callable<T> wrap(Callable<T> delegate) {
        return () -> {
            String currentName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName(currentName + "-" + threadName);
                return delegate.call();
            } finally {
                Thread.currentThread().setName(currentName);
            }
        };
    }

    protected Runnable wrap(Runnable delegate) {
        return () -> {
            String currentName = Thread.currentThread().getName();
            try {
                Thread.currentThread().setName(currentName + "-" + threadName);
                delegate.run();
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
            } finally {
                Thread.currentThread().setName(currentName);
            }
        };
    }
}
