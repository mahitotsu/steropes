package com.mahitotsu.steropes.api.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.TestMain;
import com.mahitotsu.steropes.api.infra.LockTemplate.LockRequest;

public class LockTemplateTest extends TestMain {

    @Autowired
    private LockTemplate lockTemplate;

    @Test
    public void testLockOwnerName_SameThreads() throws InterruptedException {

        final Callable<String> action = () -> this.lockTemplate.execute(
                LockRequest.builder().pKey(UUID.randomUUID().toString()).sKey(UUID.randomUUID().toString()).build(),
                li -> li.getOwnerName());

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final List<String> results = executor.invokeAll(Arrays.asList(action, action)).stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return null;
            }
        }).collect(Collectors.toList());

        final String on1 = results.get(0);
        final String on2 = results.get(1);
        assertNotNull(on1);
        assertNotNull(on2);
        assertEquals(on1, on2);
    }

    @Test
    public void testLockOwnerName_DifferentThreads() throws InterruptedException {

        final Callable<String> action = () -> this.lockTemplate.execute(
                LockRequest.builder().pKey(UUID.randomUUID().toString()).sKey(UUID.randomUUID().toString()).build(),
                li -> li.getOwnerName());

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final List<String> results = executor.invokeAll(Arrays.asList(action, action)).stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return null;
            }
        }).collect(Collectors.toList());

        final String on1 = results.get(0);
        final String on2 = results.get(1);
        assertNotNull(on1);
        assertNotNull(on2);
        assertNotEquals(on1, on2);
    }

    @Test
    public void testReentrantLock() {

        final LockRequest req = LockRequest.builder().pKey(UUID.randomUUID().toString())
                .sKey(UUID.randomUUID().toString()).build();

        this.lockTemplate.execute(req, (l1) -> {
            assertFalse(l1.isExpired());
            this.lockTemplate.execute(req, (l2) -> {
                assertFalse(l1.isExpired());
                assertFalse(l2.isExpired());
                assertSame(l1, l2);
                return true;
            });
            assertFalse(l1.isExpired());
            return true;
        });
    }

    @Test
    public void testNestedLock_NotReentrantLock() {

        final LockRequest req1 = LockRequest.builder().pKey(UUID.randomUUID().toString())
                .sKey(UUID.randomUUID().toString()).build();
        final LockRequest req2 = LockRequest.builder().pKey(UUID.randomUUID().toString())
                .sKey(UUID.randomUUID().toString()).build();

        this.lockTemplate.execute(req1, (l1) -> {
            assertFalse(l1.isExpired());
            this.lockTemplate.execute(req2, (l2) -> {
                assertFalse(l1.isExpired());
                assertFalse(l2.isExpired());
                assertNotSame(l1, l2);
                assertNotEquals(l1, l2);
                return true;
            });
            assertFalse(l1.isExpired());
            return true;
        });
    }

    @Test
    public void testMultiLocks() {

        final LockRequest req1 = LockRequest.builder().pKey(UUID.randomUUID().toString())
                .sKey(UUID.randomUUID().toString()).build();
        final LockRequest req2 = LockRequest.builder().pKey(UUID.randomUUID().toString())
                .sKey(UUID.randomUUID().toString()).build();

        this.lockTemplate.execute(Arrays.asList(req1, req2), (items) -> {
            assertEquals(2, items.size());
            assertFalse(items.getFirst().isExpired());
            assertFalse(items.getLast().isExpired());
            assertNotEquals(items.getFirst(), items.getLast());
            return true;
        });
    }
}
