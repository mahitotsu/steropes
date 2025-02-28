package com.mahitotsu.steropes.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mahitotsu.steropes.api.entity.AccountTxEntity;

public interface AccountTxRepository extends JpaRepository<AccountTxEntity, UUID>{
    
}
