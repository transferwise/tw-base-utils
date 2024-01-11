package com.transferwise.common.baseutils.concurrency;

import com.google.common.util.concurrent.RateLimiter;
import com.transferwise.common.baseutils.ExceptionUtils;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleScheduledTaskExecutor implements ScheduledTaskExecutor {

  private final ExecutorService executorService;
  private final DelayQueue<ScheduledTask> taskQueue;
  @SuppressWarnings("checkstyle:MagicNumber")
  private Duration tick = Duration.ofMillis(50);
  private Clock clock;
  private final Lock stateLock;
  private final Condition stateCondition;
  private final AtomicInteger workingTasksCount;
  private final RateLimiter nextTaskLoggingRateLimiter = RateLimiter.create(1);

  private volatile boolean started;
  private volatile boolean stopRequested;

  public SimpleScheduledTaskExecutor(String threadName, ExecutorService executorService) {
    if (threadName == null) {
      this.executorService = executorService;
    } else {
      this.executorService = new ThreadNamingExecutorServiceWrapper(threadName, executorService);
    }
    taskQueue = new DelayQueue<>();
    clock = Clock.systemUTC();
    stateLock = new ReentrantLock();
    stateCondition = stateLock.newCondition();
    workingTasksCount = new AtomicInteger();
  }

  @Override
  public TaskHandle scheduleAtFixedInterval(Runnable task, Duration initialDelay, Duration period) {
    ScheduledTask scheduledTask = new ScheduledTask(this, task, period);
    reschedule(scheduledTask, initialDelay);
    return scheduledTask.getTaskHandle();
  }

  @Override
  public TaskHandle scheduleOnce(Runnable task, Duration initialDelay) {
    ScheduledTask scheduledTask = new ScheduledTask(this, task, null);
    reschedule(scheduledTask, initialDelay);
    return scheduledTask.getTaskHandle();
  }

  public SimpleScheduledTaskExecutor setTick(Duration tick) {
    this.tick = tick;
    return this;
  }

  public SimpleScheduledTaskExecutor setClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  @Override
  public void start() {
    LockUtils.withLock(stateLock, () -> {
      if (started) {
        throw new IllegalStateException("Already started.");
      }
      started = true;
    });

    executorService.submit(() -> {
      while (!stopRequested) {
        ScheduledTask scheduledTask = null;
        try {
          scheduledTask = taskQueue.poll(tick.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          if (stopRequested) {
            break;
          } else {
            throw new RuntimeException(e);
          }
        }
        if (log.isDebugEnabled() && scheduledTask == null) {
          if (nextTaskLoggingRateLimiter.tryAcquire()) {
            var nextScheduledTask = taskQueue.peek();
            if (nextScheduledTask != null) {
              log.debug("Next scheduled task is executed at '{}'.", Instant.ofEpochMilli(nextScheduledTask.nextExecutionTime));
            }
          }
        }
        if (scheduledTask != null && !stopRequested) {
          executorService.submit(scheduledTask::execute);
        }
      }
    });
  }

  @Override
  public void stop() {
    LockUtils.withLock(stateLock, () -> {
      stopRequested = true;
      taskQueue.clear();
    });
  }

  @Override
  public boolean hasStopped() {
    return LockUtils.withLock(stateLock, () -> stopRequested && workingTasksCount.get() == 0);
  }

  @Override
  public boolean waitUntilStopped(Duration waitTime) {
    long start = currentTimeMillis();
    while (currentTimeMillis() < start + waitTime.toMillis()) {
      if (hasStopped()) {
        return true;
      }
      LockUtils.withLock(stateLock, () -> ExceptionUtils.doUnchecked(() -> {
        boolean ignored = stateCondition.await(start - currentTimeMillis() + waitTime.toMillis(), TimeUnit.MILLISECONDS);
      }));
    }
    return hasStopped();
  }

  public int getTaskQueueSize() {
    return taskQueue.size();
  }

  private void reschedule(ScheduledTask scheduledTask, Duration period) {
    LockUtils.withLock(stateLock, () -> {
      if (!stopRequested) {
        scheduledTask.nextExecutionTime = currentTimeMillis() + period.toMillis();
        taskQueue.add(scheduledTask);
      }
    });
  }

  private long currentTimeMillis() {
    return clock.millis();
  }

  private static class ScheduledTask implements Delayed {

    private final Runnable runnable;
    private final TaskHandle taskHandle;
    private final Duration period;
    private long nextExecutionTime;
    private final SimpleScheduledTaskExecutor taskExecutor;

    private final Lock stateLock;
    private final Condition stateCondition;

    private volatile boolean stopRequested;
    private volatile boolean working;

    private ScheduledTask(SimpleScheduledTaskExecutor taskExecutor, Runnable task, Duration period) {
      this.runnable = task;
      this.period = period;
      this.stateLock = new ReentrantLock();
      this.stateCondition = stateLock.newCondition();
      this.taskExecutor = taskExecutor;
      this.taskHandle = new DefaultTaskHandle();
    }

    private void execute() {
      if (!LockUtils.withLock(taskExecutor.stateLock, () -> LockUtils.withLock(stateLock, () -> {
        if (stopRequested()) {
          return false;
        }
        working = true;
        taskExecutor.workingTasksCount.incrementAndGet();
        return true;
      }))) {
        return;
      }

      try {
        runnable.run();
      } catch (Throwable t) {
        log.error(t.getMessage(), t);
      } finally {
        LockUtils.withLock(taskExecutor.stateLock, () -> {
          LockUtils.withLock(stateLock, () -> {
            working = false;
            taskExecutor.workingTasksCount.decrementAndGet();
            if (!stopRequested && period != null) {
              taskExecutor.reschedule(this, period);
            }
            stateCondition.signalAll();
          });
          taskExecutor.stateCondition.signalAll();
        });
      }
    }

    public TaskHandle getTaskHandle() {
      return taskHandle;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(nextExecutionTime - taskExecutor.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
      return Long.compare(nextExecutionTime, ((ScheduledTask) o).nextExecutionTime);
    }

    @Override
    public boolean equals(Object o) {
      return this == o;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(nextExecutionTime);
    }

    protected boolean stopRequested() {
      return stopRequested || taskExecutor.stopRequested;
    }

    class DefaultTaskHandle implements TaskHandle {

      @Override
      public void stop() {
        LockUtils.withLock(stateLock, () -> {
          stopRequested = true;
          // This is O(n)
          // TODO: Think if we should just leave the things into the queue.
          taskExecutor.taskQueue.remove(ScheduledTask.this);
          stateCondition.signalAll();
        });
      }

      @Override
      public boolean hasStopped() {
        return LockUtils.withLock(stateLock, () -> stopRequested() && !working);
      }

      @Override
      public boolean waitUntilStopped(Duration waitTime) {
        long start = taskExecutor.currentTimeMillis();
        while (taskExecutor.currentTimeMillis() >= start + waitTime.toMillis()) {
          if (hasStopped()) {
            return true;
          }
          LockUtils.withLock(stateLock, () -> ExceptionUtils.doUnchecked(() -> {
            boolean ignored = stateCondition.await(start - taskExecutor.currentTimeMillis() + waitTime.toMillis(), TimeUnit.MILLISECONDS);
          }));
        }
        return hasStopped();
      }

      @Override
      public boolean isWorking() {
        return working;
      }

    }
  }
}
