package com.chatboxai.auth_service.dto.response;

public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        AccountResponseDTO account
) {
}
