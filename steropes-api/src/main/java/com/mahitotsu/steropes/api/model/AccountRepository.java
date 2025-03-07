package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

import com.mahitotsu.steropes.api.infra.LockKey;
import com.mahitotsu.steropes.api.infra.LockKeys;
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
    @Qualifier("rw")
    private TransactionOperations rwTxOp;

    @Autowired
    @Qualifier("ro")
    private TransactionOperations roTxOp;

    public Account openAccount(final String branchNumber, final BigDecimal maxBalance) {

        return this.lockTemplate.doWithLock(LockKeys.forBranch(branchNumber),
                () -> this.rwTxOp.execute(tx -> this._openAccount(branchNumber, maxBalance)));
    }

    public Account getAccount(final String branchNumber, final String accountNumber) {

        return this.roTxOp.execute(
                tx -> this.accountDAO.findByBranchNumberAndAccountNumber(branchNumber, accountNumber)
                        .orElse(null));
    }

    public BigDecimal deposit(final Account account, final BigDecimal amount) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();
        return this.lockTemplate.doWithLock(LockKeys.forAccount(branchNumber, accountNumber),
                () -> this.rwTxOp
                        .execute(tx -> this._deposit(branchNumber, accountNumber, amount,
                                account.getMaxBalance())));
    }

    public BigDecimal withdraw(final Account account, final BigDecimal amount) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();
        return this.lockTemplate.doWithLock(LockKeys.forAccount(branchNumber, accountNumber),
                () -> this.rwTxOp
                        .execute(tx -> this._withdraw(branchNumber, accountNumber, amount,
                                new BigDecimal("0.00"))));
    }

    public BigDecimal getLastBalance(final Account account) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();

        final AccountTransaction lastTx = this.roTxOp.execute((tx) -> this.accountTransactionDAO
                .findFirstByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber,
                        accountNumber)
                .orElse(null));
        return lastTx == null ? new BigDecimal("0.00") : lastTx.getNewBalance();
    }

    public Iterable<AccountTransaction> getAccountTransactions(final Account account) {

        final String branchNumber = account.getBranchNumber();
        final String accountNumber = account.getAccountNumber();

        return this.roTxOp.execute(tx -> this.accountTransactionDAO
                .findByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber,
                        accountNumber)
                .collect(Collectors.toList()));
    }

    public boolean transfer(final Account senderAccount, final Account recipientAccount, final BigDecimal amount) {

        final LockKey sLock = LockKeys.forAccount(senderAccount.getBranchNumber(),
                senderAccount.getAccountNumber());
        final LockKey rLock = LockKeys.forAccount(recipientAccount.getBranchNumber(),
                recipientAccount.getAccountNumber());

        return this.lockTemplate.doWithLock(LockKeys.sort(new LockKey[] { sLock, rLock }),
                () -> this.rwTxOp.execute(tx -> {
                    this.withdraw(senderAccount, amount);
                    this.deposit(recipientAccount, amount);
                    return true;
                }));
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
                .findFirstByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber,
                        accountNumber)
                .orElse(null);
        final BigDecimal oldBalance = lastTx == null ? new BigDecimal("0.00") : lastTx.getNewBalance();
        final BigDecimal newBalance = oldBalance.add(amount);
        if (newBalance.compareTo(maxBalance) > 0) {
            throw new IllegalArgumentException(
                    "The deposit is rejected due to exceeding the maximum balance.");
        }

        final AccountTransaction nextTx = new AccountTransaction(branchNumber, accountNumber,
                (lastTx == null ? 0 : lastTx.getSequenceNumber()) + 1, amount, newBalance);
        this.accountTransactionDAO.save(nextTx);
        return newBalance;
    }

    private BigDecimal _withdraw(final String branchNumber, final String accountNumber, final BigDecimal amount,
            final BigDecimal minBalance) {

        final AccountTransaction lastTx = this.accountTransactionDAO
                .findFirstByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(branchNumber,
                        accountNumber)
                .orElse(null);
        final BigDecimal oldBalance = lastTx == null ? new BigDecimal("0.00") : lastTx.getNewBalance();
        final BigDecimal newBalance = oldBalance.subtract(amount);
        if (newBalance.compareTo(minBalance) < 0) {
            throw new IllegalArgumentException(
                    "The withdrawal is rejected due to falling below the minimum balance.");
        }

        final AccountTransaction nextTx = new AccountTransaction(branchNumber, accountNumber,
                (lastTx == null ? 0 : lastTx.getSequenceNumber()) + 1, amount, newBalance);
        this.accountTransactionDAO.save(nextTx);
        return newBalance;
    }
}
