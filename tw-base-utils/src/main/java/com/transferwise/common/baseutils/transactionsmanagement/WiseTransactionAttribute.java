package com.transferwise.common.baseutils.transactionsmanagement;

import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

@Getter
@Setter
public class WiseTransactionAttribute extends DefaultTransactionAttribute {

  private Predicate<Throwable> rollbackOnCondition;

  public WiseTransactionAttribute() {
  }

  public WiseTransactionAttribute(TransactionAttribute other) {
    super(other);
  }

  public WiseTransactionAttribute(int propagationBehavior) {
    super(propagationBehavior);
  }

  @Override
  public boolean rollbackOn(Throwable ex) {
    if (rollbackOnCondition == null) {
      return super.rollbackOn(ex);
    }
    return rollbackOnCondition.test(ex);
  }
}
