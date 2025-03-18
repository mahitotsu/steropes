package com.mahitotsu.steropes.api.model.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import com.mahitotsu.steropes.api.dao.AccountDAO;
import com.mahitotsu.steropes.api.dao.AccountRecord;
import com.mahitotsu.steropes.api.dao.AccountTransactionDAO;
import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.model.Account;
import com.mahitotsu.steropes.api.model.AccountRepository;

@Component
public class AccountRepositoryBean implements AccountRepository {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private AccountTransactionDAO accountTransactionDAO;

    @Autowired
    @Qualifier("rw")
    private TransactionOperations rwTxOperations;

    @Autowired
    @Qualifier("ro")
    private TransactionOperations roTxOperations;

    @Autowired
    private LockTemplate lockOperations;

    public Account openAccount(final String branchNumber, final BigDecimal maxBalance) {

        return this.lockOperations.execute(LockRequests.branchLock(branchNumber),
                () -> this.rwTxOperations.execute(_ -> this._openAccount(branchNumber, maxBalance)));
    }

    private Account createAccount(final AccountRecord accountRecord) {
        return new AccountBean(accountRecord, this.accountTransactionDAO,
                this.rwTxOperations, this.roTxOperations, this.lockOperations);
    }

    private Account _openAccount(final String branchNumber, final BigDecimal maxBalance) {

        final int lastAccountNumber = this.accountDAO
                .findFirstByBranchNumberOrderByAccountNumberDesc(branchNumber)
                .map(account -> Integer.parseInt(account.getAccountNumber()))
                .orElse(0);
        final String nextAccountNumber = String.format("%07d", lastAccountNumber + 1);

        final UUID accountId = this.accountDAO.save(AccountRecord.builder()
                .branchNumber(branchNumber)
                .accountNumber(nextAccountNumber)
                .maxBalance(maxBalance)
                .build()).getAccountId();
        final AccountRecord accountRecord = this.accountDAO.findById(accountId).orElse(null);

        return accountRecord == null ? null : this.createAccount(accountRecord);
    }

    public Account getAccount(final String branchNumber, final String accountNumber) {

        return this.roTxOperations
                .execute(_ -> this.accountDAO.findOneByBranchNumberAndAccountNumber(branchNumber, accountNumber)
                        .map(record -> this.createAccount(record)).orElse(null));
    }
}
