package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mahitotsu.steropes.api.infra.LockTemplate;
import com.mahitotsu.steropes.api.orm.Account;
import com.mahitotsu.steropes.api.orm.AccountDAO;
import com.mahitotsu.steropes.api.orm.AccountTransactionDAO;

@Repository
public class AccountRepository {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private AccountTransactionDAO accountTransactionDAO;

    @Autowired
    private LockTemplate lockTemplate;

    @Transactional
    public Account openAccount(final String branchNumber, final BigDecimal maxBalance) {

        return this.lockTemplate.doWithLock("OPEN_ACCOUNT." + branchNumber,
                () -> {
                    final int lastAccountNumber = this.accountDAO
                            .findFirstByBranchNumberOrderByAccountNumberDesc(branchNumber)
                            .map(a -> Integer.parseInt(a.getAccountNumber())).orElse(0);
                    final Account newAccount = new Account(branchNumber, String.format("%07d", lastAccountNumber + 1),
                            maxBalance);
                    this.accountDAO.save(newAccount);
                    return this.accountDAO.findById(newAccount.getId()).get();
                });
    }

    @Transactional(readOnly = true)
    public Account getAccount(final String branchNumber, final String accountNumber) {

        return this.accountDAO.findByBranchNumberAndAccountNumber(branchNumber, accountNumber).orElse(null);
    }
}
