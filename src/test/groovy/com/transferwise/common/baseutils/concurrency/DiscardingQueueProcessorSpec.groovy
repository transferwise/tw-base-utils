package com.transferwise.common.baseutils.concurrency


import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static org.awaitility.Awaitility.await

public class DiscardingQueueProcessorSpec extends Specification {
    private ExecutorService executorService
    private DiscardingQueueProcessor<String, String> processor
    private List<String> results
    private List<Throwable> errors;

    def setup() {
        results = Collections.synchronizedList(new ArrayList<>());
        errors = Collections.synchronizedList(new ArrayList<>());
        executorService = Executors.newCachedThreadPool()
        processor = new DiscardingQueueProcessor<String, String>(executorService, { payload ->
            results.add(payload.data)
        }) {
            @Override
            void onError(Throwable t) {
                errors.add(t);
            }
        }
        processor.setSoftQueueLimit(5)
        processor.setHardQueueLimit(10)

        processor.start()
    }

    def cleanup() {
        processor.stop({
            executorService.shutdown()
        })
    }

    def "processing a single event works"() {
        when:
            processor.schedule("Hello TransferWise!")
            await().until() {
                results.size() == 1
            }
        then:
            results[0] == "Hello TransferWise!"
        when:
            processor.schedule("Hi")
            await().until {
                results.size() == 2
            }
        then:
            results[1] == "Hi"
    }

    def "soft queue size will discard similar messages"() {
        when:
            CountDownLatch latch = new CountDownLatch(20)

            processor.processor = { payload ->
                latch.await()
                results.add(payload.data)
            }

            processor.setSoftLimitPredicate({ data -> true })

            int softDiscardedCount = 0
            20.times {
                def result = processor.schedule("${it}")
                if (!result.scheduled && result.discardReason == DiscardingQueueProcessor.DiscardReason.SOFT_LIMIT) {
                    softDiscardedCount++
                }
                latch.countDown()
            }

            await().until() {
                println(results.size())
                results.size() == 5
            }
        then:
            results.size() == 5
            softDiscardedCount == 15
    }

    def "hard queue limit is applied"() {
        when:
            CountDownLatch latch = new CountDownLatch(20)

            processor.processor = { payload ->
                latch.await()
                results.add(payload.data)
            }

            processor.setSoftLimitPredicate({ data -> false })

            20.times {
                processor.schedule("${it}")
                latch.countDown()
            }

            await().until() {
                results.size() == 10
            }
        then:
            results.size() == 10
    }

    def "processor errors will not stop processing"() {
        when:
            CountDownLatch latch = new CountDownLatch(20)

            processor.processor = { payload ->
                latch.await()
                throw new Exception("Bad Things Do Happen");
            }

            20.times {
                processor.schedule("${it}")
                latch.countDown()
            }

            await().until({
                errors.size() == 10
            })
        then:
            results.size() == 0
            errors.size() == 10
            errors[0].getMessage() == "Bad Things Do Happen"
        when:
            latch = new CountDownLatch(20)
            processor.processor = { payload ->
                latch.await()
                results.add(payload.data)
            }

            20.times {
                processor.schedule("${it}")
                latch.countDown()
            }

            await().until() {
                results.size() == 10
            }
        then:
            results.size() == 10
    }

}
