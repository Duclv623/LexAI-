package com.chatboxai.auth_service.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AccountResponse account
) {
}
