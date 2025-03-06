package com.mahitotsu.steropes.api.infra;

import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.util.comparator.Comparators;

import lombok.NonNull;
import lombok.Value;

public class LockKeys {

    @Value
    private static class LockKeyValue implements LockKey {
        @NonNull
        private String key;
        @NonNull
        private String scope;
    }

    private static Comparator<LockKey> comparator = Comparator
            .comparing(LockKey::getKey, Comparators.nullsHigh(String::compareTo))
            .thenComparing(LockKey::getScope, Comparators.nullsHigh(String::compareTo));

    public static SortedSet<LockKey> sort(final LockKey[] lockKeys) {
        return Arrays.stream(lockKeys).collect(Collectors.toCollection(() -> new TreeSet<LockKey>(comparator)));
    }

    public static LockKey forBranch(final String branchNumber) {
        return new LockKeyValue("BRANCH", branchNumber);
    }

    public static LockKey forAccount(final String branchNumber, final String accountNumber) {
        return new LockKeyValue("ACCOUNT", branchNumber + accountNumber);
    }
}
