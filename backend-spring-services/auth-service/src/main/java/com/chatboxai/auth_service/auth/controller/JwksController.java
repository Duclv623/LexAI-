package com.chatboxai.auth_service.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * Publish PUBLIC key dạng JWKS để gateway/downstream verify token.
 * Chỉ phần công khai được trả ra — private key không bao giờ rời auth-service.
 */
@RestController
public class JwksController {

    private final RSAKey rsaKey;

    public JwksController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        // toPublicJWK() loại bỏ private key, chỉ giữ (kty, kid, n, e, alg, use...).
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
