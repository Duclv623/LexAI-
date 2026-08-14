package com.chatboxai.api_gateway.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenValidationService {

    // throws JwtException when the signature is bad, the token expired, or it was revoked
    Jwt validate(String token);
}
