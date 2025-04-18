package com.transferwise.common.baseutils.concurrency;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Preconditions;
import com.transferwise.common.baseutils.BaseTest;
import com.transferwise.common.baseutils.clock.TestClock;
import com.transferwise.common.baseutils.concurrency.ScheduledTaskExecutor.TaskHandle;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Slf4j
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals"})
public class SimpleScheduledTaskExecutorTest extends BaseTest {

  @Test
  @Disabled("This test is for manual tweaking.")
  public void testManually() throws InterruptedException {
    ExecutorService executorService = Executors.newCachedThreadPool();
    SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService);
    scheduledTaskExecutor.start();

    ScheduledTaskExecutor.TaskHandle taskHandler = scheduledTaskExecutor
        .scheduleAtFixedInterval(() -> System.out.println("Working"), Duration.ofSeconds(1), Duration.ofSeconds(2));

    Thread.sleep(10000);

    System.out.println("Stopping the task");
    taskHandler.stop();

    Thread.sleep(5000);

    System.out.println("Stopping the executor.");
    scheduledTaskExecutor.stop();
  }

  @Test
  @Disabled("This test is for manual tweaking.")
  public void testManuallyOnceExecution() throws InterruptedException {
    ExecutorService executorService = Executors.newCachedThreadPool();
    SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService);
    scheduledTaskExecutor.start();

    ScheduledTaskExecutor.TaskHandle taskHandler = scheduledTaskExecutor.scheduleOnce(() -> System.out.println("Working"), Duration.ofSeconds(1));

    Thread.sleep(10000);

    System.out.println("Stopping the task");
    taskHandler.stop();

    Thread.sleep(5000);

    System.out.println("Stopping the executor.");
    scheduledTaskExecutor.stop();
  }

  @Test
  public void testHappyFlow() {
    TestClock testClock = new TestClock();
    Map<String, Long> results = new ConcurrentHashMap<>();

    ExecutorService executorService = Executors.newCachedThreadPool();
    SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService).setTick(Duration.ofMillis(5))
        .setClock(testClock);
    scheduledTaskExecutor.start();

    // For a bug where this created never-released lock.
    assertFalse(scheduledTaskExecutor.hasStopped());

    String resultKey = "myTask";
    ScheduledTaskExecutor.TaskHandle taskHandle = scheduledTaskExecutor.scheduleAtFixedInterval(() -> {
      if (results.get(resultKey) == null) {
        results.put(resultKey, 1L);
      } else {
        results.put(resultKey, results.get(resultKey) + 1);
      }
    }, Duration.ofSeconds(1), Duration.ofSeconds(2));
    // For a bug where this created never-released lock.
    assertFalse(taskHandle.hasStopped());

    assertNull(results.get(resultKey));
    testClock.tick(Duration.ofMillis(500));
    assertNull(results.get(resultKey));
    testClock.tick(Duration.ofMillis(501));
    await().until(() -> results.containsKey(resultKey) && results.get(resultKey) == 1L && !taskHandle.isWorking());
    testClock.tick(Duration.ofMillis(2001));
    await().until(() -> results.get(resultKey) == 2L && !taskHandle.isWorking());

    taskHandle.stop();
    taskHandle.waitUntilStopped(Duration.ofMillis(2000));
    assertTrue(taskHandle.hasStopped());
    testClock.tick(Duration.ofMillis(2001));
    assertEquals(2L, (long) results.get(resultKey));

    scheduledTaskExecutor.stop();
  }

  @Test
  @SneakyThrows
  public void testParallelExecution() {
    final int tasksCount = 1000;
    TestClock testClock = new TestClock();
    Map<Integer, Long> results = new ConcurrentHashMap<>();

    ExecutorService executorService = Executors.newCachedThreadPool();
    SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService).setTick(Duration.ofMillis(5))
        .setClock(testClock);
    scheduledTaskExecutor.start();

    CountDownLatch latch = new CountDownLatch(tasksCount);

    for (int i = 0; i < tasksCount; i++) {
      final int idx = i;
      scheduledTaskExecutor.scheduleAtFixedInterval(() -> {
        if (results.get(idx) == null) {
          results.put(idx, 1L);
        } else {
          results.put(idx, results.get(idx) + 1);
        }
        latch.countDown();
      }, Duration.ofSeconds(0), Duration.ofSeconds(2));
    }

    await().until(() -> {
      for (int i = 0; i < tasksCount; i++) {
        if (results.get(i) == null) {
          return false;
        }
      }
      return true;
    });

    Preconditions.checkState(latch.await(1, TimeUnit.MINUTES));

    await().until(() -> {
      testClock.tick(Duration.ofSeconds(1));
      for (int i = 0; i < tasksCount; i++) {
        if (results.get(i) < 2) {
          return false;
        }
      }
      return true;
    });

    scheduledTaskExecutor.stop();
    assertTrue(scheduledTaskExecutor.waitUntilStopped(Duration.ofSeconds(2)));
  }

  @Test
  public void testIfAfterTaskStopTheTaskQueueIsCleaned() {
    Map<String, Long> results = new ConcurrentHashMap<>();

    ExecutorService executorService = Executors.newCachedThreadPool();
    SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService).setTick(Duration.ofMillis(5));
    scheduledTaskExecutor.start();

    String resultKey = "myTask";
    ScheduledTaskExecutor.TaskHandle taskHandle = scheduledTaskExecutor.scheduleAtFixedInterval(() -> {
      if (results.get(resultKey) == null) {
        results.put(resultKey, 1L);
      } else {
        results.put(resultKey, results.get(resultKey) + 1);
      }
    }, Duration.ofHours(1), Duration.ofHours(2));

    assertEquals(1, scheduledTaskExecutor.getTaskQueueSize());
    taskHandle.stop();
    assertEquals(0, scheduledTaskExecutor.getTaskQueueSize());

    scheduledTaskExecutor.stop();
  }

  @Test
  public void testIfSchedulingOnceWorks() {
    TestClock testClock = new TestClock();
    Map<String, Long> results = new ConcurrentHashMap<>();

    ExecutorService executorService = Executors.newCachedThreadPool();
    SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService).setTick(Duration.ofMillis(5))
        .setClock(testClock);
    scheduledTaskExecutor.start();

    String resultKey = "myTask";
    final ScheduledTaskExecutor.TaskHandle taskHandle = scheduledTaskExecutor.scheduleOnce(() -> {
      if (results.get(resultKey) == null) {
        results.put(resultKey, 1L);
      } else {
        results.put(resultKey, results.get(resultKey) + 1);
      }
    }, Duration.ofSeconds(1));

    assertNull(results.get(resultKey));
    testClock.tick(Duration.ofMillis(500));
    assertNull(results.get(resultKey));
    testClock.tick(Duration.ofMillis(501));
    await().until(() -> results.containsKey(resultKey) && results.get(resultKey) == 1L && !taskHandle.isWorking());

    assertEquals(0, scheduledTaskExecutor.getTaskQueueSize());

    scheduledTaskExecutor.stop();
  }

  /*
    Covers a bug, where ScheduledTask's equals method was considering tasks with the same execution time as equals.

    Stopping one task, stopped all other tasks with the same execution time as well.
   */
  @Test
  public void testIfStoppingTasksWorksCorrectly() {
    var testClock = new TestClock();
    Map<Long, Boolean> results = new ConcurrentHashMap<>();

    var executorService = Executors.newCachedThreadPool();
    var scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService).setTick(Duration.ofMillis(5))
        .setClock(testClock);
    scheduledTaskExecutor.start();

    List<TaskHandle> taskHandleList = new ArrayList<>();
    var finishedCount = new AtomicInteger();
    for (int i = 0; i < 100; i++) {
      long finalI = i;
      taskHandleList.add(scheduledTaskExecutor.scheduleOnce(
          () -> {
            results.put(finalI, Boolean.TRUE);
            finishedCount.incrementAndGet();
          }, Duration.ofSeconds(1))
      );
    }

    taskHandleList.get(50).stop();
    testClock.tick(Duration.ofMillis(1001));

    await().until(() -> finishedCount.get() == 99);

    assertNull(results.get(50L));
    for (long i = 0; i < 100; i++) {
      if (i != 50) {
        assertNotNull(results.get(i));
      }
    }

    scheduledTaskExecutor.stop();
  }
}
