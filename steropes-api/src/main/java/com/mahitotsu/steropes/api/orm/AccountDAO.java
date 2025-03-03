package com.mahitotsu.steropes.api.orm;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountDAO extends JpaRepository<Account, UUID> {

}
