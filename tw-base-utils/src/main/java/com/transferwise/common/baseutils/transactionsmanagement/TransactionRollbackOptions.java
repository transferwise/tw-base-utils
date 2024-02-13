package com.transferwise.common.baseutils.transactionsmanagement;

import java.util.Collection;
import java.util.function.Predicate;

public class TransactionRollbackOptions {

  Predicate<Throwable> whitelist(Collection<Class<? extends Throwable>> exceptions) {
    return ex -> {
      if (exceptions.contains(ex)) {
        return false;
      }
      return ex instanceof RuntimeException || ex instanceof Error;
    };
  }

  Predicate<Throwable> blacklist(Collection<Class<? extends Throwable>> exceptions) {
    return ex -> {
      if (exceptions.contains(ex)) {
        return true;
      }
      return ex instanceof Error;
    };
  }
}
