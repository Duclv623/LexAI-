package com.chatboxai.auth_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.chatboxai.auth_service.dao.repository.AccountRepository;
import com.chatboxai.auth_service.dto.request.ChangePasswordRequestDTO;
import com.chatboxai.auth_service.entity.Account;
import com.chatboxai.auth_service.service.JwtService;

class AuthServiceImplTest {

    private static final Long ACCOUNT_ID = 42L;

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthServiceImpl service =
            new AuthServiceImpl(accountRepository, passwordEncoder, jwtService);

    private Account storedAccount() {
        var account = new Account();
        account.setEmail("levan@example.com");
        account.setPasswordHash("hash-cu");
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công -> THU HỒI token, phiên cũ không dùng tiếp được")
    void changePasswordRevokesTheToken() {
        storedAccount();
        when(passwordEncoder.matches("cu", "hash-cu")).thenReturn(true);
        when(passwordEncoder.matches("moi-8-ky-tu", "hash-cu")).thenReturn(false);
        when(passwordEncoder.encode("moi-8-ky-tu")).thenReturn("hash-moi");

        service.changePassword(ACCOUNT_ID, new ChangePasswordRequestDTO("cu", "moi-8-ky-tu"));

        verify(jwtService).revoke(ACCOUNT_ID);
    }

    @Test
    @DisplayName("Sai mật khẩu hiện tại -> KHÔNG thu hồi gì, người khác không đá được phiên của bạn")
    void wrongCurrentPasswordRevokesNothing() {
        storedAccount();
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(
                ACCOUNT_ID, new ChangePasswordRequestDTO("sai", "moi-8-ky-tu")))
                .isInstanceOf(ResponseStatusException.class);

        verify(jwtService, never()).revoke(ACCOUNT_ID);
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Đăng xuất -> xoá token khỏi Redis")
    void logoutRevokesTheToken() {
        service.logout(ACCOUNT_ID);

        verify(jwtService).revoke(ACCOUNT_ID);
    }
}
