package com.mahitotsu.steropes.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import com.mahitotsu.steropes.api.AbstractTestBase;
import com.mahitotsu.steropes.api.orm.Account;
import com.mahitotsu.steropes.api.orm.AccountTransaction;

public class AccountRepositoryTest extends AbstractTestBase {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void testOpenAccount_Single() {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountRepository.openAccount(branchNumber, maxBalance);

        assertNotNull(account);
        assertNotNull(account.getId());
        assertEquals(branchNumber, account.getBranchNumber());
        assertNotNull(account.getAccountNumber());
        assertEquals(maxBalance, account.getMaxBalance());
    }

    @Test
    public void testOpenAccount_Multi_Serial() throws InterruptedException {

        final String branchNumber = this.randomBranchNumber();
        final Collection<Callable<Account>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final BigDecimal maxBalance = new BigDecimal("100" + String.valueOf(i) + ".00");
            tasks.add(() -> this.accountRepository.openAccount(branchNumber, maxBalance));
        }

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final List<Account> accounts = executor.invokeAll(tasks).stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error occurred while processing task.", e);
            }
        }).filter(i -> i != null).collect(Collectors.toList());

        assertEquals(tasks.size(), accounts.size());
        assertEquals(tasks.size(), accounts.stream().map(a -> a.getAccountNumber()).distinct().count());
    }

    @Test
    public void testOpenAccount_Multi_Parallel() throws InterruptedException {

        final String branchNumber = this.randomBranchNumber();
        final Collection<Callable<Account>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final BigDecimal maxBalance = new BigDecimal("100" + String.valueOf(i) + ".00");
            tasks.add(() -> this.accountRepository.openAccount(branchNumber, maxBalance));
        }

        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final List<Account> accounts = executor.invokeAll(tasks).stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error occurred while processing task.", e);
            }
        }).filter(i -> i != null).collect(Collectors.toList());

        assertEquals(tasks.size(), accounts.size());
        assertEquals(tasks.size(), accounts.stream().map(a -> a.getAccountNumber()).distinct().count());
    }

    @Test
    public void testGetAccount_Exists() {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account openedAccount = this.accountRepository.openAccount(branchNumber, maxBalance);
        final Account retrievedAccount = this.accountRepository.getAccount(openedAccount.getBranchNumber(),
                openedAccount.getAccountNumber());

        assertNotNull(retrievedAccount);
        assertEquals(openedAccount, retrievedAccount);
    }

    @Test
    public void testGetAccount_NotExists() {

        final String branchNumber = this.randomBranchNumber();
        final String accountNumber = "0000000";

        final Account retrievedAccount = this.accountRepository.getAccount(branchNumber, accountNumber);

        assertNull(retrievedAccount);
    }

    @Test
    public void testDeposit_Single() {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountRepository.openAccount(branchNumber, maxBalance);
        assertEquals(new BigDecimal("0.00"), this.accountRepository.getLastBalance(account));
        assertFalse(this.accountRepository.getAccountTransactions(account).iterator().hasNext());

        final BigDecimal amount = new BigDecimal("100.00");
        final BigDecimal newBalance = this.accountRepository.deposit(account, amount);
        assertNotNull(newBalance);
        assertEquals(newBalance, this.accountRepository.getLastBalance(account));

        final Iterator<AccountTransaction> i = this.accountRepository.getAccountTransactions(account).iterator();
        assertTrue(i.hasNext());

        final AccountTransaction lastTx = i.next();
        assertEquals(account.getBranchNumber(), lastTx.getBranchNumber());
        assertEquals(account.getAccountNumber(), lastTx.getAccountNumber());
        assertEquals(1, lastTx.getSequenceNumber());
        assertEquals(amount, lastTx.getAmount());
        assertEquals(newBalance, lastTx.getNewBalance());

        assertFalse(i.hasNext());
    }

    @Test
    public void testDeposit_Single_ExceededMaxBalance() {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountRepository.openAccount(branchNumber, maxBalance);
        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> this.accountRepository.deposit(account, maxBalance.add(new BigDecimal("1.00"))));
    }

    @Test
    public void testDeposit_Multi_Serial() throws InterruptedException {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountRepository.openAccount(branchNumber, maxBalance);
        final Collection<Callable<BigDecimal>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int amount = (i + 1) * 10;
            tasks.add(() -> this.accountRepository.deposit(account, new BigDecimal(String.valueOf(amount) + ".00")));
        }

        Executors.newFixedThreadPool(1).invokeAll(tasks);

        BigDecimal balance = this.accountRepository.getLastBalance(account);
        int index = tasks.size();
        final Iterator<AccountTransaction> transactions = this.accountRepository.getAccountTransactions(account).iterator();
        while (transactions.hasNext()) {
            final AccountTransaction tx = transactions.next();
            assertEquals(index, tx.getSequenceNumber().intValue());
            assertEquals(balance, tx.getNewBalance());
            index--;
            balance = balance.subtract(tx.getAmount());
        }
        assertEquals(new BigDecimal("0.00"), balance);
    }

    @Test
    public void testDeposit_Multi_Parallel() throws InterruptedException {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountRepository.openAccount(branchNumber, maxBalance);
        final Collection<Callable<BigDecimal>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int amount = (i + 1) * 10;
            tasks.add(() -> this.accountRepository.deposit(account, new BigDecimal(String.valueOf(amount) + ".00")));
        }

        Executors.newFixedThreadPool(3).invokeAll(tasks);

        BigDecimal balance = this.accountRepository.getLastBalance(account);
        int index = tasks.size();
        final Iterator<AccountTransaction> transactions = this.accountRepository.getAccountTransactions(account).iterator();
        while (transactions.hasNext()) {
            final AccountTransaction tx = transactions.next();
            assertEquals(index, tx.getSequenceNumber().intValue());
            assertEquals(balance, tx.getNewBalance());
            index--;
            balance = balance.subtract(tx.getAmount());
        }
        assertEquals(new BigDecimal("0.00"), balance);
    }
}
