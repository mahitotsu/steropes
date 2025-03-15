package com.mahitotsu.steropes.api.model.impl;

import com.mahitotsu.steropes.api.infra.LockTemplate.LockRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LockRequests {

    public static LockRequest branchLock(final String branchNumber) {
        return LockRequest.builder().pKey("BRANCH_LOCK").sKey(branchNumber).build();
    }

    public static LockRequest accountLock(final String branchNumber, final String accountNumber) {
        return LockRequest.builder().pKey("ACCOUNT_LOCK").sKey(branchNumber + accountNumber).build();
    }
}
