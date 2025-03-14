package com.mahitotsu.steropes.api.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.TestMain;

public class AccountServiceTest extends TestMain{

    private static final Random RANDOM = new Random();

    @Autowired
    private AccountService accountService;

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

        final Account found = accountService.getAccount(branchNumber, opened.getAccountNumber());
        assertNotNull(found);
        assertEquals(opened, found);
    }
}
