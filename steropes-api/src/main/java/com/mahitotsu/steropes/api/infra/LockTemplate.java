package com.mahitotsu.steropes.api.infra;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.support.locks.LockRegistry;

public class LockTemplate {

    private static final int DEFAULT_LOCK_TIMEOUT_MILLIS = 5000;

    public LockTemplate(final LockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;
    }

    private LockRegistry lockRegistry;

    private Duration lockTimeout;

    public void setLockTimeout(final Duration lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public <T> T doWithLock(final String lockKey, final Callable<? extends T> task) {

        final Lock lock = this.lockRegistry.obtain(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(this.lockTimeout == null ? DEFAULT_LOCK_TIMEOUT_MILLIS
                    : this.lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while attempting to acquire lock for key: " + lockKey,
                    e);
        }

        if (locked == false) {
            throw new RuntimeException("Failed to acquire lock for key: " + lockKey
                    + " within timeout: " + this.lockTimeout.toMillis() + " ms.");
        }

        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(
                    "An unexpected error occurred while executing the task with lock for key: " + lockKey, e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
