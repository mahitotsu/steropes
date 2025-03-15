package com.mahitotsu.steropes.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.TestMain;

public class AccountRepositoryTest extends TestMain {

    private static final Random RANDOM = new Random();

    @Autowired
    private AccountRepository accountService;

    private String randomBranchNumber() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    @Test
    public void testOpenAccount() {

        final String branchNumber = randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account opened = accountService.openAccount(branchNumber, maxBalance);
        assertNotNull(opened);
        assertNotNull(opened.getAccountNumber());
        assertEquals(maxBalance, opened.getMaxBalance());
        assertEquals(new BigDecimal("0.00"), opened.getCurrentBalance());

        final Account found = accountService.getAccount(branchNumber, opened.getAccountNumber());
        assertNotNull(found);
        assertEquals(opened, found);
    }

    @Test
    public void testOpenAccount_Multi_Parallel() throws InterruptedException {

        final String branchNumber = randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Callable<Account> task = () -> accountService.openAccount(branchNumber, maxBalance);
        final List<Callable<Account>> tasks = IntStream.range(0, 10).mapToObj(_ -> task).collect(Collectors.toList());
        final ExecutorService executor = Executors.newFixedThreadPool(4);

        final SortedSet<Account> results = executor.invokeAll(tasks).stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors
                .toCollection(() -> new TreeSet<>((a1, a2) -> a1.getAccountNumber().compareTo(a2.getAccountNumber()))));
        assertEquals(tasks.size(), results.size());
    }

    @Test
    public void testAccountTransaction() {

        final String branchNumber = randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountService.openAccount(branchNumber, maxBalance);
        assertEquals(new BigDecimal("0.00"), account.getCurrentBalance());

        account.deposit(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), account.getCurrentBalance());

        account.deposit(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("30.00"), account.getCurrentBalance());

        account.withdraw(new BigDecimal("25.00"));
        assertEquals(new BigDecimal("5.00"), account.getCurrentBalance());
    }

    @Test
    public void testAccountTransaction_Multi_Parallel() throws InterruptedException {

        final String branchNumber = randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountService.openAccount(branchNumber, maxBalance);
        account.deposit(new BigDecimal("100.00"));

        final Callable<BigDecimal> depositTask = () -> account.deposit(new BigDecimal("100.00"));
        final Callable<BigDecimal> withdrawTask = () -> account.withdraw(new BigDecimal("10.00"));
        final List<Callable<BigDecimal>> tasks = IntStream.range(0, 10)
                .mapToObj(i -> i % 2 == 0 ? depositTask : withdrawTask)
                .collect(Collectors.toList());

        final ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.invokeAll(tasks);

        assertEquals(new BigDecimal("550.00"), account.getCurrentBalance());
    }
}
