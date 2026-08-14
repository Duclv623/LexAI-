package com.chatboxai.auth_service.controller;

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
import com.chatboxai.auth_service.dto.response.CustomResponse;
import com.chatboxai.auth_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public CustomResponse<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return new CustomResponse<>(true, authService.register(request), "Đăng ký thành công");
    }

    @PostMapping("/login")
    public CustomResponse<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return new CustomResponse<>(true, authService.login(request), "Đăng nhập thành công");
    }

    @GetMapping("/me")
    public CustomResponse<AccountResponseDTO> me(@AuthenticationPrincipal Jwt jwt) {
        return new CustomResponse<>(true,
                authService.me(Long.valueOf(jwt.getSubject())),
                "Lấy thông tin tài khoản thành công");
    }

    @PatchMapping("/password")
    public CustomResponse<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(Long.valueOf(jwt.getSubject()), request);
        return new CustomResponse<>(true, "Đổi mật khẩu thành công");
    }
}
