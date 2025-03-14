package com.mahitotsu.steropes.api.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDAO extends JpaRepository<AccountRecord, UUID> {

    Optional<AccountRecord> findFirstByBranchNumberOrderByAccountNumberDesc(String branchNumber);

    Optional<AccountRecord> findOneByBranchNumberAndAccountNumber(String branchNumber, String accountNumber);
}
