package com.transferwise.common.baseutils.concurrency;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.base.Preconditions;
import com.transferwise.common.baseutils.ExceptionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DiscardingQueueProcessorTest {

  private ExecutorService executorService;
  private DiscardingQueueProcessor<String, String> processor;
  private List<String> results;
  private List<Throwable> errors;

  @BeforeEach
  void beforeEach() {
    results = Collections.synchronizedList(new ArrayList<>());
    errors = Collections.synchronizedList(new ArrayList<>());
    executorService = Executors.newCachedThreadPool();
    processor = new DiscardingQueueProcessor<>(executorService, payload -> results.add(payload.getData())) {
      @Override
      public void onError(Throwable t) {
        errors.add(t);
      }
    };
    processor.setSoftQueueLimit(5);
    processor.setHardQueueLimit(10);

    processor.start();
  }

  @AfterEach
  void afterEach() {
    processor.stop(() -> executorService.shutdown());
  }

  @Test
  void processingSingleEventWorks() {
    processor.schedule("Hello TransferWise!");
    await().until(() -> results.size(), equalTo(1));
    assertThat(results.get(0), equalTo("Hello TransferWise!"));

    processor.schedule("Hi");
    await().until(() -> results.size(), equalTo(2));
    assertThat(results.get(1), equalTo("Hi"));
  }

  @Test
  void softQueueSizeWillDiscardSimilarMessages() {
    var latch = new CountDownLatch(20);

    processor.setProcessor(payload -> {
      awaitOrThrow(latch);
      results.add(payload.getData());
    });

    processor.setSoftLimitPredicate(data -> true);

    var softDiscardedCount = 0;
    for (var i = 0; i < 20; i++) {
      var result = processor.schedule(String.valueOf(i));
      if (!result.isScheduled() && result.getDiscardReason() == DiscardingQueueProcessor.DiscardReason.SOFT_LIMIT) {
        softDiscardedCount++;
      }
      latch.countDown();
    }

    await().until(() -> results.size(), equalTo(5));

    assertThat(results.size(), equalTo(5));
    assertThat(softDiscardedCount, equalTo(15));
  }

  @Test
  void hardQueueLimitIsApplied() {
    var latch = new CountDownLatch(20);

    processor.setProcessor(payload -> {
      awaitOrThrow(latch);
      results.add(payload.getData());
    });

    processor.setSoftLimitPredicate(data -> false);

    for (var i = 0; i < 20; i++) {
      processor.schedule(String.valueOf(i));
      latch.countDown();
    }

    await().until(() -> results.size(), equalTo(10));
  }

  @Test
  void processorErrorsWillNotStopProcessing() {
    var latch = new CountDownLatch(20);

    processor.setProcessor(payload -> {
      awaitOrThrow(latch);
      throw new RuntimeException("Bad Things Do Happen");
    });

    for (var i = 0; i < 20; i++) {
      processor.schedule(String.valueOf(i));
      latch.countDown();
    }

    await().until(() -> errors.size(), equalTo(10));
    assertThat(results.size(), equalTo(0));
    assertThat(errors.size(), equalTo(10));
    assertThat(errors.get(0).getMessage(), equalTo("Bad Things Do Happen"));

    var latch1 = new CountDownLatch(20);
    processor.setProcessor(payload -> {
      awaitOrThrow(latch1);
      results.add(payload.getData());
    });

    for (var i = 0; i < 20; i++) {
      processor.schedule(String.valueOf(i));
      latch1.countDown();
    }

    await().until(() -> results.size(), equalTo(10));
  }

  void awaitOrThrow(CountDownLatch latch) {
    ExceptionUtils.doUnchecked(() -> Preconditions.checkState(latch.await(10, TimeUnit.SECONDS)));
  }
}
