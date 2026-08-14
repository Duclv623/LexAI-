package com.chatboxai.auth_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.chatboxai.auth_service.config.SecurityConfig;
import com.chatboxai.auth_service.dto.response.AccountResponseDTO;
import com.chatboxai.auth_service.dto.response.AuthResponseDTO;
import com.chatboxai.auth_service.exception.GlobalExceptionHandler;
import com.chatboxai.auth_service.service.AuthService;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    // SecurityConfig enables oauth2ResourceServer, @WebMvcTest does not autoconfigure a decoder
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static AuthResponseDTO authResponse() {
        return new AuthResponseDTO("token-gia", "Bearer", 86400,
                new AccountResponseDTO(1L, "levan@example.com", "Le Van", "USER", "ACTIVE", Instant.EPOCH));
    }

    private static String body(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    @Test
    @DisplayName("Đăng ký hợp lệ -> 200, dữ liệu nằm trong CustomResponse.data")
    void registerReturnsWrappedData() throws Exception {
        when(authService.register(any())).thenReturn(authResponse());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("levan@example.com", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token-gia"))
                .andExpect(jsonPath("$.data.account.email").value("levan@example.com"));
    }

    @Test
    @DisplayName("Email sai định dạng -> 400 kèm fields.email, KHÔNG gọi tới service")
    void invalidEmailReportsTheField() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("khong-phai-email", "secret123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.email").value("Email không đúng định dạng"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Mật khẩu quá ngắn -> 400 kèm fields.password")
    void shortPasswordReportsTheField() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("levan@example.com", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").value("Mật khẩu từ 6 đến 100 ký tự"));
    }

    @Test
    @DisplayName("Sai nhiều trường cùng lúc -> báo hết trong một response")
    void everyInvalidFieldIsReported() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("sai", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("Email đã tồn tại -> 409 kèm message, không phải body mặc định của Spring")
    void duplicateEmailKeepsItsStatus() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("levan@example.com", "secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("Sai thông tin đăng nhập -> 401 kèm message")
    void badCredentialsAreUnauthorized() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("levan@example.com", "sai-mat-khau")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
