package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;

public interface AccountRepository {

    Account openAccount(String branchNumber, BigDecimal maxBalance);

    Account getAccount(String branchNumber, String accountNumber);
}
