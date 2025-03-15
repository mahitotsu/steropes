package com.mahitotsu.steropes.api.dao;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "AccountTransaction")
@Table(name = "account_transactions")
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AccountTransactionRecord {

    @GeneratedValue
    @Id
    @Column(name = "transaction_id", unique = true, nullable = false, insertable = false, updatable = false)
    private UUID transactionId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Valid
    private AccountRecord account;

    @Column(name = "sequence_number", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Min(1)
    private Integer sequenceNumber;

    @Column(name = "amount", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;

    @Column(name = "new_balance", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Digits(integer = 16, fraction = 2)
    private BigDecimal newBalane;
}
