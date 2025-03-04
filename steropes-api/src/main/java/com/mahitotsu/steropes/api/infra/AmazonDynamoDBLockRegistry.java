package com.mahitotsu.steropes.api.infra;

import java.util.concurrent.TimeUnit;
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
    }

    private AmazonDynamoDBLockClient lockClient;

    @Override
    public Lock obtain(final Object lockKey) {
        return new AmazonDynamoDBLock(this.lockClient, lockKey);
    }

    private static class AmazonDynamoDBLock implements Lock {

        public AmazonDynamoDBLock(final AmazonDynamoDBLockClient lockClient, final Object lockKey) {

            this.lockClient = lockClient;

            final String[] lockKeys = String.class.cast(lockKey).split("\\.");
            this.lockKey = lockKeys[0];
            this.scope = lockKeys.length > 1 ? lockKeys[1] : "_";

            this.currentLock = null;
        }

        private AmazonDynamoDBLockClient lockClient;

        private String lockKey;

        private String scope;

        private LockItem currentLock;

        private AcquireLockOptionsBuilder baseOptions() {

            final AcquireLockOptionsBuilder builder = AcquireLockOptions.builder(this.lockKey);
            if (this.scope != null) {
                builder.withSortKey(this.scope);
            }
            return builder;
        }

        @Override
        public void lock() {

            try {
                this.currentLock = this.lockClient.acquireLock(this.baseOptions().build());
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException("Lock acquisition was interrupted", e);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.currentLock = this.lockClient.acquireLock(this.baseOptions().build());
        }

        @Override
        public boolean tryLock() {

            try {
                this.currentLock = this.lockClient.tryAcquireLock(this.baseOptions().build()).orElse(null);
            } catch (final InterruptedException e) {
                this.currentLock = null;
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
                this.currentLock = null;
            }
            return this.currentLock != null;
        }

        @Override
        public void unlock() {

            if (this.currentLock == null) {
                throw new IllegalMonitorStateException("Attempting to unlock without holding the lock");
            }

            this.lockClient.releaseLock(this.currentLock);
            this.currentLock = null;
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Conditions are not supported for DynamoDB locks");
        }
    }
}
