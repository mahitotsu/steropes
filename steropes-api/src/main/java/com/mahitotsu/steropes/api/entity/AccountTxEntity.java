package com.mahitotsu.steropes.api.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "AccountTx")
@Table(name = "account_transactions")
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountTxEntity {

    public static enum TxStatus {
        REGISTERED, ACCEPTED, REJECTED,
    }

    @Id
    @Column(name = "tx_id", nullable = false, unique = true, insertable = false, updatable = false)
    private UUID txId;

    @Column(name = "tx_timestamp", nullable = false, insertable = false, updatable = false)
    private ZonedDateTime txTimestamp;

    @Column(name = "branch_number", nullable = false, insertable = true, updatable = false)
    private String branchNumber;

    @Column(name = "account_number", nullable = false, insertable = true, updatable = false)
    private String accountNumber;

    @Column(name = "amount", nullable = false, insertable = true, updatable = false)
    private BigDecimal amount;

    @Column(name = "tx_status", nullable = false, insertable = false, updatable = true)
    @Setter(AccessLevel.PUBLIC)
    private TxStatus txStatus;

    @Column(name = "tx_sequence", nullable = false, insertable = false, updatable = true)
    @Setter(AccessLevel.PUBLIC)
    private int txSequence;

    @Column(name = "new_balance", nullable = false, insertable = false, updatable = true)
    @Setter(AccessLevel.PUBLIC)
    private BigDecimal newBalance;
}
