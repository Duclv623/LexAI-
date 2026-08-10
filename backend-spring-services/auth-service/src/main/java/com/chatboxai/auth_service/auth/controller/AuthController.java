package com.chatboxai.auth_service.auth.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatboxai.auth_service.auth.dto.AccountResponse;
import com.chatboxai.auth_service.auth.dto.AuthResponse;
import com.chatboxai.auth_service.auth.dto.ChangePasswordRequest;
import com.chatboxai.auth_service.auth.dto.LoginRequest;
import com.chatboxai.auth_service.auth.dto.RegisterRequest;
import com.chatboxai.auth_service.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        // Token đã được Spring Security verify; sub chính là account id.
        return authService.me(Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping("/password")
    public Map<String, Boolean> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(Long.valueOf(jwt.getSubject()), request);
        return Map.of("success", true);
    }
}
