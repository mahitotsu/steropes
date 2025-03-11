package com.mahitotsu.steropes.api.infra;

import java.util.Optional;
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
    public static class LockRequest {
        private String pKey;
        private String sKey;
    }

    @Autowired
    private BeanFactory beanFactory;

    private AmazonDynamoDBLockClient getLockClient() {

        final AmazonDynamoDBLockClient hold = lockClientHolder.get();
        if (hold != null) {
            return hold;
        }
        final AmazonDynamoDBLockClient newClient = this.beanFactory.getBean(
                AmazonDynamoDBLockClient.class);
        lockClientHolder.set(newClient);
        return newClient;
    }

    public <T> T execute(final LockRequest request, final Supplier<? extends T> action) {
        return this.execute(request, (_) -> action.get());
    }

    public <T> T execute(final LockRequest request, final Function<LockItem, T> action) {

        final AmazonDynamoDBLockClient lockClient = this.getLockClient();
        LockItem lockItem = null;
        boolean requireNewLock = true;

        try {

            final AcquireLockOptionsBuilder builder = AcquireLockOptions
                    .builder(request.pKey)
                    .withTimeUnit(TimeUnit.MILLISECONDS)
                    .withReentrant(true)
                    .withRefreshPeriod(500L)
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
                lockClientHolder.remove();
            }
        }
    }
}
