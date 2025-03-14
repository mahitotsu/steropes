package com.mahitotsu.steropes.api.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.mahitotsu.steropes.api.dao.AccountDAO;
import com.mahitotsu.steropes.api.dao.AccountRecord;
import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.infra.LockTemplate.LockRequest;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Service
public class AccountService {

    @EqualsAndHashCode
    @ToString
    private static class AccountImpl implements Account {

        public AccountImpl(final AccountRecord accountRecord) {
            this.accountRecord = accountRecord;
        }

        private final AccountRecord accountRecord;

        @Override
        public String getBranchNumber() {
            return this.accountRecord.getBranchNumber();
        }

        @Override
        public String getAccountNumber() {
            return this.accountRecord.getAccountNumber();
        }

        @Override
        public BigDecimal getMaxBalance() {
            return this.accountRecord.getMaxBalance();
        }
    }

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    @Qualifier("rw")
    private TransactionOperations rwTxOperations;

    @Autowired
    @Qualifier("ro")
    private TransactionOperations roTxOperations;

    @Autowired
    private LockTemplate lockOperations;

    public Account openAccount(final String branchNumber, final BigDecimal maxBalance) {

        return this.lockOperations.execute(LockRequest.builder().pKey("BRANCH_LOCK").sKey(branchNumber).build(),
                () -> this.rwTxOperations.execute(_ -> this._openAccount(branchNumber, maxBalance)));
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

        return accountRecord == null ? null : new AccountImpl(accountRecord);
    }

    public Account getAccount(final String branchNumber, final String accountNumber) {

        return this.roTxOperations
                .execute(_ -> this.accountDAO.findOneByBranchNumberAndAccountNumber(branchNumber, accountNumber)
                        .map(record -> new AccountImpl(record)).orElse(null));
    }
}
