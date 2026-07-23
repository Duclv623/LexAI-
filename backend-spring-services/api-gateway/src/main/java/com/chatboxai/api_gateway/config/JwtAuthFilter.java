package com.chatboxai.api_gateway.config;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gác cổng hệ thống: verify JWT cho mọi request cần bảo vệ.
 *
 * 1. Luôn XOÁ các header identity do client tự gửi (chống giả mạo X-User-*).
 * 2. Route public (login/register/health) → cho qua, không cần token.
 * 3. Route còn lại → bắt buộc Bearer token hợp lệ (verify qua JWKS), nếu sai trả 401.
 * 4. Token hợp lệ → gắn X-User-Id / X-User-Role / X-User-Email đã xác thực rồi forward.
 *
 * Downstream vẫn verify lại (zero-trust), nhưng header này giúp chúng đọc identity tiện lợi.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20) // chạy sau rate-limit, trước khi gateway forward
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";
    private static final String USER_EMAIL = "X-User-Email";

    private final JwtDecoder jwtDecoder;

    // Path public — không cần token. Còn lại đều phải xác thực.
    private final List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/health",
            "/api/chat/health",
            "/api/ai/health",
            "/actuator/health"
    );

    public JwtAuthFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Bước 1: luôn ẩn header identity client tự gửi.
        Map<String, String> identity = new HashMap<>();
        identity.put(USER_ID, null);
        identity.put(USER_ROLE, null);
        identity.put(USER_EMAIL, null);

        String path = request.getRequestURI();

        if (isPublic(path)) {
            filterChain.doFilter(new MutatedHeadersRequest(request, identity), response);
            return;
        }

        String token = bearerToken(request);
        if (token == null) {
            unauthorized(response, "Missing bearer token");
            return;
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token); // verify chữ ký + exp + issuer qua JWKS
        } catch (JwtException e) {
            unauthorized(response, "Invalid token");
            return;
        }

        // Bước 4: gắn identity đã xác thực (ghi đè, không tin client).
        identity.put(USER_ID, jwt.getSubject());
        identity.put(USER_ROLE, jwt.getClaimAsString("role"));
        identity.put(USER_EMAIL, jwt.getClaimAsString("email"));

        filterChain.doFilter(new MutatedHeadersRequest(request, identity), response);
    }

    private boolean isPublic(String path) {
        return publicPaths.stream().anyMatch(path::equals);
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length()).trim();
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"%s\"}".formatted(message));
    }

    /**
     * Bọc request để ép giá trị một số header:
     *  - value == null  → coi như header không tồn tại (xoá header client gửi).
     *  - value != null  → trả về đúng giá trị ta xác thực (ghi đè client).
     * So khớp tên header không phân biệt hoa/thường (đúng chuẩn HTTP).
     */
    private static class MutatedHeadersRequest extends HttpServletRequestWrapper {

        private final Map<String, String> overrides; // key đã lowercase

        MutatedHeadersRequest(HttpServletRequest request, Map<String, String> overrides) {
            super(request);
            this.overrides = new HashMap<>();
            overrides.forEach((k, v) -> this.overrides.put(k.toLowerCase(), v));
        }

        @Override
        public String getHeader(String name) {
            String key = name.toLowerCase();
            if (overrides.containsKey(key)) {
                return overrides.get(key); // có thể null → header bị xoá
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String key = name.toLowerCase();
            if (overrides.containsKey(key)) {
                String value = overrides.get(key);
                return value == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // Bỏ các header bị override khỏi danh sách gốc, rồi thêm lại những cái có giá trị.
            List<String> names = new java.util.ArrayList<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String n = original.nextElement();
                if (!overrides.containsKey(n.toLowerCase())) {
                    names.add(n);
                }
            }
            overrides.forEach((k, v) -> {
                if (v != null) {
                    names.add(k);
                }
            });
            return Collections.enumeration(names);
        }
    }
}
