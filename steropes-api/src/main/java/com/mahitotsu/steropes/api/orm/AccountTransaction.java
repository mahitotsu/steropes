package com.mahitotsu.steropes.api.orm;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name="AccountTransaction")
@Table(name="account_transaction")
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.NONE)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AccountTransaction extends EntityBase {

    @Column(name="branch_number", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Pattern(regexp = "^[0-9]{3}$")
    private String branchNumber;

    @Column(name="account_number", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Pattern(regexp = "^[0-9]{7}$")
    private String accountNubmer;

    @Column(name="sequence_number", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Min(1)
    private Integer sequenceNumber;

    @Column(name="amount", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Digits(integer = 13, fraction = 2)
    @DecimalMin(value = "-9999999999999.99", inclusive = true)
    @DecimalMax(value = "9999999999999.99", inclusive = true)
    private BigDecimal amount;

    @Column(name="new_balance", nullable = false, insertable = true, updatable = false)
    @NotNull
    @Digits(integer = 13, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    @DecimalMax(value = "9999999999999.99", inclusive = true)
    private BigDecimal newBalance;
}
