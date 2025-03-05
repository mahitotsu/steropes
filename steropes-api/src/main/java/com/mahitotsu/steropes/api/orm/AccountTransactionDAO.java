package com.mahitotsu.steropes.api.orm;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionDAO extends JpaRepository<AccountTransaction, UUID> {

    Optional<AccountTransaction> findFirstByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(String branchNumber,
            String accountNumber);

    Stream<AccountTransaction> findByBranchNumberAndAccountNumberOrderBySequenceNumberDesc(String branchNumber,
            String accountNumber);
}
