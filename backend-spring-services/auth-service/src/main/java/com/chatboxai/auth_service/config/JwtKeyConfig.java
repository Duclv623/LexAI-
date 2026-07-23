package com.chatboxai.auth_service.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Cấu hình khoá RSA cho JWT (RS256).
 *
 * auth-service là ISSUER duy nhất giữ private key để KÝ token.
 * Public key được publish qua endpoint /.well-known/jwks.json để
 * gateway và các service khác VERIFY (zero-trust, không ai cần secret).
 *
 * Ở đây key được sinh mới mỗi lần khởi động (đủ cho môi trường học/dev):
 * vì public key luôn được phát qua JWKS nên verify vẫn chạy đúng trong 1 phiên.
 * Lên production nên nạp cặp key cố định từ file/biến môi trường thay cho generate.
 */
@Configuration
public class JwtKeyConfig {

    /**
     * Sinh cặp RSA 2048-bit, gói vào Nimbus RSAKey kèm một keyID (kid).
     * kid giúp phía verify biết token được ký bằng key nào trong JWKS.
     */
    @Bean
    public RSAKey rsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey(keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể sinh cặp khoá RSA cho JWT", e);
        }
    }

    /** Nguồn khoá dùng chung cho encoder (ký) và JWKS endpoint (publish public key). */
    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /** Encoder ký token bằng private key. */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Decoder verify token bằng public key ngay tại auth-service (dùng cho /me — zero-trust).
     * Các service khác dùng decoder trỏ tới jwk-set-uri thay vì key nội bộ.
     */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo JwtDecoder", e);
        }
    }
}
