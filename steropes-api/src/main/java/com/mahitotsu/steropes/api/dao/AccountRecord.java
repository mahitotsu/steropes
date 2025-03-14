package com.mahitotsu.steropes.api.dao;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Account")
@Table(name = "accounts")
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Setter(AccessLevel.PRIVATE)
public class AccountRecord {

    @GeneratedValue
    @Id
    @Column(name = "account_id", unique = true, nullable = false, insertable = false, updatable = false)
    private UUID accountId;;

    @Column(name = "branch_number", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Size(min = 3, max = 3)
    @Pattern(regexp = "[0-9]+")
    private String branchNumber;

    @Column(name = "account_number", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Size(min = 7, max = 7)
    @Pattern(regexp = "[0-9]+")
    private String accountNumber;

    @Column(name = "max_balance", nullable = false, insertable = true, updatable = true)
    @NotNull
    @Min(0)
    @Digits(integer = 16, fraction = 2)
    @Setter(AccessLevel.PUBLIC)
    private BigDecimal maxBalance;
}
