package com.mahitotsu.steropes.api.infra;

import java.time.Duration;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

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

    public <T> T doWithLock(final LockKey lockKey, final Execution<? extends T> task) throws RuntimeException {
        return this.doWithLock(LockKeys.sort(new LockKey[] { lockKey }), task);
    }

    public <T> T doWithLock(final SortedSet<LockKey> lockKeySet, final Execution<? extends T> task)
            throws RuntimeException {

        final long lockId = System.currentTimeMillis();
        log.debug("STARTED lock={}.{}", lockKeySet, lockId);

        final List<Lock> locks = lockKeySet.stream().map(l -> this.lockRegistry.obtain(l)).collect(Collectors.toList());
        final ListIterator<Lock> i = locks.listIterator();

        try {
            final long timeout = (this.lockTimeout == null ? DEFAULT_LOCK_TIMEOUT_MILLIS : this.lockTimeout.toMillis());
            final long limit = System.currentTimeMillis() + timeout;
            while (i.hasNext()) {
                final long t = limit - System.currentTimeMillis();
                if (t >= 0) {
                    if (i.next().tryLock(t, TimeUnit.MILLISECONDS) == false) {
                        i.previous();
                        break;
                    }
                } else {
                    break;
                }
            }
            log.debug("ACQUIRED lock={}.{}", locks, lockId);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted while attempting to acquire lock for key: " + lockKeySet,
                    e);
        }

        if (i.hasNext()) {
            throw new RuntimeException("Failed to acquire lock for key: " + lockKeySet);
        }

        try {
            return task.execute();
        } catch (RuntimeException e) {
            log.error("Task execution failed.", e);
            throw e;
        } finally {
            while (i.hasPrevious()) {
                i.previous().unlock();
                log.debug("RELEASED lock={}.{}", lockKeySet, lockId);
            }
        }
    }
}
