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

    // auth-service là bên ghi key này, xem JwtServiceImpl.TOKEN_KEY_PREFIX
    private static final String TOKEN_KEY_PREFIX = "jwt:";

    private final JwtDecoder jwtDecoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Jwt validate(String token) {
        // bước 1: chữ ký, hạn dùng và issuer, kiểm tra tại chỗ bằng public key lấy từ jwks
        Jwt jwt = jwtDecoder.decode(token);

        // bước 2: đây có còn là token mà auth-service đã cấp cho tài khoản đó không
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
            // fail CLOSED, ngược với rate limit filter. cho request đi qua trong lúc không đọc được
            // danh sách thu hồi sẽ khiến mọi token đã bị thu hồi hoạt động trở lại
            log.error("Redis không khả dụng — từ chối mọi token cho tới khi khôi phục", e);
            throw new JwtException("Không kiểm tra được trạng thái token", e);
        }
    }
}
