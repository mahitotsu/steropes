package com.mahitotsu.steropes.api.orm;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionSystemException;

import com.mahitotsu.steropes.api.AbstractTestBase;

import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolationException;

public class AccountTest extends AbstractTestBase {

    private Random random = new Random();

    @Autowired
    private AccountDAO accountRepository;

    private String randomBranchNumber() {
        return String.format("%03d", this.random.nextInt(1000));
    }

    private String randomAccountNumber() {
        return String.format("%07d", this.random.nextInt(10000000));
    }

    @Test
    public void testSave() {

        final String branchNumber = this.randomBranchNumber();
        final String accountNumber = this.randomAccountNumber();
        final BigDecimal maxBalance = new BigDecimal(1000);

        final Account saved = this.accountRepository.save(new Account(branchNumber, accountNumber, maxBalance));
        assertEquals(branchNumber, saved.getBranchNumber());
        assertEquals(accountNumber, saved.getAccountNubmer());
        assertEquals(maxBalance, saved.getMaxBalance());
        assertNotNull(saved.getId());
    }

    private void assertJpaValidation(final Executable executable) {

        final Exception e = assertThrows(TransactionSystemException.class, executable);

        final Throwable c1 = e.getCause();
        assertNotNull(c1);
        assertInstanceOf(RollbackException.class, c1);

        final Throwable c2 = c1.getCause();
        assertNotNull(c2);
        assertInstanceOf(ConstraintViolationException.class, c2);
    }

    @Test
    public void testSave_InvalidPropertyValues() {

        final String branchNumber = this.randomBranchNumber();
        final String accountNumber = this.randomAccountNumber();
        final BigDecimal maxBalance = new BigDecimal(1000);

        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(null, accountNumber, maxBalance)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account("0123", accountNumber, maxBalance)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account("01", accountNumber, maxBalance)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account("01a", accountNumber, maxBalance)));

        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(branchNumber, null, maxBalance)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(branchNumber, "01234567", maxBalance)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(branchNumber, "012345", maxBalance)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(branchNumber, "012345a", maxBalance)));

        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(branchNumber, accountNumber, null)));
        this.assertJpaValidation(
                () -> this.accountRepository.save(new Account(branchNumber, accountNumber, new BigDecimal(-1))));
        this.assertJpaValidation(
                () -> this.accountRepository
                        .save(new Account(branchNumber, accountNumber, new BigDecimal("10000000000000"))));
    }
}
