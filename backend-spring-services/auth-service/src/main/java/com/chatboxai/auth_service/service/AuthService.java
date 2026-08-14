package com.chatboxai.auth_service.service;

import com.chatboxai.auth_service.dto.request.ChangePasswordRequestDTO;
import com.chatboxai.auth_service.dto.request.LoginRequestDTO;
import com.chatboxai.auth_service.dto.request.RegisterRequestDTO;
import com.chatboxai.auth_service.dto.response.AccountResponseDTO;
import com.chatboxai.auth_service.dto.response.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);

    AccountResponseDTO me(Long accountId);

    void changePassword(Long accountId, ChangePasswordRequestDTO request);

    void logout(Long accountId);
}
