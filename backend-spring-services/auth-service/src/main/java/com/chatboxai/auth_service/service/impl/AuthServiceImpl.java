package com.chatboxai.auth_service.service.impl;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chatboxai.auth_service.dao.repository.AccountRepository;
import com.chatboxai.auth_service.dto.request.ChangePasswordRequestDTO;
import com.chatboxai.auth_service.dto.request.LoginRequestDTO;
import com.chatboxai.auth_service.dto.request.RegisterRequestDTO;
import com.chatboxai.auth_service.dto.response.AccountResponseDTO;
import com.chatboxai.auth_service.dto.response.AuthResponseDTO;
import com.chatboxai.auth_service.entity.Account;
import com.chatboxai.auth_service.mapper.AccountMapper;
import com.chatboxai.auth_service.service.AuthService;
import com.chatboxai.auth_service.service.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String email = normalizeEmail(request.getEmail());
        if (accountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());

        return toAuthResponse(accountRepository.save(account));
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        Account account = accountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return toAuthResponse(account);
    }

    @Override
    public AccountResponseDTO me(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        return AccountMapper.toResponse(account);
    }

    @Override
    public void changePassword(Long accountId, ChangePasswordRequestDTO request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        // require the current password even with a valid token, tokens can be stolen
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu cũ");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);

        // kick every session, the old token must not survive a password change
        jwtService.revoke(accountId);
    }

    @Override
    public void logout(Long accountId) {
        jwtService.revoke(accountId);
    }

    private AuthResponseDTO toAuthResponse(Account account) {
        return new AuthResponseDTO(
                jwtService.createToken(account),
                "Bearer",
                jwtService.getExpirationSeconds(),
                AccountMapper.toResponse(account));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
