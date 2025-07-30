# Thread Utils

Utilities for thread management, monitoring, and debugging thread-related issues.

## Overview

The thread utilities package provides classes for working with threads, including thread information extraction, thread dumps, and thread monitoring capabilities.

## Key Classes

### ThreadUtils
Main utility class for thread operations and monitoring.

```java
// Get detailed thread information
ThreadInfo threadInfo = ThreadUtils.getThreadInfo(threadId);
String threadDetails = ThreadUtils.toString(threadInfo);

// Generate thread dump for all threads
List<ThreadInfo> allThreads = ThreadUtils.getAllThreadInfos();

// Generate thread dump with custom processing
ThreadUtils.processAllThreads((threadInfo) -> {
    if (threadInfo.getState() == Thread.State.BLOCKED) {
        log.warn("Blocked thread detected: {}", threadInfo.getName());
    }
});
```

## Usage Examples

### Thread Monitoring
```java
@Component
public class ThreadMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(ThreadMonitor.class);
    
    public void monitorThreads() {
        List<ThreadInfo> threadInfos = ThreadUtils.getAllThreadInfos();
        
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo.getState() == Thread.State.BLOCKED) {
                log.warn("Blocked thread: {}", ThreadUtils.toString(threadInfo));
            }
            
            if (threadInfo.getCpuTime() > TimeUnit.MINUTES.toNanos(5)) {
                log.warn("High CPU thread: {}", ThreadUtils.toString(threadInfo));
            }
        }
    }
    
    public void logThreadDump() {
        log.info("=== Thread Dump ===");
        ThreadUtils.processAllThreads(threadInfo -> {
            log.info(ThreadUtils.toString(threadInfo));
        });
        log.info("=== End Thread Dump ===");
    }
}
```

### Deadlock Detection
```java
@Service
public class DeadlockDetector {
    
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    
    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void checkForDeadlocks() {
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            log.error("Deadlock detected! Affected threads:");
            
            for (long threadId : deadlockedThreads) {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
                log.error("Deadlocked thread: {}", ThreadUtils.toString(threadInfo));
            }
            
            // Generate full thread dump for analysis
            ThreadUtils.processAllThreads(threadInfo -> {
                log.debug("Thread dump: {}", ThreadUtils.toString(threadInfo));
            });
        }
    }
}
```

### Performance Monitoring
```java
@Component
public class ThreadPerformanceMonitor {
    
    private final Map<Long, Long> lastCpuTimes = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 60000) // Monitor every minute
    public void monitorThreadPerformance() {
        ThreadUtils.processAllThreads(threadInfo -> {
            long threadId = threadInfo.getId();
            long currentCpuTime = threadInfo.getCpuTime();
            
            Long lastCpuTime = lastCpuTimes.get(threadId);
            if (lastCpuTime != null) {
                long cpuTimeDiff = currentCpuTime - lastCpuTime;
                long cpuTimeMs = TimeUnit.NANOSECONDS.toMillis(cpuTimeDiff);
                
                if (cpuTimeMs > 30000) { // More than 30 seconds of CPU time
                    log.warn("High CPU usage thread: {} used {}ms CPU time", 
                        threadInfo.getName(), cpuTimeMs);
                }
            }
            
            lastCpuTimes.put(threadId, currentCpuTime);
        });
    }
}
```

### Thread State Analysis
```java
@Service
public class ThreadStateAnalyzer {
    
    public ThreadStateReport analyzeThreadStates() {
        Map<Thread.State, Integer> stateCounts = new EnumMap<>(Thread.State.class);
        List<ThreadInfo> blockedThreads = new ArrayList<>();
        List<ThreadInfo> waitingThreads = new ArrayList<>();
        
        ThreadUtils.processAllThreads(threadInfo -> {
            Thread.State state = threadInfo.getState();
            stateCounts.merge(state, 1, Integer::sum);
            
            switch (state) {
                case BLOCKED:
                    blockedThreads.add(threadInfo);
                    break;
                case WAITING:
                case TIMED_WAITING:
                    waitingThreads.add(threadInfo);
                    break;
            }
        });
        
        return new ThreadStateReport(stateCounts, blockedThreads, waitingThreads);
    }
    
    public static class ThreadStateReport {
        private final Map<Thread.State, Integer> stateCounts;
        private final List<ThreadInfo> blockedThreads;
        private final List<ThreadInfo> waitingThreads;
        
        public ThreadStateReport(Map<Thread.State, Integer> stateCounts,
                               List<ThreadInfo> blockedThreads,
                               List<ThreadInfo> waitingThreads) {
            this.stateCounts = stateCounts;
            this.blockedThreads = blockedThreads;
            this.waitingThreads = waitingThreads;
        }
        
        // Getters...
    }
}
```

