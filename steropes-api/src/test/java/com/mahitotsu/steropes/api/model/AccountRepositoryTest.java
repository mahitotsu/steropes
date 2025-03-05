package com.mahitotsu.steropes.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.AbstractTestBase;
import com.mahitotsu.steropes.api.orm.Account;

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
}
