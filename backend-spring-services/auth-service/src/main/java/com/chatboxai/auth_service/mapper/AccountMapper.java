package com.chatboxai.auth_service.mapper;

import org.springframework.stereotype.Component;

import com.chatboxai.auth_service.dto.response.AccountResponseDTO;
import com.chatboxai.auth_service.entity.Account;

@Component
public class AccountMapper {

    public static AccountResponseDTO toResponse(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getRole(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
