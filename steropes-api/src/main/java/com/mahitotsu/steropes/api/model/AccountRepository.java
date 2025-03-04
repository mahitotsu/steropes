package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.orm.Account;
import com.mahitotsu.steropes.api.orm.AccountDAO;
import com.mahitotsu.steropes.api.orm.AccountTransactionDAO;

import jakarta.transaction.Transactional;

@Repository
public class AccountRepository {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private AccountTransactionDAO accountTransactionDAO;

    @Autowired
    private LockTemplate lockTemplate;

    @Transactional
    @DsqlRetry
    public Account openAccount(final String branchCode, final BigDecimal maxBalance) {

        final Account account = this.lockTemplate.doWithLock("OPEN_ACCOUNT",
                () -> this.accountDAO.findFirstByBranchNumberOrderByAccountNumberDesc(branchCode).orElse(null));
        return account;
    }
}
