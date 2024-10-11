package com.transferwise.common.baseutils.threads;

import com.transferwise.common.baseutils.ExceptionUtils;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadUtils {

  public static String toString(ThreadInfo threadInfo) {
    var sb = new StringBuilder();
    sb.append("\"")
        .append(threadInfo.getName())
        .append("\"")
        .append(" group=\"").append(threadInfo.getGroupName()).append("\"")
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

  public static String toString(ThreadInfo[] threadInfos) {
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

  public static ThreadInfo[] getInconsistentThreadDump(ExecutorService executorService, int concurrency) {
    long startTimeMs = System.currentTimeMillis();

    try {
      return ExceptionUtils.doUnchecked(() -> {
        var threadMxBean = ManagementFactory.getThreadMXBean();

        var threads = org.apache.commons.lang3.ThreadUtils.findThreads((Predicate<Thread>) Objects::nonNull);

        var threadInfos = new ThreadInfo[threads.size()];

        var semaphore = new Semaphore(concurrency);
        var latch = new CountDownLatch(threads.size());

        int i = 0;
        for (var thread : threads) {
          var threadId = thread.getId();
          semaphore.acquire();
          int finalI = i;

          executorService.submit(() -> {
            try {
              StackTraceElement[] stackTrace = thread.getStackTrace();

              var threadState = new ThreadInfo()
                  .setStackTrace(stackTrace)
                  .setId(threadId)
                  .setState(thread.getState())
                  .setName(thread.getName())
                  .setPriority(thread.getPriority())
                  .setCpuTime(threadMxBean.getThreadCpuTime(threadId))
                  .setUserTime(threadMxBean.getThreadUserTime(threadId))
                  .setGroupName(thread.getThreadGroup().getName())
                  .setDaemon(thread.isDaemon());

              threadInfos[finalI] = threadState;
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

        return threadInfos;
      });
    } finally {
      if (log.isDebugEnabled()) {
        log.debug("Inconsistent thread dump took {} ms.", (System.currentTimeMillis() - startTimeMs));
      }
    }
  }

}