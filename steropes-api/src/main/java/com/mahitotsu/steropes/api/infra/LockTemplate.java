package com.mahitotsu.steropes.api.infra;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.support.locks.LockRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LockTemplate {

    public static interface Execution<T> {
        T execute() throws RuntimeException;
    }

    private static final int DEFAULT_LOCK_TIMEOUT_MILLIS = 5000;

    public LockTemplate(final LockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;
    }

    private LockRegistry lockRegistry;

    private Duration lockTimeout;

    public void setLockTimeout(final Duration lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public <T> T doWithLock(final LockKey lockKey, final Execution<? extends T> task) throws RuntimeException{

        final long lockId = System.currentTimeMillis();
        log.debug("STARTED lock={}.{}", lockKey, lockId);

        final Lock lock = this.lockRegistry.obtain(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(this.lockTimeout == null ? DEFAULT_LOCK_TIMEOUT_MILLIS
                    : this.lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("ACQUIRED lock={}.{}", lockKey, lockId);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while attempting to acquire lock for key: " + lockKey,
                    e);
        }

        if (locked == false) {
            throw new RuntimeException("Failed to acquire lock for key: " + lockKey
                    + " within timeout: " + this.lockTimeout.toMillis() + " ms.");
        }

        try {
            return task.execute();
        } finally {
            if (locked) {
                lock.unlock();
                log.debug("RELEASED lock={}.{}", lockKey, lockId);
            }
        }
    }
}
