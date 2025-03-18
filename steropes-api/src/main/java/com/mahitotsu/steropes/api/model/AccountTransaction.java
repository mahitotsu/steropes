package com.mahitotsu.steropes.api.model;

import java.math.BigDecimal;

public interface AccountTransaction {
    
    int getSequenceNumber();

    BigDecimal getAmount();

    BigDecimal getNewBalance();
}
