package com.chatboxai.api_gateway.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenValidationService {

    // ném JwtException khi chữ ký sai, token hết hạn, hoặc token đã bị thu hồi
    Jwt validate(String token);
}
