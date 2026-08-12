package com.chatboxai.api_gateway.filter;

import com.chatboxai.api_gateway.config.RateLimitProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Khoá lại hành vi rate limit. Redis được mock nên test chạy không cần docker.
 */
@SuppressWarnings("unchecked") // matcher của Mockito trả RedisScript thô
class RedisRateLimitFilterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisRateLimitFilter filter = new RedisRateLimitFilter(redis, properties());

    /** Đúng bộ rule trong application.yaml. */
    private static RateLimitProperties properties() {
        var props = new RateLimitProperties();
        props.setRules(List.of(
                rule("auth-login", "/api/auth/login", 10),
                rule("auth-register", "/api/auth/register", 5),
                rule("auth", "/api/auth/", 60),
                rule("chat", "/api/chat/", 20),
                rule("ai", "/api/ai/", 10)
        ));
        return props;
    }

    private static RateLimitProperties.Rule rule(String name, String prefix, int limit) {
        var r = new RateLimitProperties.Rule();
        r.setName(name);
        r.setPathPrefix(prefix);
        r.setLimit(limit);
        r.setWindowSeconds(60);
        return r;
    }

    private static MockHttpServletRequest get(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    @Test
    @DisplayName("Dưới ngưỡng -> cho đi tiếp")
    void underLimitPassesThrough() throws Exception {
        doReturn(20L).when(redis).execute(any(RedisScript.class), anyList(), any());

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(get("/api/chat/messages"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Vượt ngưỡng -> 429 và KHÔNG đi tiếp")
    void overLimitReturns429() throws Exception {
        doReturn(21L).when(redis).execute(any(RedisScript.class), anyList(), any());

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(get("/api/chat/messages"), response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("FAIL-OPEN: Redis chết -> vẫn cho đi tiếp, KHÔNG được 500")
    void redisDownFailsOpen() throws Exception {
        doThrow(new QueryTimeoutException("Redis không kết nối được"))
                .when(redis).execute(any(RedisScript.class), anyList(), any());

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(get("/api/chat/messages"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Path /health không đụng tới Redis")
    void healthPathSkipsRedisEntirely() throws Exception {
        var chain = new MockFilterChain();
        filter.doFilter(get("/api/auth/health"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(redis);
    }

    @Test
    @DisplayName("LONGEST-PREFIX: /api/auth/login dùng rule auth-login, không phải auth")
    void loginUsesMostSpecificRule() throws Exception {
        doReturn(1L).when(redis).execute(any(RedisScript.class), anyList(), any());

        filter.doFilter(get("/api/auth/login"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(capturedKey()).isEqualTo("rate-limit:auth-login:127.0.0.1");
    }

    @Test
    @DisplayName("LONGEST-PREFIX: /api/auth/me rơi về rule auth chung")
    void otherAuthPathsUseGenericRule() throws Exception {
        doReturn(1L).when(redis).execute(any(RedisScript.class), anyList(), any());

        filter.doFilter(get("/api/auth/me"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(capturedKey()).isEqualTo("rate-limit:auth:127.0.0.1");
    }

    @Test
    @DisplayName("Key dựng từ IP thật, KHÔNG lấy từ X-Forwarded-For client gửi")
    void forwardedForHeaderIsIgnored() throws Exception {
        doReturn(1L).when(redis).execute(any(RedisScript.class), anyList(), any());

        var request = get("/api/chat/messages");
        request.addHeader("X-Forwarded-For", "1.2.3.4"); // client tự khai, phải bị bỏ qua

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(capturedKey())
                .isEqualTo("rate-limit:chat:127.0.0.1")
                .doesNotContain("1.2.3.4");
    }

    @Test
    @DisplayName("Path không khớp rule nào -> không đụng Redis")
    void unmatchedPathSkipsRedis() throws Exception {
        var chain = new MockFilterChain();
        filter.doFilter(get("/actuator/health"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(redis);
    }

    private String capturedKey() {
        var keys = ArgumentCaptor.forClass(List.class);
        verify(redis).execute(any(RedisScript.class), keys.capture(), any());
        return (String) keys.getValue().get(0);
    }
}
