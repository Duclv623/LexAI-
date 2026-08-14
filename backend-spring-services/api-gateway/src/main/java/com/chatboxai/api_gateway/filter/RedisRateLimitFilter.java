package com.chatboxai.api_gateway.filter;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatboxai.api_gateway.configs.RateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RedisRateLimitFilter extends OncePerRequestFilter {

    // INCR and EXPIRE must be atomic, split them and a crash in between leaves a key with no ttl
    private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return c
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitProperties.Rule rule = resolveRule(path);

        if (rule == null || isHealthCheck(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate-limit:%s:%s".formatted(rule.getName(), clientIp(request));

        Long requests;
        try {
            requests = redisTemplate.execute(
                    INCR_WITH_TTL,
                    List.of(key),
                    String.valueOf(rule.getWindowSeconds())
            );
        } catch (DataAccessException e) {
            // fail open on purpose, rate limit is a guard and must not take the gateway down
            logger.warn("Redis không khả dụng — tạm bỏ qua rate limit cho " + path, e);
            filterChain.doFilter(request, response);
            return;
        }

        if (requests != null && requests > rule.getLimit()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"Too many requests"}""");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // longest prefix wins, so the result does not depend on rule order in the yaml
    private RateLimitProperties.Rule resolveRule(String path) {
        return properties.getRules().stream()
                .filter(rule -> path.startsWith(rule.getPathPrefix()))
                .max(Comparator.comparingInt(rule -> rule.getPathPrefix().length()))
                .orElse(null);
    }

    // health probes poll constantly, they must not eat the shared budget
    private boolean isHealthCheck(String path) {
        return path.endsWith("/health");
    }

    // this gateway is the edge, X-Forwarded-For is client supplied and must not be trusted
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
