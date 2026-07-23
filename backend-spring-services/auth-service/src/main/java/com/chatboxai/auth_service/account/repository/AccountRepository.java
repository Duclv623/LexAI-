package com.chatboxai.auth_service.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatboxai.auth_service.account.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByEmail(String email);

    Optional<Account> findByEmail(String email);
}
