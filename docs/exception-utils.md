# ExceptionUtils

Utility class for handling exceptions and converting checked exceptions to unchecked ones.

## Overview

The `ExceptionUtils` class provides static methods to simplify exception handling, particularly for converting checked exceptions to runtime exceptions and formatting exception information.

## Key Methods

### `doUnchecked(Callable<T> callable)`
Executes a `Callable` and converts any checked exceptions to unchecked runtime exceptions.

```java
// Example usage
String result = ExceptionUtils.doUnchecked(() -> {
    // Code that might throw checked exceptions
    return someMethodThatThrowsCheckedException();
});
```

### `doUnchecked(RunnableWithException runnable)`
Executes a `RunnableWithException` and converts any checked exceptions to unchecked runtime exceptions.

```java
// Example usage
ExceptionUtils.doUnchecked(() -> {
    // Code that might throw checked exceptions
    someMethodThatThrowsCheckedException();
});
```

### `toUnchecked(Throwable t)`
Converts a `Throwable` to a `RuntimeException`. Handles special cases like `Error`, `UndeclaredThrowableException`, and `InvocationTargetException`.

```java
try {
    riskyOperation();
} catch (Exception e) {
    throw ExceptionUtils.toUnchecked(e);
}
```

### `rootCauseToString(Throwable t, int stacksCount)`
Returns a string representation of the root cause of an exception with a specified number of stack trace elements.

```java
String rootCause = ExceptionUtils.rootCauseToString(exception, 5);
```

### `toString(Throwable t, int stacksCount)`
Returns a string representation of the exception chain with a specified number of stack trace elements for each exception.

### `getAllCauseMessages(Throwable t)`
Returns all cause messages in the exception chain as a single string, separated by newlines.

```java
String allMessages = ExceptionUtils.getAllCauseMessages(exception);
```

## Usage Examples

### Converting Checked Exceptions
```java
// Instead of handling checked exceptions manually
public String readFile(String path) {
    return ExceptionUtils.doUnchecked(() -> {
        return Files.readString(Paths.get(path));
    });
}
```

### Exception Information Extraction
```java
try {
    complexOperation();
} catch (Exception e) {
    // Get root cause with 3 stack trace lines
    String rootCause = ExceptionUtils.rootCauseToString(e, 3);
    log.error("Root cause: {}", rootCause);
    
    // Get all exception messages in the chain
    String allMessages = ExceptionUtils.getAllCauseMessages(e);
    log.error("All messages: {}", allMessages);
}
```

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.ExceptionUtils`
