package com.chatboxai.auth_service.auth.dto;

import java.time.Instant;

import com.chatboxai.auth_service.account.entity.Account;

public record AccountResponse(
        Long id,
        String email,
        String fullName,
        String role,
        String status,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
