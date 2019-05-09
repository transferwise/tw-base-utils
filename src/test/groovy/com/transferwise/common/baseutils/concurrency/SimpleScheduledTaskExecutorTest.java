package com.transferwise.common.baseutils.concurrency;

import com.transferwise.common.baseutils.clock.TestClock;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class SimpleScheduledTaskExecutorTest {
    @Test
    @Ignore("This test is for manual tweaking.")
    public void testManually() throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService);
        scheduledTaskExecutor.start();

        ScheduledTaskExecutor.TaskHandle taskHandler = scheduledTaskExecutor.scheduleAtFixedInterval(() -> {
            System.out.println("Working");
        }, Duration.ofSeconds(1), Duration.ofSeconds(2));

        Thread.sleep(10000);

        System.out.println("Stopping the task");
        taskHandler.stop();

        Thread.sleep(5000);

        System.out.println("Stopping the executor.");
        scheduledTaskExecutor.stop();
    }

    @Test
    @Ignore("This test is for manual tweaking.")
    public void testManuallyOnceExecution() throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService);
        scheduledTaskExecutor.start();

        ScheduledTaskExecutor.TaskHandle taskHandler = scheduledTaskExecutor.scheduleOnce(() -> {
            System.out.println("Working");
        }, Duration.ofSeconds(1));

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

        String resultKey = "myTask";
        ScheduledTaskExecutor.TaskHandle taskHandle = scheduledTaskExecutor.scheduleAtFixedInterval(() -> {
            if (results.get(resultKey) == null) {
                results.put(resultKey, 1l);
            } else {
                results.put(resultKey, results.get(resultKey) + 1);
            }
        }, Duration.ofSeconds(1), Duration.ofSeconds(2));

        assertEquals(results.get(resultKey), null);
        testClock.tick(Duration.ofMillis(500));
        assertEquals(results.get(resultKey), null);
        testClock.tick(Duration.ofMillis(501));
        await().until(() -> results.containsKey(resultKey) && results.get(resultKey) == 1l && !taskHandle.isWorking());
        testClock.tick(Duration.ofMillis(2001));
        await().until(() -> results.get(resultKey) == 2l && !taskHandle.isWorking());

        taskHandle.stop();
        taskHandle.waitUntilStopped(Duration.ofMillis(2000));
        assertTrue(taskHandle.hasStopped());
        testClock.tick(Duration.ofMillis(2001));
        assertTrue(results.get(resultKey) == 2l);

        scheduledTaskExecutor.stop();
    }

    @Test
    public void testParallelExecution() {
        int N = 1000;
        TestClock testClock = new TestClock();
        Map<Integer, Long> results = new ConcurrentHashMap<>();

        ExecutorService executorService = Executors.newCachedThreadPool();
        SimpleScheduledTaskExecutor scheduledTaskExecutor = new SimpleScheduledTaskExecutor("test", executorService).setTick(Duration.ofMillis(5))
                .setClock(testClock);
        scheduledTaskExecutor.start();

        for (int i = 0; i < N; i++) {
            final int idx = i;
            scheduledTaskExecutor.scheduleAtFixedInterval(() -> {
                if (results.get(idx) == null) {
                    results.put(idx, 1l);
                } else {
                    results.put(idx, results.get(idx) + 1);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, Duration.ofSeconds(0), Duration.ofSeconds(2));
        }

        await().until(() -> {
            for (int i = 0; i < N; i++) {
                if (Objects.equals(results.get(i), 1)) {
                    return false;
                }
            }
            return true;
        });

        testClock.tick(Duration.ofSeconds(3));

        await().until(() -> {
            for (int i = 0; i < N; i++) {
                if (Objects.equals(results.get(i), 2)) {
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
                results.put(resultKey, 1l);
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
        ScheduledTaskExecutor.TaskHandle taskHandle = scheduledTaskExecutor.scheduleOnce(() -> {
            if (results.get(resultKey) == null) {
                results.put(resultKey, 1l);
            } else {
                results.put(resultKey, results.get(resultKey) + 1);
            }
        }, Duration.ofSeconds(1));

        assertEquals(results.get(resultKey), null);
        testClock.tick(Duration.ofMillis(500));
        assertEquals(results.get(resultKey), null);
        testClock.tick(Duration.ofMillis(501));
        await().until(() -> results.containsKey(resultKey) && results.get(resultKey) == 1l && !taskHandle.isWorking());

        assertEquals(0, scheduledTaskExecutor.getTaskQueueSize());

        scheduledTaskExecutor.stop();
    }
}
