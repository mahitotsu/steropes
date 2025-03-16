package com.mahitotsu.steropes.api.model.impl;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.transaction.support.TransactionOperations;

import com.mahitotsu.steropes.api.dao.AccountRecord;
import com.mahitotsu.steropes.api.dao.AccountTransactionDAO;
import com.mahitotsu.steropes.api.dao.AccountTransactionRecord;
import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.model.Account;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class AccountBean implements Account {

    AccountBean(final AccountRecord accountRecord, final AccountTransactionDAO accountTransactionDAO,
            final TransactionOperations rwTxOperations, final TransactionOperations roTxOperations,
            final LockTemplate lockOperations) {

        this.accountRecord = accountRecord;
        this.accountTransactionDAO = accountTransactionDAO;
        this.rwTxOperations = rwTxOperations;
        this.roTxOperations = roTxOperations;
        this.lockOperations = lockOperations;
    }

    private final AccountRecord accountRecord;

    private final AccountTransactionDAO accountTransactionDAO;

    private final TransactionOperations rwTxOperations;

    private final TransactionOperations roTxOperations;

    private final LockTemplate lockOperations;

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

    @SuppressWarnings("null")
    @Override
    public BigDecimal getCurrentBalance() {
        return this.roTxOperations.execute(_ -> this.getLastTransaction(accountRecord))
                .map(t -> t.getNewBalane()).orElseGet(() -> new BigDecimal("0.00"));
    }

    private Optional<AccountTransactionRecord> getLastTransaction(final AccountRecord account) {
        return this.accountTransactionDAO
                .findFirstByAccountOrderBySequenceNumberDesc(this.accountRecord);
    }

    @Override
    public BigDecimal deposit(final BigDecimal amount) {
        return this.lockOperations.doWithLock(
                LockRequests.accountLock(this.accountRecord.getBranchNumber(), this.accountRecord.getAccountNumber()),
                () -> this.rwTxOperations.execute(_ -> this._deposit(amount).getNewBalane()));
    }

    private AccountTransactionRecord _deposit(final BigDecimal amount) {

        final AccountTransactionRecord lastTx = this.getLastTransaction(this.accountRecord).orElse(null);
        final BigDecimal newBalance = lastTx == null ? amount : lastTx.getNewBalane().add(amount);
        if (newBalance.compareTo(this.accountRecord.getMaxBalance()) >= 0) {
            throw new IllegalArgumentException("Exceeds max balance");
        }

        final Integer sequenceNumber = lastTx == null ? 1 : lastTx.getSequenceNumber() + 1;
        final AccountTransactionRecord newTx = AccountTransactionRecord.builder()
                .account(this.accountRecord)
                .sequenceNumber(sequenceNumber)
                .amount(amount)
                .newBalane(newBalance)
                .build();
        return this.accountTransactionDAO.save(newTx);
    }

    @Override
    public BigDecimal withdraw(final BigDecimal amount) {
        return this.lockOperations.doWithLock(
                LockRequests.accountLock(this.accountRecord.getBranchNumber(), this.accountRecord.getAccountNumber()),
                () -> this.rwTxOperations.execute(_ -> this._withdraw(amount).getNewBalane()));
    }

    private AccountTransactionRecord _withdraw(final BigDecimal amount) {

        final AccountTransactionRecord lastTx = this.getLastTransaction(this.accountRecord).orElse(null);
        final BigDecimal newBalance = lastTx == null ? amount : lastTx.getNewBalane().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Exceeds current balance");
        }

        final Integer sequenceNumber = lastTx == null ? 1 : lastTx.getSequenceNumber() + 1;
        final AccountTransactionRecord newTx = AccountTransactionRecord.builder()
                .account(this.accountRecord)
                .sequenceNumber(sequenceNumber)
                .amount(amount)
                .newBalane(newBalance)
                .build();
        return this.accountTransactionDAO.save(newTx);
    }
}
