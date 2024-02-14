package com.transferwise.common.baseutils.transactionsmanagement;

import com.transferwise.common.baseutils.ExceptionUtils;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

@Slf4j
public class TransactionsHelper implements ITransactionsHelper {

  @Autowired
  private PlatformTransactionManager transactionManager;

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
    private boolean flag;
    private Predicate<Throwable> rollbackOnCondition;

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
    public IBuilder rollbackFor(Collection<Class<? extends Throwable>> exceptions) {
      this.rollbackOnCondition = exceptions::contains;
      return this;
    }

    @Override
    public IBuilder noRollbackFor(Collection<Class<? extends Throwable>> exceptions) {
      this.rollbackOnCondition = Predicate.not(exceptions::contains);
      return this;
    }

    @Override
    public <T> T call(Callable<T> callable) {
      return ExceptionUtils.doUnchecked(() -> {
        WiseTransactionAttribute def =
            propagation == null ? new WiseTransactionAttribute() : new WiseTransactionAttribute(propagation.value());
        def.setReadOnly(readOnly);
        def.setName(name);
        if (isolation != null) {
          def.setIsolationLevel(isolation.value());
        }
        if (rollbackOnCondition != null) {
          def.setRollbackOnCondition(rollbackOnCondition);
        }
        if (timeout != null) {
          def.setTimeout(timeout);
        }

        TransactionStatus status = transactionManager.getTransaction(def);
        T result;
        try {
          result = callable.call();
        } catch (Throwable t) {
          if (def.rollbackOn(t)) {
            try {
              transactionManager.rollback(status);
            } catch (Throwable t2) {
              log.error("Failed to rollback transaction '{}' ({}).", name != null ? name : "<no-txn-name>", def, t2);
            }
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
