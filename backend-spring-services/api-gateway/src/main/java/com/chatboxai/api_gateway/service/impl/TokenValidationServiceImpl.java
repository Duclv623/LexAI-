package com.chatboxai.api_gateway.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import com.chatboxai.api_gateway.service.TokenValidationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenValidationServiceImpl implements TokenValidationService {

    private static final Logger log = LoggerFactory.getLogger(TokenValidationServiceImpl.class);

    // auth-service writes this key, see JwtServiceImpl.TOKEN_KEY_PREFIX
    private static final String TOKEN_KEY_PREFIX = "jwt:";

    private final JwtDecoder jwtDecoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Jwt validate(String token) {
        // step 1: signature, expiry and issuer, verified locally with the public key from the jwks
        Jwt jwt = jwtDecoder.decode(token);

        // step 2: is this still the token auth-service handed out for that account
        String current = currentToken(jwt.getSubject());
        if (!token.equals(current)) {
            throw new JwtException("Token đã bị thu hồi");
        }

        return jwt;
    }

    private String currentToken(String accountId) {
        try {
            return redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + accountId);
        } catch (DataAccessException e) {
            // fail CLOSED, unlike the rate limit filter. letting requests through when the
            // revocation list is unreachable would make every revoked token work again
            log.error("Redis không khả dụng — từ chối mọi token cho tới khi khôi phục", e);
            throw new JwtException("Không kiểm tra được trạng thái token", e);
        }
    }
}
