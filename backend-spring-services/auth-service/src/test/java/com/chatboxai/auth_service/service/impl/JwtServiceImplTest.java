package com.chatboxai.auth_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.chatboxai.auth_service.config.JwtKeyConfig;
import com.chatboxai.auth_service.entity.Account;

@SuppressWarnings("unchecked")
class JwtServiceImplTest {

    private static final long TTL = 86400;

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final JwtServiceImpl service = newService();

    private JwtServiceImpl newService() {
        var boot = new JwtKeyConfig("");
        var key = boot.rsaKey();
        return new JwtServiceImpl(
                boot.jwtEncoder(boot.jwkSource(key)), key, redisTemplate,
                "http://localhost:8081", TTL);
    }

    private static Account account() {
        var account = new Account();
        account.setEmail("levan@example.com");
        ReflectionTestUtils.setField(account, "id", 42L);
        return account;
    }

    @Test
    @DisplayName("Cấp token -> lưu vào Redis dưới key jwt:{accountId} để gateway đối chiếu")
    void tokenIsStoredForTheAccount() {
        when(redisTemplate.opsForValue()).thenReturn(values);

        String token = service.createToken(account());

        verify(values).set(eq("jwt:42"), eq(token), eq(Duration.ofSeconds(TTL)));
    }

    @Test
    @DisplayName("TTL của key khớp hạn token, key không sống lâu hơn thứ nó cho phép")
    void keyTtlMatchesTokenLifetime() {
        when(redisTemplate.opsForValue()).thenReturn(values);

        service.createToken(account());

        verify(values).set(eq("jwt:42"), org.mockito.ArgumentMatchers.anyString(),
                eq(Duration.ofSeconds(service.getExpirationSeconds())));
    }

    @Test
    @DisplayName("Thu hồi -> xoá key, gateway sẽ từ chối token đó ngay lần gọi sau")
    void revokeDeletesTheKey() {
        service.revoke(42L);

        verify(redisTemplate).delete("jwt:42");
    }

    @Test
    @DisplayName("Đăng nhập lần hai ghi đè key -> chỉ token mới nhất còn hiệu lực")
    void secondLoginOverwritesTheStoredToken() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        var account = account();

        String first = service.createToken(account);
        String second = service.createToken(account);

        assertThat(second).isNotEqualTo(first);
        verify(values).set(eq("jwt:42"), eq(first), eq(Duration.ofSeconds(TTL)));
        verify(values).set(eq("jwt:42"), eq(second), eq(Duration.ofSeconds(TTL)));
    }
}
