package com.mahitotsu.steropes.api.orm;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionDAO extends JpaRepository<AccountTransaction, UUID> {

}
