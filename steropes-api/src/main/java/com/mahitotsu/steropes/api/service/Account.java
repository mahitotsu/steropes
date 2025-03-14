package com.mahitotsu.steropes.api.service;

import java.math.BigDecimal;

public interface Account {

    String getBranchNumber();

    String getAccountNumber();

    BigDecimal getMaxBalance();
}
