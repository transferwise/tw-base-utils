# Concurrency Utils

Utilities for concurrent programming, thread management, and asynchronous task execution.

## Overview

The concurrency utilities package provides classes for managing concurrent operations, thread pools, scheduled tasks, and queue processing with various threading utilities.

## Key Classes

### LockUtils
Utility methods for working with locks in a more functional style.

```java
// Execute code with a lock
LockUtils.withLock(myLock, () -> {
    // Critical section code
    sharedResource.modify();
});

// Execute code with a lock and return a value
String result = LockUtils.withLock(myLock, () -> {
    return sharedResource.getValue();
});
```

### DiscardingQueueProcessor
A queue processor that can discard items when under pressure, with configurable soft and hard limits.

```java
DiscardingQueueProcessor<String, ProcessedData> processor = 
    new DiscardingQueueProcessor<String, ProcessedData>()
        .setExecutorService(executorService)
        .setProcessor(data -> processData(data))
        .setDataTransformer(input -> transform(input))
        .setSoftLimitPredicate(item -> shouldApplySoftLimit(item))
        .setHardLimit(1000)
        .setSoftLimit(800);

processor.start();

// Add items to process
processor.add("item1");
processor.add("item2");

// Stop processing
processor.stop(() -> System.out.println("Processor stopped"));
```

### SimpleScheduledTaskExecutor
A simple implementation of scheduled task execution with configurable tick intervals.

```java
SimpleScheduledTaskExecutor executor = new SimpleScheduledTaskExecutor("my-scheduler", executorService);
executor.start();

// Schedule a recurring task
TaskHandle handle = executor.scheduleAtFixedRate(() -> {
    System.out.println("Periodic task executed");
}, Duration.ofSeconds(10));

// Stop the task
handle.stop();

// Stop the executor
executor.stop();
```

### ThreadNamingExecutorServiceWrapper
Wraps an ExecutorService to automatically name threads for better debugging.

```java
ExecutorService originalExecutor = Executors.newFixedThreadPool(5);
ExecutorService namedExecutor = new ThreadNamingExecutorServiceWrapper("MyService", originalExecutor);

// All tasks submitted to namedExecutor will have thread names prefixed with "MyService"
namedExecutor.submit(() -> {
    // Thread name will be something like "pool-1-thread-1-MyService"
    System.out.println("Running in: " + Thread.currentThread().getName());
});
```

### CountingThreadFactory
A thread factory that creates threads with incrementing names.

```java
ThreadFactory factory = new CountingThreadFactory("worker");
ExecutorService executor = Executors.newFixedThreadPool(3, factory);
// Creates threads named: worker-1, worker-2, worker-3, etc.
```

### DefaultExecutorServicesProvider
Provides default executor services with proper lifecycle management.

```java
@Autowired
private IExecutorServicesProvider executorProvider;

// Get global executor service
ExecutorService executor = executorProvider.getGlobalExecutorService();

// Get scheduled task executor
ScheduledTaskExecutor scheduler = executorProvider.getGlobalScheduledTaskExecutor();
```

## Usage Examples

### Safe Lock Operations
```java
private final Lock dataLock = new ReentrantLock();
private Map<String, String> data = new HashMap<>();

public void updateData(String key, String value) {
    LockUtils.withLock(dataLock, () -> {
        data.put(key, value);
    });
}

public String getData(String key) {
    return LockUtils.withLock(dataLock, () -> {
        return data.get(key);
    });
}
```

### Queue Processing with Backpressure
```java
@Service
public class DataProcessor {
    
    private DiscardingQueueProcessor<RawData, ProcessedData> processor;
    
    @PostConstruct
    public void init() {
        processor = new DiscardingQueueProcessor<RawData, ProcessedData>()
            .setExecutorService(Executors.newFixedThreadPool(4))
            .setProcessor(this::processData)
            .setDataTransformer(this::transformData)
            .setHardLimit(10000)
            .setSoftLimit(8000)
            .setSoftLimitPredicate(data -> data.getPriority() < Priority.HIGH);
        
        processor.start();
    }
    
    public void processAsync(RawData data) {
        processor.add(data);
    }
    
    private ProcessedData transformData(RawData raw) {
        return new ProcessedData(raw);
    }
    
    private void processData(ProcessedData data) {
        // Process the data
        saveToDatabase(data);
    }
}
```

### Scheduled Tasks
```java
@Component
public class ScheduledTaskManager {
    
    private SimpleScheduledTaskExecutor scheduler;
    private List<TaskHandle> taskHandles = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        ExecutorService executor = Executors.newCachedThreadPool();
        scheduler = new SimpleScheduledTaskExecutor("scheduler", executor);
        scheduler.start();
        
        // Schedule periodic cleanup
        TaskHandle cleanupTask = scheduler.scheduleAtFixedRate(
            this::performCleanup, 
            Duration.ofHours(1)
        );
        taskHandles.add(cleanupTask);
    }
    
    private void performCleanup() {
        // Cleanup logic
        log.info("Performing scheduled cleanup");
    }
    
    @PreDestroy
    public void shutdown() {
        taskHandles.forEach(TaskHandle::stop);
        scheduler.stop();
    }
}
```

## Best Practices

1. **Use LockUtils**: Prefer `LockUtils.withLock()` over manual lock/unlock to ensure proper cleanup.

2. **Thread Naming**: Use `ThreadNamingExecutorServiceWrapper` for better debugging and monitoring.

3. **Backpressure Handling**: Use `DiscardingQueueProcessor` when you need to handle high-throughput scenarios with graceful degradation.

4. **Lifecycle Management**: Always properly start and stop schedulers and processors.

5. **Resource Cleanup**: Use `@PreDestroy` or try-with-resources patterns to ensure proper cleanup.

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.concurrency`
