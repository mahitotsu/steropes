package com.mahitotsu.steropes.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.amazonaws.services.dynamodbv2.model.LockNotGrantedException;
import com.mahitotsu.steropes.api.model.Account;
import com.mahitotsu.steropes.api.model.AccountRepository;

import lombok.Data;
import software.amazon.awssdk.services.dynamodb.model.ReplicatedWriteConflictException;

@Service
@Retryable(retryFor = { LockNotGrantedException.class,
        ReplicatedWriteConflictException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 1.5, random = true))
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public AccountInfo openAccount(final String branchNumber, final long maxBalance) {
        return toInfo(this.accountRepository.openAccount(branchNumber, this.toBigDecimal(maxBalance)));
    }

    public void deposit(final String branchNumber, final String accountNumber, final long amount) {
        Optional.ofNullable(this.accountRepository.getAccount(branchNumber, accountNumber))
                .orElseThrow(NoSuchElementException::new)
                .deposit(this.toBigDecimal(amount));
    }

    public void withdraw(final String branchNumber, final String accountNumber, final long amount) {
        Optional.ofNullable(this.accountRepository.getAccount(branchNumber, accountNumber))
                .orElseThrow(NoSuchElementException::new)
                .withdraw(this.toBigDecimal(amount));
    }

    public void transfer(final String sourceBranchNumber, final String sourceAccountNumber,
            final String destinationBranchNumber, final String destinaitonAccountNumber, final long amount) {
        final Account sourceAccount = Optional
                .ofNullable(this.accountRepository.getAccount(sourceBranchNumber, sourceAccountNumber))
                .orElseThrow(NoSuchElementException::new);
        final Account destAccount = Optional
                .ofNullable(this.accountRepository.getAccount(destinationBranchNumber, destinaitonAccountNumber))
                .orElseThrow(NoSuchElementException::new);
        sourceAccount.transfer(destAccount, this.toBigDecimal(amount));
    }

    public AccountInfo getAccount(final String branchNumber, final String accountNumber) {
        return this.toInfo(Optional.ofNullable(this.accountRepository.getAccount(branchNumber, accountNumber))
                .orElseThrow(NoSuchElementException::new));
    }

    private BigDecimal toBigDecimal(final long value) {
        final BigDecimal decimal = new BigDecimal(value);
        decimal.setScale(2, RoundingMode.DOWN);
        return decimal;
    }

    private long toLong(final BigDecimal value) {
        return value.setScale(0, RoundingMode.DOWN).longValue();
    }

    private AccountInfo toInfo(final Account account) {

        final AccountInfo info = new AccountInfo();
        info.setBranchNumber(account.getBranchNumber());
        info.setAccountNumber(account.getAccountNumber());
        info.setMaxBalance(this.toLong(account.getMaxBalance()));
        info.setCurrentBalance(this.toLong(account.getCurrentBalance()));

        return info;
    }

    @Data
    public static class AccountInfo {
        private String branchNumber;
        private String accountNumber;
        private long maxBalance;
        private long currentBalance;
    }
}
