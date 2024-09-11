package com.transferwise.common.baseutils;

import java.util.concurrent.Executors;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class ThreadUtilsTest {

  @Test
  @SneakyThrows
  void takingThreadDumpWorks() {

    var executorService = Executors.newCachedThreadPool();

    for (int i = 0; i < 100; i++) {
      executorService.submit(() -> {
        while (true) {
          var st = "blah";
          for (int j = 0; j < 16; j++) {
            st += st;
          }
          Thread.sleep(10);
        }
      });
    }

    Thread.sleep(100);

    long start = System.currentTimeMillis();
    var threadInfos = ThreadUtils.getInconsistentThreadDump(executorService, 100);

    System.out.println("It took " + (System.currentTimeMillis() - start) + "ms");

    System.out.println(ThreadUtils.toString(threadInfos));
  }
}
