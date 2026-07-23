package com.chatboxai.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decoder verify token bằng PUBLIC key lấy từ JWKS endpoint của auth-service.
 * Gateway không giữ secret nào — chỉ fetch public key (Nimbus tự cache & xoay key).
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        // Kiểm tra cả hạn token (exp) lẫn issuer để chắc token do auth-service phát.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