### Custom Thread Dump
```java
@RestController
public class DiagnosticsController {
    
    @GetMapping("/diagnostics/threads")
    public ResponseEntity<String> getThreadDump() {
        StringBuilder dump = new StringBuilder();
        dump.append("Thread Dump - ").append(Instant.now()).append("\n");
        dump.append("=" .repeat(50)).append("\n\n");
        
        ThreadUtils.processAllThreads(threadInfo -> {
            dump.append(ThreadUtils.toString(threadInfo)).append("\n\n");
        });
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(dump.toString());
    }
    
    @GetMapping("/diagnostics/threads/summary")
    public ResponseEntity<Map<String, Object>> getThreadSummary() {
        Map<String, Object> summary = new HashMap<>();
        Map<Thread.State, Integer> stateCounts = new EnumMap<>(Thread.State.class);
        AtomicInteger totalThreads = new AtomicInteger(0);
        AtomicLong totalCpuTime = new AtomicLong(0);
        
        ThreadUtils.processAllThreads(threadInfo -> {
            totalThreads.incrementAndGet();
            stateCounts.merge(threadInfo.getState(), 1, Integer::sum);
            totalCpuTime.addAndGet(threadInfo.getCpuTime());
        });
        
        summary.put("totalThreads", totalThreads.get());
        summary.put("stateDistribution", stateCounts);
        summary.put("totalCpuTimeMs", TimeUnit.NANOSECONDS.toMillis(totalCpuTime.get()));
        summary.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(summary);
    }
}
```

### Thread Pool Monitoring
```java
@Component
public class ThreadPoolMonitor {
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Monitor common thread pools
        monitorThreadPool("http-nio", "http-nio-");
        monitorThreadPool("task-executor", "task-");
        monitorThreadPool("scheduled", "scheduling-");
    }
    
    private void monitorThreadPool(String poolName, String threadPrefix) {
        List<ThreadInfo> poolThreads = new ArrayList<>();
        
        ThreadUtils.processAllThreads(threadInfo -> {
            if (threadInfo.getName().startsWith(threadPrefix)) {
                poolThreads.add(threadInfo);
            }
        });
        
        if (!poolThreads.isEmpty()) {
            log.info("Thread pool '{}' has {} threads", poolName, poolThreads.size());
            
            long blockedCount = poolThreads.stream()
                .mapToLong(ti -> ti.getState() == Thread.State.BLOCKED ? 1 : 0)
                .sum();
                
            if (blockedCount > 0) {
                log.warn("Thread pool '{}' has {} blocked threads", poolName, blockedCount);
            }
        }
    }
}
```

## Thread Information Details

The `ThreadUtils.toString()` method provides comprehensive thread information including:

- Thread name and ID
- Thread group
- Priority and daemon status
- CPU time and user time
- Thread state
- Lock information (if blocked)
- Stack trace (configurable depth)

## Best Practices

1. **Regular Monitoring**: Implement regular thread monitoring to catch issues early.

2. **Deadlock Detection**: Use scheduled deadlock detection in production systems.

3. **Resource Cleanup**: Monitor for threads that aren't properly cleaned up.

4. **Performance Analysis**: Track CPU usage patterns to identify performance bottlenecks.

5. **Alerting**: Set up alerts for abnormal thread states or counts.

6. **Thread Naming**: Use descriptive thread names for easier debugging.

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.threads`
