package com.chatboxai.api_gateway.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // chặn flood trước, rồi mới tới JwtAuthFilter
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RedisRateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitProperties.Rule rule = resolveRule(request.getRequestURI());

        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate-limit:%s:%s".formatted(rule.getName(), clientIp(request));
        Long requests = redisTemplate.opsForValue().increment(key);

        if (requests != null && requests == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rule.getWindowSeconds()));
        }

        if (requests != null && requests > rule.getLimit()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"Too many requests"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitProperties.Rule resolveRule(String path) {
        return properties.getRules().stream()
                .filter(rule -> path.startsWith(rule.getPathPrefix()))
                .findFirst()
                .orElse(null);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
