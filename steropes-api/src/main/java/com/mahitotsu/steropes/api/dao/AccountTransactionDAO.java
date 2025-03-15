package com.mahitotsu.steropes.api.dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionDAO extends JpaRepository<AccountTransactionRecord, UUID> {
    
    Optional<AccountTransactionRecord> findFirstByAccountOrderBySequenceNumberDesc(AccountRecord account);
}
