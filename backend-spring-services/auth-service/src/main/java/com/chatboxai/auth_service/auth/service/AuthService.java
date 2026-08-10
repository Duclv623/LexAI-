package com.chatboxai.auth_service.auth.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chatboxai.auth_service.account.entity.Account;
import com.chatboxai.auth_service.account.repository.AccountRepository;
import com.chatboxai.auth_service.auth.dto.AccountResponse;
import com.chatboxai.auth_service.auth.dto.AuthResponse;
import com.chatboxai.auth_service.auth.dto.ChangePasswordRequest;
import com.chatboxai.auth_service.auth.dto.LoginRequest;
import com.chatboxai.auth_service.auth.dto.RegisterRequest;

@Service
public class AuthService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AccountRepository accounts, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (accounts.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());

        Account saved = accounts.save(account);
        return toAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Account account = accounts.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return toAuthResponse(account);
    }

    public AccountResponse me(Long accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        return AccountResponse.from(account);
    }

    /**
     * Đổi mật khẩu.
     *
     * HẠN CHẾ ĐÃ BIẾT: token cũ vẫn còn hiệu lực sau khi đổi mật khẩu, vì JWT là
     * stateless — server không giữ danh sách token đã cấp nên không thu hồi được.
     * Muốn thu hồi thật thì cần thêm refresh token + danh sách đen, hoặc gắn số phiên
     * bản mật khẩu vào claim và đối chiếu mỗi lần verify.
     */
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        // Có token rồi vẫn phải xác nhận mật khẩu cũ: token nằm trong localStorage
        // nên hoàn toàn có thể bị lấy cắp.
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu cũ");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        accounts.save(account);
    }

    private AuthResponse toAuthResponse(Account account) {
        return new AuthResponse(
                jwtService.createToken(account),
                "Bearer",
                jwtService.getExpirationSeconds(),
                AccountResponse.from(account)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
