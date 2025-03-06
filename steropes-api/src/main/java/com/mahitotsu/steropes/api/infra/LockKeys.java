package com.mahitotsu.steropes.api.infra;

import lombok.Value;

public class LockKeys {
   
    @Value
    private static class LockKeyValue implements LockKey {
        private String key;
        private String scope;
    }

    public static LockKey forBranch(final String branchNumber) {
        return new LockKeyValue("BRANCH", branchNumber);
    }

    public static LockKey forAccount(final String branchNumber, final String accountNumber) {
        return new LockKeyValue("ACCOUNT", branchNumber + accountNumber);
    }
}
