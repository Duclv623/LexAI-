package com.chatboxai.auth_service.service;

import com.chatboxai.auth_service.entity.Account;

public interface JwtService {

    String createToken(Account account);

    void revoke(Long accountId);

    long getExpirationSeconds();
}
