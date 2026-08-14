package com.chatboxai.auth_service.dto.response;

import java.time.Instant;

public record AccountResponseDTO(
        Long id,
        String email,
        String fullName,
        String role,
        String status,
        Instant createdAt
) {
}
