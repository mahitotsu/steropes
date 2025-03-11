package com.mahitotsu.steropes.api.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.LockItem;
import com.amazonaws.services.dynamodbv2.model.LockNotGrantedException;
import com.mahitotsu.steropes.api.TestMain;

public class LockConfigTest extends TestMain {

    private static class LockOwnerNameHolder implements Runnable {

        private LockOwnerNameHolder(final BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        private BeanFactory beanFactory;

        private LockItem lockItem;

        private String ownerName;

        @Override
        public void run() {

            final AmazonDynamoDBLockClient lockClient = this.beanFactory.getBean(AmazonDynamoDBLockClient.class);
            try {
                this.lockItem = lockClient.tryAcquireLock(AcquireLockOptions
                        .builder(UUID.randomUUID().toString()).withSortKey(UUID.randomUUID().toString()).build())
                        .orElse(null);
                if (this.lockItem != null) {
                    this.ownerName = this.lockItem.getOwnerName();
                }
            } catch (InterruptedException e) {
                this.lockItem = null;
            } finally {
                if (this.lockItem != null) {
                    lockClient.releaseLock(this.lockItem);
                }
            }
        }

        public String getOwnerName() {
            return this.ownerName;
        }
    }

    @Autowired
    private BeanFactory beanFactory;

    @Test
    public void testLockOwnerName_SameThreads() throws InterruptedException {

        final LockOwnerNameHolder l1 = new LockOwnerNameHolder(this.beanFactory);
        final LockOwnerNameHolder l2 = new LockOwnerNameHolder(this.beanFactory);
        final Thread t1 = new Thread(() -> {
            l1.run();
            l2.run();
        });
        t1.start();
        t1.join();

        final String on1 = l1.getOwnerName();
        final String on2 = l2.getOwnerName();
        assertNotNull(on1);
        assertNotNull(on2);
        assertEquals(on1, on2);
    }

    @Test
    public void testLockOwnerName_DifferentThreads() throws InterruptedException {

        final LockOwnerNameHolder l1 = new LockOwnerNameHolder(this.beanFactory);
        final LockOwnerNameHolder l2 = new LockOwnerNameHolder(this.beanFactory);

        final Thread t1 = new Thread(l1);
        final Thread t2 = new Thread(l2);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        final String on1 = l1.getOwnerName();
        final String on2 = l2.getOwnerName();
        assertNotNull(on1);
        assertNotNull(on2);
        assertNotEquals(on1, on2);
    }

    @Test
    public void testLock_BetweenTwoThreads() throws InterruptedException {

        final AtomicInteger i = new AtomicInteger(0);
        final String pKey = UUID.randomUUID().toString();
        final String sKey = UUID.randomUUID().toString();

        final Thread t1 = new Thread(() -> {

            final AmazonDynamoDBLockClient lockClient = this.beanFactory.getBean(AmazonDynamoDBLockClient.class);
            LockItem lockItem = null;
            synchronized (i) {
                try {
                    lockItem = lockClient.acquireLock(AcquireLockOptions
                            .builder(pKey)
                            .withSortKey(sKey)
                            .withDeleteLockOnRelease(true)
                            .build());
                    assertNotNull(lockItem);
                    assertTrue(lockClient.hasLock(pKey, Optional.of(sKey)));
                    i.getAndIncrement();
                } catch (BeansException | LockNotGrantedException | InterruptedException e) {
                    //
                }
                i.notify();
            }

            synchronized (i) {
                while (i.get() < 2) {
                    try {
                        i.wait(500);
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }

            if (lockItem != null) {
                lockClient.releaseLock(lockItem);
            }
            assertFalse(lockClient.hasLock(pKey, Optional.of(sKey)));
        });

        final Thread t2 = new Thread(() -> {

            final AmazonDynamoDBLockClient lockClient = this.beanFactory.getBean(AmazonDynamoDBLockClient.class);
            synchronized (i) {
                while (i.get() < 1) {
                    try {
                        i.wait(500);
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }

            assertFalse(lockClient.hasLock(pKey, Optional.of(sKey)));
            i.getAndIncrement();

            synchronized (i) {
                i.notify();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
