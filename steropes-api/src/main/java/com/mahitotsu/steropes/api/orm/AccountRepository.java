package com.mahitotsu.steropes.api.orm;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

}
