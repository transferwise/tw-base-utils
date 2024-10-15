package com.transferwise.common.baseutils.threads;

import com.google.common.util.concurrent.MoreExecutors;
import com.transferwise.common.baseutils.ExceptionUtils;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadUtils {

  public static String toString(ThreadInfo threadInfo) {
    var sb = new StringBuilder();
    sb.append("\"")
        .append(threadInfo.getName())
        .append("\"")
        .append(" group=\"").append(threadInfo.getGroupName() == null ? "system" : threadInfo.getGroupName()).append("\"")
        .append(" id=").append(threadInfo.getId())
        .append(" prio=").append(threadInfo.getPriority())
        .append(" daemon=").append(threadInfo.isDaemon())
        .append(" cpuTime=").append(threadInfo.getCpuTime() / 1_000_000)
        .append(" userTime=").append(threadInfo.getUserTime() / 1_000_000)
        .append(" ").append(threadInfo.getState());

    for (var stackTraceElement : threadInfo.getStackTrace()) {
      sb.append("\n").append("\tat ").append(stackTraceElement.toString());
    }

    return sb.toString();
  }

  public static String toString(List<ThreadInfo> threadInfos) {
    var sb = new StringBuilder();
    boolean first = true;
    for (var threadInfo : threadInfos) {
      if (!first) {
        sb.append("\n\n");
      } else {
        first = false;
      }
      sb.append(toString(threadInfo));
    }
    return sb.toString();
  }

  public static List<ThreadInfo> getInconsistentThreadDump() {
    return getInconsistentThreadDump(MoreExecutors.newDirectExecutorService(), 1);
  }

  public static List<ThreadInfo> getInconsistentThreadDump(ExecutorService executorService, int concurrency) {
    long startTimeMs = System.currentTimeMillis();

    try {
      return ExceptionUtils.doUnchecked(() -> getInconsistentThreadDump0(executorService, concurrency));
    } finally {
      if (log.isDebugEnabled()) {
        log.debug("Inconsistent thread dump took {} ms to take.", (System.currentTimeMillis() - startTimeMs));
      }
    }
  }

  private static ArrayList<ThreadInfo> getInconsistentThreadDump0(ExecutorService executorService, int concurrency) throws InterruptedException {
    var threadMxBean = ManagementFactory.getThreadMXBean();

    var threads = org.apache.commons.lang3.ThreadUtils.findThreads((Predicate<Thread>) Objects::nonNull);

    final var threadsCount = threads.size();
    var threadInfos = new AtomicReferenceArray<ThreadInfo>(threadsCount);

    var semaphore = new Semaphore(concurrency);
    var latch = new CountDownLatch(threadsCount);

    int i = 0;
    for (var thread : threads) {
      var threadId = thread.getId();
      semaphore.acquire();
      int finalI = i;

      executorService.submit(() -> {
        try {
          StackTraceElement[] stackTrace = thread.getStackTrace();
          final var threadGroup = thread.getThreadGroup();
          var threadGroupName = threadGroup == null ? null : threadGroup.getName();

          var threadInfo = new ThreadInfo()
              .setStackTrace(stackTrace)
              .setId(threadId)
              .setState(thread.getState())
              .setName(thread.getName())
              .setPriority(thread.getPriority())
              .setCpuTime(threadMxBean.getThreadCpuTime(threadId))
              .setUserTime(threadMxBean.getThreadUserTime(threadId))
              .setGroupName(threadGroupName)
              .setDaemon(thread.isDaemon());

          threadInfos.set(finalI, threadInfo);
        } catch (Throwable t) {
          log.error("Processing thread with id {} failed.", threadId, t);
        } finally {
          semaphore.release();
          latch.countDown();
        }
      });
      i++;
    }

    if (!latch.await(1, TimeUnit.MINUTES)) {
      throw new IllegalStateException("We were unable to complete inconsistent thread dump.");
    }

    var result = new ArrayList<ThreadInfo>(threadsCount);
    for (int j = 0; j < threadsCount; j++) {
      var threadInfo = threadInfos.get(j);
      if (threadInfo != null) {
        result.add(threadInfo);
      }
    }
    return result;
  }
}
