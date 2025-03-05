package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.orm.Account;
import com.mahitotsu.steropes.api.orm.AccountDAO;
import com.mahitotsu.steropes.api.orm.AccountTransaction;
import com.mahitotsu.steropes.api.orm.AccountTransactionDAO;

@Repository
@Retryable(retryFor = {
        CannotAcquireLockException.class }, maxAttempts = 3, backoff = @Backoff(delay = 300, maxDelay = 3000, multiplier = 1.5, random = true))
public class AccountRepository {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private AccountTransactionDAO accountTransactionDAO;

    @Autowired
    private LockTemplate lockTemplate;

    @Autowired
    private TransactionOperations txOps;

    public Account openAccount(final String branchNumber, final BigDecimal maxBalance) {

        return this.lockTemplate.doWithLock("OPEN_ACCOUNT." + branchNumber,
                () -> this.txOps.execute(tx -> this._openAccount(branchNumber, maxBalance)));
    }

    @Transactional(readOnly = true)
    public Account getAccount(final String branchNumber, final String accountNumber) {

        return this.accountDAO.findByBranchNumberAndAccountNumber(branchNumber, accountNumber).orElse(null);
    }

    public BigDecimal deposit(final Account account, final BigDecimal amount) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();
        return this.lockTemplate.doWithLock("DEPOSIT." + branchNumber + accountNumber,
                () -> this.txOps
                        .execute(tx -> this._deposit(branchNumber, accountNumber, amount, account.getMaxBalance())));
    }

    @Transactional(readOnly = true)
    public BigDecimal getLastBalance(final Account account) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();

        final AccountTransaction lastTx = this.accountTransactionDAO
                .findFirstByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber, accountNumber)
                .orElse(null);
        return lastTx == null ? new BigDecimal("0.00") : lastTx.getNewBalance();
    }

    @Transactional(readOnly = true)
    public Iterable<AccountTransaction> getAccountTransactions(final Account account) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();

        return this.accountTransactionDAO
                .findByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber, accountNumber)
                .collect(Collectors.toList());
    }

    private Account _openAccount(final String branchNumber, final BigDecimal maxBalance) {

        final int lastAccountNumber = this.accountDAO
                .findFirstByBranchNumberOrderByAccountNumberDesc(branchNumber)
                .map(a -> Integer.parseInt(a.getAccountNumber())).orElse(0);
        final Account newAccount = new Account(branchNumber,
                String.format("%07d", lastAccountNumber + 1),
                maxBalance);
        this.accountDAO.save(newAccount);
        return this.accountDAO.findById(newAccount.getId()).get();
    }

    private BigDecimal _deposit(final String branchNumber, final String accountNumber, final BigDecimal amount,
            final BigDecimal maxBalance) {

        final AccountTransaction lastTx = this.accountTransactionDAO
                .findFirstByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber, accountNumber)
                .orElse(null);
        final BigDecimal oldBalance = lastTx == null ? new BigDecimal("0.00") : lastTx.getNewBalance();
        final BigDecimal newBalance = oldBalance.add(amount);
        if (newBalance.compareTo(maxBalance) > 0) {
            throw new IllegalArgumentException("The deposit is rejected due to exceeding the maximum balance.");
        }

        final AccountTransaction nextTx = new AccountTransaction(branchNumber, accountNumber,
                (lastTx == null ? 0 : lastTx.getSequenceNumber()) + 1, amount, newBalance);
        this.accountTransactionDAO.save(nextTx);
        return newBalance;
    }
}
