package com.mahitotsu.steropes.api.infra;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LockTemplate {

    private static final int DEFAULT_LOCK_TIMEOUT_MILLIS = 5000;

    public LockTemplate(final LockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;
    }

    private LockRegistry lockRegistry;

    private Duration lockTimeout;

    private Logger logger = LoggerFactory.getLogger(LockTemplate.class);

    public void setLockTimeout(final Duration lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public <T> T doWithLock(final String lockKey, final Callable<? extends T> task) {

        final long lockId = System.currentTimeMillis();
        this.logger.info("STARTED lock={}.{}", lockKey, lockId);

        final Lock lock = this.lockRegistry.obtain(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(this.lockTimeout == null ? DEFAULT_LOCK_TIMEOUT_MILLIS
                    : this.lockTimeout.toMillis(), TimeUnit.MILLISECONDS);
            this.logger.info("ACQUIRED lock={}.{}", lockKey, lockId);
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
            this.logger.debug("ERROR lock={}.{}", lockKey, lockId);
            this.logger.error("An unexpected error occured while executing the task with lock.", e);
            throw new RuntimeException(
                    "An unexpected error occurred while executing the task with lock for key: " + lockKey, e);
        } finally {
            if (locked) {
                lock.unlock();
                this.logger.info("RELEASED lock={}.{}", lockKey, lockId);
            }
        }
    }
}
