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

public class AccountTransactionTest extends AbstractTestBase {

    private Random random = new Random();

    @Autowired
    private AccountTransactionDAO accountTransactionRepository;

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
        final int sequenceNumber = 1;
        final BigDecimal amount = new BigDecimal(10);
        final BigDecimal newBalance = new BigDecimal(1000);

        final AccountTransaction saved = this.accountTransactionRepository
                .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber, amount,
                        newBalance));
        assertEquals(branchNumber, saved.getBranchNumber());
        assertEquals(accountNumber, saved.getAccountNubmer());
        assertEquals(sequenceNumber, saved.getSequenceNumber());
        assertEquals(amount, saved.getAmount());
        assertEquals(newBalance, saved.getNewBalance());
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
        final int sequenceNumber = 1;
        final BigDecimal amount = new BigDecimal(10);
        final BigDecimal newBalance = new BigDecimal(1000);

        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(null, accountNumber, sequenceNumber, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction("0123", accountNumber, sequenceNumber, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction("01", accountNumber, sequenceNumber, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction("01a", accountNumber, sequenceNumber, amount, newBalance)));

        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, null, sequenceNumber, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, "01234567", sequenceNumber, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, "012345", sequenceNumber, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, "012345a", sequenceNumber, amount, newBalance)));

        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, null, amount, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, Integer.valueOf(0), amount,
                                newBalance)));

        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber, null, newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber,
                                new BigDecimal("-10000000000000"), newBalance)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber,
                                new BigDecimal("10000000000000"), newBalance)));

        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber, amount, null)));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber, amount,
                                new BigDecimal("-10000000000000"))));
        this.assertJpaValidation(
                () -> this.accountTransactionRepository
                        .save(new AccountTransaction(branchNumber, accountNumber, sequenceNumber, amount,
                                new BigDecimal("10000000000000"))));
    }
}
