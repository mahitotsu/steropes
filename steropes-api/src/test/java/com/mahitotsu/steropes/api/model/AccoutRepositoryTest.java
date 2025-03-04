package com.mahitotsu.steropes.api.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.AbstractTestBase;
import com.mahitotsu.steropes.api.orm.Account;

public class AccoutRepositoryTest extends AbstractTestBase{

    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    public void testOpenAccount() {

        final String branchNumber = this.randomBranchNumber();
        final BigDecimal maxBalance = new BigDecimal("1000.00");

        final Account account = this.accountRepository.openAccount(branchNumber, maxBalance);
        assertNotNull(account);
    }
}
