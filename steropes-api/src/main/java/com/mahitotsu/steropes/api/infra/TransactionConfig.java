package com.mahitotsu.steropes.api.infra;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    @Bean
    @Qualifier("rw")
    public TransactionOperations readWriteTxTemplate(final PlatformTransactionManager transactionManager) {

        final TransactionTemplate rwTx = new TransactionTemplate(transactionManager);
        rwTx.setReadOnly(false);
        rwTx.setPropagationBehavior(TransactionAttribute.PROPAGATION_REQUIRED);
        return rwTx;
    }

    @Bean
    @Qualifier("ro")
    public TransactionOperations readOnlyTxTemplate(final PlatformTransactionManager transactionManager) {

        final TransactionTemplate rwTx = new TransactionTemplate(transactionManager);
        rwTx.setReadOnly(true);
        rwTx.setPropagationBehavior(TransactionAttribute.PROPAGATION_REQUIRED);
        return rwTx;
    }
}
