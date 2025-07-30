# Clock Utils

Utilities for working with time and clock operations, including clock holders and test clock implementations.

## Overview

The clock utilities package provides classes for managing time operations, including a centralized clock holder and utilities for testing time-dependent code.

## Key Classes

### ClockHolder
A centralized holder for the system clock that can be replaced for testing purposes.

```java
// Get current time using the system clock
Clock clock = ClockHolder.getClock();
long currentMillis = clock.millis();
Instant now = clock.instant();
```

### ClockUtils
Utility methods for common clock operations.

```java
// Example clock utility operations
Duration elapsed = ClockUtils.measureExecutionTime(() -> {
    // Some operation to measure
    expensiveOperation();
});
```

### TestClock
A test implementation of `Clock` that allows manual control of time for testing purposes.

```java
// In tests, you can control time
TestClock testClock = new TestClock();
ClockHolder.setClock(testClock);

// Advance time manually
testClock.advanceBy(Duration.ofMinutes(5));

// Reset to system clock after test
ClockHolder.useSystemClock();
```

## Usage Examples

### Basic Time Operations
```java
// Get current time
Clock clock = ClockHolder.getClock();
Instant now = clock.instant();
long millis = clock.millis();
```

### Testing Time-Dependent Code
```java
@Test
public void testTimeBasedLogic() {
    TestClock testClock = new TestClock();
    ClockHolder.setClock(testClock);
    
    try {
        // Set specific time
        testClock.setTime(Instant.parse("2023-01-01T10:00:00Z"));
        
        // Test your time-dependent logic
        MyService service = new MyService();
        service.scheduleTask();
        
        // Advance time
        testClock.advanceBy(Duration.ofHours(1));
        
        // Verify behavior after time advancement
        assertTrue(service.isTaskReady());
        
    } finally {
        // Always reset to system clock
        ClockHolder.useSystemClock();
    }
}
```

### Measuring Execution Time
```java
// Measure how long an operation takes
Duration executionTime = ClockUtils.measureExecutionTime(() -> {
    // Your operation here
    processLargeDataset();
});

System.out.println("Operation took: " + executionTime.toMillis() + "ms");
```

## Best Practices

1. **Use ClockHolder in production code**: Instead of `System.currentTimeMillis()` or `Instant.now()`, use `ClockHolder.getClock()` to make your code testable.

2. **Always reset clock in tests**: Use try-finally blocks or test cleanup methods to reset the clock to system time.

3. **Inject clock dependencies**: For better testability, consider injecting clock instances rather than using static methods.

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.clock`
