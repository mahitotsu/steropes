package com.mahitotsu.steropes.api.dao;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionDAO extends JpaRepository<AccountTransactionRecord, UUID> {
    
    Optional<AccountTransactionRecord> findFirstByAccountOrderBySequenceNumberDesc(AccountRecord account);

    Stream<AccountTransactionRecord> findByAccountOrderBySequenceNumberDesc(AccountRecord account);
}
