package com.mahitotsu.steropes.api.infra;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.support.locks.LockRegistry;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AcquireLockOptions.AcquireLockOptionsBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.LockItem;

public class AmazonDynamoDBLockRegistry implements LockRegistry {

    public AmazonDynamoDBLockRegistry(final AmazonDynamoDBLockClient lockClient) {
        this.lockClient = lockClient;
        this.entryCounts = new ConcurrentHashMap<>();
    }

    private AmazonDynamoDBLockClient lockClient;

    private ConcurrentMap<LockKey, AtomicInteger> entryCounts;

    @Override
    public Lock obtain(final Object lockKey) {
        return new AmazonDynamoDBLock(this.lockClient, lockKey, this.entryCounts);
    }

    private static class AmazonDynamoDBLock implements Lock {

        public AmazonDynamoDBLock(final AmazonDynamoDBLockClient lockClient, final Object lockKey,
                final ConcurrentMap<LockKey, AtomicInteger> entryCounts) {

            this.lockClient = lockClient;
            this.lockKey = LockKey.class.cast(lockKey);
            this.key = this.lockKey.getKey();
            this.scope = this.lockKey.getScope();
            this.entryCounts = entryCounts;
            this.entryCounts.computeIfAbsent(this.lockKey, k -> new AtomicInteger());

            this.scope = Optional.ofNullable(this.lockKey.getScope()).orElse("_");

            this.currentLock = null;
        }

        private AmazonDynamoDBLockClient lockClient;

        private LockKey lockKey;

        private String key;

        private String scope;

        private LockItem currentLock;

        private ConcurrentMap<LockKey, AtomicInteger> entryCounts;

        private AcquireLockOptionsBuilder baseOptions() {

            final AcquireLockOptionsBuilder builder = AcquireLockOptions.builder(this.key);
            if (this.scope != null) {
                builder.withSortKey(this.scope);
            }
            builder.withReentrant(true);
            return builder;
        }

        @Override
        public void lock() {

            try {
                this.lockInterruptibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock acquisition was interrupted", e);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.currentLock = this.lockClient.acquireLock(this.baseOptions().build());
            this.entryCounts.get(this.lockKey).incrementAndGet();
            System.out.println(this.entryCounts.get(this.lockKey).get() + ", " + this.lockKey);
        }

        @Override
        public boolean tryLock() {

            try {
                this.currentLock = this.lockClient.tryAcquireLock(this.baseOptions().build()).orElse(null);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock acquisition was interrupted", e);
            }

            if (this.currentLock != null) {
                this.entryCounts.get(this.lockKey).incrementAndGet();
                System.out.println(this.entryCounts.get(this.lockKey).get() + ", " + this.lockKey);
            }
            return this.currentLock != null;
        }

        @Override
        public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {

            try {
                this.currentLock = this.lockClient
                        .tryAcquireLock(this.baseOptions()
                                .withAdditionalTimeToWaitForLock(time)
                                .withTimeUnit(unit)
                                .build())
                        .orElse(null);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock acquisition was interrupted", e);
            }

            if (this.currentLock != null) {
                this.entryCounts.get(this.lockKey).incrementAndGet();
                System.out.println(this.entryCounts.get(this.lockKey).get() + ", " + this.lockKey);
            }
            return this.currentLock != null;
        }

        @Override
        public void unlock() {

            if (this.currentLock == null) {
                throw new IllegalMonitorStateException("Attempting to unlock without holding the lock");
            }
            if (this.entryCounts.get(this.lockKey).decrementAndGet() > 0) {
                System.out.println(this.entryCounts.get(this.lockKey).get() + ", " + this.lockKey);
                System.out.println("not-release");
                return;
            }

            System.out.println(this.entryCounts.get(this.lockKey).get() + ", " + this.lockKey);
            System.out.println("release");
            this.lockClient.releaseLock(this.currentLock);
            this.currentLock = null;
            this.entryCounts.remove(this.lockKey);
            System.out.println(this.entryCounts);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Conditions are not supported for DynamoDB locks");
        }
    }
}
