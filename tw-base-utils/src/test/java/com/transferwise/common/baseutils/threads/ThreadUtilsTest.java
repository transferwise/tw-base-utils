package com.transferwise.common.baseutils.threads;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import com.transferwise.common.baseutils.ExceptionUtils;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class ThreadUtilsTest {

  @Test
  @SneakyThrows
  void takingThreadDumpWorks() {
    ProcessHandle processHandle = ProcessHandle.current();
    long pid = processHandle.pid();
    System.out.println("The PID of this Java process is: " + pid);

    var executorService = Executors.newCachedThreadPool();

    final var shouldRun = new AtomicBoolean(true);
    var runningThreads = new AtomicInteger(0);

    final var threadsCount = 100;
    for (int i = 0; i < threadsCount; i++) {
      executorService.submit(() -> {
        runningThreads.incrementAndGet();
        while (shouldRun.get()) {
          ExceptionUtils.doUnchecked(() -> Thread.sleep(1));
        }
      });
    }

    await().until(() -> runningThreads.get() == threadsCount);

    long start = System.currentTimeMillis();
    var threadInfos = ThreadUtils.getInconsistentThreadDump(executorService, threadsCount);

    shouldRun.set(false);

    System.out.println("It took " + (System.currentTimeMillis() - start) + "ms");

    System.out.println(ThreadUtils.toString(threadInfos));

    int suitableThreadsFound = 0;

    for (var threadInfo : threadInfos) {
      var st = ThreadUtils.toString(threadInfo);
      if (st.contains("at ")
          && st.contains(ThreadUtilsTest.class.getName())
          && st.contains("group=\"main\"")) {
        suitableThreadsFound++;
      }
    }

    // 1 is the test's thread itself
    assertThat(suitableThreadsFound, equalTo(threadsCount + 1));
  }

  @Test
  @SneakyThrows
  void takingThreadDumpWithCurrentThreadWorks() {
    assertThat(ThreadUtils.getInconsistentThreadDump().size(), greaterThan(0));
  }
}
