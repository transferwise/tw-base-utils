package com.transferwise.common.baseutils.transactionsmanagement;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

/**
 * We will not follow old J2EE logic about RuntimeExceptions vs CheckedExceptions and rollback on any exception.
 */
public interface ITransactionsHelper {

  boolean isRollbackOnly();

  void markAsRollbackOnly();

  IBuilder withTransaction();

  interface IBuilder {

    IBuilder withPropagation(Propagation propagation);

    IBuilder asNew();

    IBuilder asSuspended();

    IBuilder asReadOnly();

    IBuilder asReadOnly(boolean readOnly);

    IBuilder withName(String name);

    IBuilder withIsolation(Isolation isolation);

    IBuilder withTimeout(Integer timeout);

    IBuilder rollbackFor(Collection<Class<? extends Throwable>> exceptions);

    IBuilder noRollbackFor(Collection<Class<? extends Throwable>> exceptions);

    <T> T call(Callable<T> callable);

    void run(Runnable runnable);
  }
}
