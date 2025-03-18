package com.mahitotsu.steropes.api.model.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.support.TransactionOperations;

import com.mahitotsu.steropes.api.dao.AccountRecord;
import com.mahitotsu.steropes.api.dao.AccountTransactionDAO;
import com.mahitotsu.steropes.api.dao.AccountTransactionRecord;
import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.model.Account;
import com.mahitotsu.steropes.api.model.AccountTransaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
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
                .map(t -> t.getNewBalance()).orElseGet(() -> new BigDecimal("0.00"));
    }

    private Optional<AccountTransactionRecord> getLastTransaction(final AccountRecord account) {
        return this.accountTransactionDAO
                .findFirstByAccountOrderBySequenceNumberDesc(this.accountRecord);
    }

    @Override
    public BigDecimal deposit(final BigDecimal amount) {
        return this.lockOperations.execute(
                LockRequests.accountLock(this.accountRecord.getBranchNumber(), this.accountRecord.getAccountNumber()),
                () -> this.rwTxOperations.execute(_ -> this._deposit(amount).getNewBalance()));
    }

    private AccountTransactionRecord _deposit(final BigDecimal amount) {

        final AccountTransactionRecord lastTx = this.getLastTransaction(this.accountRecord).orElse(null);
        final BigDecimal newBalance = lastTx == null ? amount : lastTx.getNewBalance().add(amount);
        if (newBalance.compareTo(this.accountRecord.getMaxBalance()) >= 0) {
            throw new IllegalArgumentException("Exceeds max balance");
        }

        final Integer sequenceNumber = lastTx == null ? 1 : lastTx.getSequenceNumber() + 1;
        final AccountTransactionRecord newTx = AccountTransactionRecord.builder()
                .account(this.accountRecord)
                .sequenceNumber(sequenceNumber)
                .amount(amount)
                .newBalance(newBalance)
                .build();
        return this.accountTransactionDAO.save(newTx);
    }

    @Override
    public BigDecimal withdraw(final BigDecimal amount) {
        return this.lockOperations.execute(
                LockRequests.accountLock(this.accountRecord.getBranchNumber(), this.accountRecord.getAccountNumber()),
                () -> this.rwTxOperations.execute(_ -> this._withdraw(amount).getNewBalance()));
    }

    private AccountTransactionRecord _withdraw(final BigDecimal amount) {

        final AccountTransactionRecord lastTx = this.getLastTransaction(this.accountRecord).orElse(null);
        final BigDecimal newBalance = lastTx == null ? amount : lastTx.getNewBalance().subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Exceeds current balance");
        }

        final Integer sequenceNumber = lastTx == null ? 1 : lastTx.getSequenceNumber() + 1;
        final AccountTransactionRecord newTx = AccountTransactionRecord.builder()
                .account(this.accountRecord)
                .sequenceNumber(sequenceNumber)
                .amount(amount.negate())
                .newBalance(newBalance)
                .build();
        return this.accountTransactionDAO.save(newTx);
    }

    @Override
    public BigDecimal transfer(final Account destination, final BigDecimal amount) {
        return this.lockOperations.execute(
                Arrays.asList(LockRequests.accountLock(this), LockRequests.accountLock(destination)),
                () -> this.rwTxOperations.execute(_ -> {
                    final BigDecimal newBalance = this.withdraw(amount);
                    destination.deposit(amount);
                    return newBalance;
                }));
    }

    @Override
    public List<AccountTransaction> getRecentTransactions() {
        return this.roTxOperations
                .execute(_ -> this.accountTransactionDAO.findByAccountOrderBySequenceNumberDesc(this.accountRecord)
                        .map(r -> new AccountTransactionBean(r))
                        .collect(Collectors.toCollection(() -> new ArrayList<AccountTransaction>())));
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    private static class AccountTransactionBean implements AccountTransaction {

        AccountTransactionBean(final AccountTransactionRecord record) {
            this.sequenceNumber = record.getSequenceNumber();
            this.amount = record.getAmount();
            this.newBalance =  record.getNewBalance();
        }

        private int sequenceNumber;
        private BigDecimal amount;
        private BigDecimal newBalance;
    }
}
