package com.chatboxai.auth_service.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatboxai.auth_service.auth.dto.AccountResponse;
import com.chatboxai.auth_service.auth.dto.AuthResponse;
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
    public AccountResponse me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return authService.me(authorizationHeader);
    }
}
