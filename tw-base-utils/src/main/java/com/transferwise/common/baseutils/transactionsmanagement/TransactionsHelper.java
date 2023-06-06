package com.transferwise.common.baseutils.transactionsmanagement;

import com.transferwise.common.baseutils.ExceptionUtils;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
public class TransactionsHelper implements ITransactionsHelper {
  private final PlatformTransactionManager transactionManager;

  public TransactionsHelper(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  public boolean isRollbackOnly() {
    return transactionManager.getTransaction(null).isRollbackOnly();
  }

  @Override
  public void markAsRollbackOnly() {
    transactionManager.getTransaction(null).setRollbackOnly();
  }

  @Override
  public IBuilder withTransaction() {
    return new Builder(transactionManager);
  }

  private static class Builder implements IBuilder {

    private Propagation propagation;
    private PlatformTransactionManager transactionManager;
    private boolean readOnly;
    private String name;
    private Isolation isolation;
    private Integer timeout;

    private Builder(PlatformTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
    }

    @Override
    public IBuilder withPropagation(Propagation propagation) {
      this.propagation = propagation;
      return this;
    }

    @Override
    public IBuilder asNew() {
      this.propagation = Propagation.REQUIRES_NEW;
      return this;
    }

    @Override
    public IBuilder asSuspended() {
      this.propagation = Propagation.NOT_SUPPORTED;
      return this;
    }

    @Override
    public IBuilder asReadOnly() {
      return asReadOnly(true);
    }

    @Override
    public IBuilder asReadOnly(boolean readOnly) {
      this.readOnly = readOnly;
      return this;
    }

    @Override
    public IBuilder withName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public IBuilder withIsolation(Isolation isolation) {
      this.isolation = isolation;
      return this;
    }

    @Override
    public IBuilder withTimeout(Integer timeout) {
      this.timeout = timeout;
      return this;
    }

    @Override
    public <T> T call(Callable<T> callable) {
      return ExceptionUtils.doUnchecked(() -> {
        DefaultTransactionDefinition def =
            propagation == null ? new DefaultTransactionDefinition() : new DefaultTransactionDefinition(propagation.value());
        def.setReadOnly(readOnly);
        def.setName(name);
        if (isolation != null) {
          def.setIsolationLevel(isolation.value());
        }
        if (timeout != null) {
          def.setTimeout(timeout);
        }

        TransactionStatus status = transactionManager.getTransaction(def);
        T result;
        try {
          result = callable.call();
        } catch (Throwable t) {
          try {
            transactionManager.rollback(status);
          } catch (Throwable t2) {
            log.error("Failed to rollback transaction '{}' ({}).", name != null ? name : "<no-txn-name>", def, t2);
          }
          throw t;
        }
        transactionManager.commit(status);
        return result;
      });
    }

    @Override
    public void run(Runnable runnable) {
      call(() -> {
        runnable.run();
        return null;
      });
    }
  }
}
