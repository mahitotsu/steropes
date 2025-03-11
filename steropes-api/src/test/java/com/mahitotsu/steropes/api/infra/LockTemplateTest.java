package com.mahitotsu.steropes.api.infra;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.TestMain;
import com.mahitotsu.steropes.api.infra.LockTemplate.LockRequest;

public class LockTemplateTest extends TestMain {

    @Autowired
    private LockTemplate lockTemplate;

    @Test
    public void testLockOwnerName_SameThread() {

        final Callable<String> action = () -> this.lockTemplate.execute(
                LockRequest.builder().pKey(UUID.randomUUID().toString()).sKey(UUID.randomUUID().toString()).build(),
                li -> li.getOwnerName());
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        final List<Future<String>> results = executor.invokeAll(null);
    }
}
