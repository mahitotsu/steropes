package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;
import java.util.List;

public interface Account {

    String getBranchNumber();

    String getAccountNumber();

    BigDecimal getMaxBalance();

    BigDecimal getCurrentBalance();

    BigDecimal deposit(BigDecimal amount);

    BigDecimal withdraw(BigDecimal amount);

    BigDecimal transfer(Account destination, BigDecimal amount);

    List<AccountTransaction> getRecentTransactions();
}
