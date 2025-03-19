package com.mahitotsu.steropes.api.infra;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
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
import com.amazonaws.services.dynamodbv2.model.LockNotGrantedException;

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

    public <T> T execute(final LockRequest request,
            final Supplier<T> action) {
        return this.execute(request, _ -> action.get());
    }

    public <T> T execute(final Collection<LockRequest> requests,
            final Supplier<T> action) {
        return this.execute(requests, _ -> action.get());
    }

    public <T> T execute(final LockRequest request,
            final Function<LockItem, T> action) {
        return this.execute(Collections.singleton(request), items -> action.apply(items.getFirst()));
    }

    public <T> T execute(final Collection<LockRequest> requests,
            final Function<SequencedCollection<LockItem>, T> action) {

        if (action == null) {
            return null;
        }

        if (requests == null || requests.isEmpty()) {
            return action.apply(null);
        }

        final LockClientHolder holder = this.getLockClient();
        final AmazonDynamoDBLockClient lockClient = holder.getLockClient();
        final SortedSet<LockRequest> reqSet = new TreeSet<>(requests);

        final Deque<LockItem> lockStack = new ArrayDeque<>(reqSet.size());
        final Map<LockItem, Boolean> lockMap = new HashMap<>();

        try {
            for (final Iterator<LockRequest> i = reqSet.iterator(); i.hasNext();) {
                final LockRequest request = i.next();
                if (request == null) {
                    continue;
                }

                final AcquireLockOptionsBuilder builder = AcquireLockOptions
                        .builder(request.pKey)
                        .withTimeUnit(TimeUnit.MILLISECONDS)
                        .withReentrant(true)
                        .withRefreshPeriod(100L)
                        .withDeleteLockOnRelease(true);
                if (request.getSKey() != null) {
                    builder.withSortKey(request.sKey);
                }
                final boolean requireNewLock = (lockClient.hasLock(request.getPKey(),
                        Optional.ofNullable(request.getSKey())) == false);
                final LockItem lockItem = lockClient.acquireLock(builder.build());
                lockStack.push(lockItem);
                lockMap.put(lockItem, requireNewLock);
            }

            return action.apply(Collections.unmodifiableSequencedCollection(lockStack));

        } catch (InterruptedException e) {
            throw new IllegalStateException("The lock could not be acquired.", e);
        } finally {

            for (final Iterator<LockItem> i = lockStack.reversed().iterator(); i.hasNext();) {
                final LockItem lockItem = i.next();
                final Boolean isNewLock = lockMap.get(lockItem);
                if (isNewLock != null & Boolean.TRUE.equals(isNewLock)) {
                    lockClient.releaseLock(lockItem);
                }
            }
            this.cleanupLockClient(holder);
        }
    }
}
