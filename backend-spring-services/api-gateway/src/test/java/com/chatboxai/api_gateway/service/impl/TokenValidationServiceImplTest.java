package com.chatboxai.api_gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@SuppressWarnings("unchecked")
class TokenValidationServiceImplTest {

    private static final String TOKEN = "token-cua-user";

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final TokenValidationServiceImpl service =
            new TokenValidationServiceImpl(jwtDecoder, redisTemplate);

    private static Jwt jwtOf(String token) {
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("42")
                .claim("role", "USER")
                .build();
    }

    private void redisHolds(String stored) {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("jwt:42")).thenReturn(stored);
    }

    @Test
    @DisplayName("Chữ ký hợp lệ và khớp token đang lưu -> cho qua")
    void validAndCurrentTokenPasses() {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwtOf(TOKEN));
        redisHolds(TOKEN);

        assertThat(service.validate(TOKEN).getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("Đã đăng xuất (Redis không còn key) -> từ chối dù chữ ký vẫn đúng")
    void loggedOutTokenIsRejected() {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwtOf(TOKEN));
        redisHolds(null);

        assertThatThrownBy(() -> service.validate(TOKEN))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("thu hồi");
    }

    @Test
    @DisplayName("Đăng nhập máy khác ghi đè -> token cũ hết hiệu lực ngay")
    void tokenReplacedByNewerLoginIsRejected() {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwtOf(TOKEN));
        redisHolds("token-cua-lan-dang-nhap-moi");

        assertThatThrownBy(() -> service.validate(TOKEN))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Chữ ký sai -> lỗi từ decoder bay ra, không hỏi Redis làm gì")
    void badSignatureNeverReachesRedis() {
        when(jwtDecoder.decode("rac")).thenThrow(new JwtException("chữ ký sai"));

        assertThatThrownBy(() -> service.validate("rac"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Redis chết -> TỪ CHỐI (fail-closed), khác hẳn rate limit vốn fail-open")
    void redisOutageFailsClosed() {
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwtOf(TOKEN));
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("jwt:42")).thenThrow(new QueryTimeoutException("redis down"));

        // cho qua lúc này nghĩa là mọi token đã thu hồi đều sống lại
        assertThatThrownBy(() -> service.validate(TOKEN))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Không kiểm tra được");
    }
}
