package com.transferwise.common.baseutils.transactionsmanagement;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class TransactionsConfiguration {
    @Bean
    @ConditionalOnMissingBean(ITransactionsHelper.class)
    public TransactionsHelper twTransactionsHelper() {
        return new TransactionsHelper();
    }
}
