package com.transferwise.common.baseutils.transactionsmanagement;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

public class TransactionsConfiguration {

  @Bean
  @ConditionalOnMissingBean(ITransactionsHelper.class)
  public TransactionsHelper twTransactionsHelper(PlatformTransactionManager platformTransactionManager) {
    return new TransactionsHelper(platformTransactionManager);
  }
}
