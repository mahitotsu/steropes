package com.mahitotsu.steropes.api.infra;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AcquireLockOptions.AcquireLockOptionsBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.LockItem;
import com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Component
public class LockTemplate {

    private static final ThreadLocal<AmazonDynamoDBLockClient> lockClientHolder = new ThreadLocal<>();

    @Data
    @Getter
    @Setter(AccessLevel.NONE)
    @Builder
    public static class LockRequest implements Comparable<LockRequest> {
        private String pKey;
        private String sKey;

        @Override
        public int compareTo(final LockRequest o) {
            if (this.pKey.equals(o.pKey)) {
                return this.sKey.compareTo(o.sKey);
            }
            return this.pKey.compareTo(o.pKey);
        }
    }

    @Data
    @Getter
    @Setter(AccessLevel.NONE)
    @Builder
    private static class LockClientHolder {
        private AmazonDynamoDBLockClient lockClient;
        boolean created;
    }

    @Autowired
    private BeanFactory beanFactory;

    private LockClientHolder getLockClient() {

        final AmazonDynamoDBLockClient hold = lockClientHolder.get();
        if (hold != null) {
            return LockClientHolder.builder().lockClient(hold).created(false).build();
        }
        final AmazonDynamoDBLockClient newClient = this.beanFactory.getBean(
                AmazonDynamoDBLockClient.class);
        lockClientHolder.set(newClient);
        return LockClientHolder.builder().lockClient(newClient).created(true).build();
    }

    private void cleanupLockClient(final LockClientHolder holder) {

        if (holder.isCreated() == false) {
            return;
        }

        try {
            lockClientHolder.remove();
            holder.getLockClient().close();
        } catch (IOException e) {
            //
        }
    }

    public <T> T doWithLocks(final Collection<LockRequest> requests, final Supplier<T> action) {
        return this._doWithLocks(new TreeSet<>(requests), action);
    }

    private <T> T _doWithLocks(final SortedSet<LockRequest> requests, final Supplier<T> action) {
        if (requests.isEmpty()) {
            return this.doWithLock(null, action);
        } else if (requests.size() == 1) {
            return this.doWithLock(requests.iterator().next(), action);
        } else {
            requests.removeFirst();
            return this._doWithLocks(requests, action);
        }
    }

    public <T> T doWithLock(final LockRequest request, final Supplier<T> action) {
        return this.doWithLock(request, _ -> action.get());
    }

    public <T> T doWithLock(final LockRequest request, final Function<LockItem, T> action) {

        if (request == null) {
            return action.apply(null);
        }

        final LockClientHolder holder = this.getLockClient();
        final AmazonDynamoDBLockClient lockClient = holder.getLockClient();
        LockItem lockItem = null;
        boolean requireNewLock = true;

        try {

            final AcquireLockOptionsBuilder builder = AcquireLockOptions
                    .builder(request.pKey)
                    .withTimeUnit(TimeUnit.MILLISECONDS)
                    .withReentrant(true)
                    .withRefreshPeriod(100L)
                    .withDeleteLockOnRelease(true);
            if (request.getSKey() != null) {
                builder.withSortKey(request.sKey);
            }

            requireNewLock = (lockClient.hasLock(request.getPKey(),
                    Optional.ofNullable(request.getSKey())) == false);
            lockItem = lockClient.acquireLock(builder.build());

            return action != null ? action.apply(lockItem) : null;

        } catch (InterruptedException e) {
            throw new LockCurrentlyUnavailableException(e);
        } finally {
            if (lockItem != null && requireNewLock) {
                lockClient.releaseLock(lockItem);
                this.cleanupLockClient(holder);
            }
        }
    }
}
