package com.chatboxai.auth_service.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatboxai.auth_service.dto.request.ChangePasswordRequestDTO;
import com.chatboxai.auth_service.dto.request.LoginRequestDTO;
import com.chatboxai.auth_service.dto.request.RegisterRequestDTO;
import com.chatboxai.auth_service.dto.response.AccountResponseDTO;
import com.chatboxai.auth_service.dto.response.AuthResponseDTO;
import com.chatboxai.auth_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AccountResponseDTO me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping("/password")
    public Map<String, Boolean> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(Long.valueOf(jwt.getSubject()), request);
        return Map.of("success", true);
    }
}
