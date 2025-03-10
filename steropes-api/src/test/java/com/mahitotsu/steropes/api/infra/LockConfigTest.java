package com.mahitotsu.steropes.api.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    @Autowired
    private BeanFactory beanFactory;

    @Test
    public void testLockClient_SameThreads() throws InterruptedException {

        final AtomicReference<AmazonDynamoDBLockClient> ref1 = new AtomicReference<>();
        final AtomicReference<AmazonDynamoDBLockClient> ref2 = new AtomicReference<>();

        final Thread t1 = new Thread(() -> {
            ref1.getAndSet(this.beanFactory.getBean(AmazonDynamoDBLockClient.class));
            ref2.getAndSet(this.beanFactory.getBean(AmazonDynamoDBLockClient.class));
        });
        t1.start();
        t1.join();

        final AmazonDynamoDBLockClient lockClient1 = ref1.get();
        final AmazonDynamoDBLockClient lockClient2 = ref2.get();

        assertNotNull(lockClient1);
        assertNotNull(lockClient2);
        assertSame(lockClient1, lockClient2);
    }

    @Test
    public void testLockClient_DifferentThreads() throws InterruptedException {

        final AtomicReference<AmazonDynamoDBLockClient> ref1 = new AtomicReference<>();
        final AtomicReference<AmazonDynamoDBLockClient> ref2 = new AtomicReference<>();

        final Thread t1 = new Thread(() -> ref1.getAndSet(this.beanFactory.getBean(AmazonDynamoDBLockClient.class)));
        final Thread t2 = new Thread(() -> ref2.getAndSet(this.beanFactory.getBean(AmazonDynamoDBLockClient.class)));
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        final AmazonDynamoDBLockClient lockClient1 = ref1.get();
        final AmazonDynamoDBLockClient lockClient2 = ref2.get();

        assertNotNull(lockClient1);
        assertNotNull(lockClient2);
        assertNotSame(lockClient1, lockClient2);
    }

    @Test
    public void testLock_BetweenTwoThreads() throws InterruptedException {

        final AtomicReference<LockItem> ref = new AtomicReference<>();
        final AtomicInteger i = new AtomicInteger(0);
        final String pKey = UUID.randomUUID().toString();
        final String sKey = UUID.randomUUID().toString();

        final Thread t1 = new Thread(() -> {

            final AmazonDynamoDBLockClient client = this.beanFactory.getBean(AmazonDynamoDBLockClient.class);
            synchronized (ref) {
                try {
                    final LockItem lock = client.acquireLock(AcquireLockOptions
                            .builder(pKey)
                            .withSortKey(sKey)
                            .withDeleteLockOnRelease(true)
                            .build());
                    assertNotNull(lock);
                    assertTrue(client.hasLock(pKey, Optional.of(sKey)));
                    ref.set(lock);
                    i.getAndIncrement();
                } catch (BeansException | LockNotGrantedException | InterruptedException e) {
                    //
                }
                ref.notify();
            }

            synchronized (ref) {
                while (i.get() < 2) {
                    try {
                        ref.wait(500);
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }

            final LockItem lock = ref.get();
            client.releaseLock(lock);
            assertNotNull(lock);
            assertFalse(client.hasLock(pKey, Optional.of(sKey)));
        });

        final Thread t2 = new Thread(() -> {

            final AmazonDynamoDBLockClient client = this.beanFactory.getBean(AmazonDynamoDBLockClient.class);
            synchronized (ref) {
                while (i.get() < 1) {
                    try {
                        ref.wait(500);
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }

            final LockItem lock = ref.get();
            assertNotNull(lock);
            assertFalse(client.hasLock(pKey, Optional.of(sKey)));
            i.getAndIncrement();

            synchronized(ref) {
                ref.notify();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
