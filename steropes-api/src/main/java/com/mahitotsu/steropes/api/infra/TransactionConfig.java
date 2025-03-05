package com.mahitotsu.steropes.api.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    @Bean
    public TransactionOperations transactionOperations(final PlatformTransactionManager transactionManager) {

        final TransactionTemplate rwTx = new TransactionTemplate(transactionManager);
        rwTx.setReadOnly(false);
        return rwTx;
    }
}
