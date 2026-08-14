package com.chatboxai.api_gateway.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatboxai.api_gateway.service.TokenValidationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";
    private static final String USER_EMAIL = "X-User-Email";

    private final TokenValidationService tokenValidationService;

    private final List<String> publicPaths = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/health",
            "/api/chat/health",
            "/api/ai/health",
            "/actuator/health"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // always strip identity headers the client sent itself
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
            jwt = tokenValidationService.validate(token);
        } catch (JwtException e) {
            unauthorized(response, "Invalid token");
            return;
        }

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
}
