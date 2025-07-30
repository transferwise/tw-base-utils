# Transaction Utils

Utilities for managing database transactions with Spring's transaction management framework.

## Overview

The transaction utilities package provides helper classes for programmatic transaction management, offering a fluent API for transaction configuration and execution.

## Key Classes

### TransactionsHelper
Main utility class that provides a fluent API for transaction management.

```java
@Autowired
private ITransactionsHelper transactionsHelper;

// Execute code in a transaction
transactionsHelper.withTransaction()
    .withName("myTransaction")
    .withPropagation(Propagation.REQUIRED)
    .withIsolation(Isolation.READ_COMMITTED)
    .withTimeout(30)
    .call(() -> {
        // Your transactional code here
        userService.createUser(userData);
        auditService.logUserCreation(userData);
        return "success";
    });
```

### ITransactionsHelper
Interface defining the transaction helper contract.

## Key Methods

### `withTransaction()`
Returns a builder for configuring and executing transactional code.

### `isRollbackOnly()`
Checks if the current transaction is marked for rollback only.

```java
if (transactionsHelper.isRollbackOnly()) {
    log.warn("Transaction is marked for rollback");
}
```

### `markAsRollbackOnly()`
Marks the current transaction for rollback only.

```java
if (errorCondition) {
    transactionsHelper.markAsRollbackOnly();
}
```

## Transaction Builder API

The builder provides a fluent API for configuring transactions:

### `withName(String name)`
Sets a name for the transaction (useful for monitoring and debugging).

### `withPropagation(Propagation propagation)`
Sets the transaction propagation behavior.

### `withIsolation(Isolation isolation)`
Sets the transaction isolation level.

### `withTimeout(int timeoutSeconds)`
Sets the transaction timeout in seconds.

### `withReadOnly(boolean readOnly)`
Marks the transaction as read-only.

### `call(Callable<T> callable)`
Executes the callable within the configured transaction and returns the result.

### `run(Runnable runnable)`
Executes the runnable within the configured transaction.

## Usage Examples

### Basic Transaction
```java
@Service
public class UserService {
    
    @Autowired
    private ITransactionsHelper transactionsHelper;
    
    public User createUserWithAudit(UserData userData) {
        return transactionsHelper.withTransaction()
            .withName("createUserWithAudit")
            .call(() -> {
                User user = userRepository.save(new User(userData));
                auditRepository.save(new AuditLog("USER_CREATED", user.getId()));
                return user;
            });
    }
}
```

### Transaction with Custom Configuration
```java
public void performComplexOperation() {
    transactionsHelper.withTransaction()
        .withName("complexOperation")
        .withPropagation(Propagation.REQUIRES_NEW)
        .withIsolation(Isolation.SERIALIZABLE)
        .withTimeout(60)
        .run(() -> {
            // Complex operation that needs its own transaction
            performDataMigration();
            updateStatistics();
        });
}
```

### Read-Only Transaction
```java
public List<User> generateReport() {
    return transactionsHelper.withTransaction()
        .withName("generateReport")
        .withReadOnly(true)
        .withIsolation(Isolation.READ_COMMITTED)
        .call(() -> {
            // Read-only operations
            List<User> users = userRepository.findAll();
            return users.stream()
                .filter(this::isActiveUser)
                .collect(Collectors.toList());
        });
}
```

### Conditional Rollback
```java
public void processPayment(PaymentRequest request) {
    transactionsHelper.withTransaction()
        .withName("processPayment")
        .run(() -> {
            try {
                Payment payment = paymentService.createPayment(request);
                
                if (!paymentGateway.processPayment(payment)) {
                    // Mark transaction for rollback
                    transactionsHelper.markAsRollbackOnly();
                    throw new PaymentProcessingException("Payment failed");
                }
                
                notificationService.sendConfirmation(payment);
                
            } catch (Exception e) {
                // Transaction will be rolled back automatically
                log.error("Payment processing failed", e);
                throw e;
            }
        });
}
```

### Nested Transactions
```java
public void performNestedOperations() {
    transactionsHelper.withTransaction()
        .withName("outerTransaction")
        .withPropagation(Propagation.REQUIRED)
        .run(() -> {
            // Outer transaction work
            performOuterWork();
            
            // Inner transaction with different propagation
            transactionsHelper.withTransaction()
                .withName("innerTransaction")
                .withPropagation(Propagation.REQUIRES_NEW)
                .run(() -> {
                    // This runs in a separate transaction
                    performInnerWork();
                });
        });
}
```

### Transaction Status Checking
```java
public void conditionalProcessing() {
    transactionsHelper.withTransaction()
        .withName("conditionalProcessing")
        .run(() -> {
            processFirstStep();
            
            if (someErrorCondition()) {
                transactionsHelper.markAsRollbackOnly();
                return; // Transaction will be rolled back
            }
            
            processSecondStep();
            
            // Check if transaction is still valid
            if (transactionsHelper.isRollbackOnly()) {
                log.warn("Transaction marked for rollback, skipping final step");
                return;
            }
            
            processFinalStep();
        });
}
```

## Best Practices

1. **Use Descriptive Names**: Always provide meaningful transaction names for better monitoring and debugging.

2. **Choose Appropriate Propagation**: Understand the different propagation behaviors and choose the right one for your use case.

3. **Handle Exceptions Properly**: Let exceptions bubble up to trigger automatic rollback, or use `markAsRollbackOnly()` for conditional rollbacks.

4. **Set Appropriate Timeouts**: Configure realistic timeouts to prevent long-running transactions from blocking resources.

5. **Use Read-Only for Queries**: Mark read-only transactions to enable database optimizations.

6. **Avoid Long Transactions**: Keep transactions as short as possible to minimize lock contention.

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.transactionsmanagement`
