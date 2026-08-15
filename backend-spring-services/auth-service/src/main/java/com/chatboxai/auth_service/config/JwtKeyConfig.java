package com.chatboxai.auth_service.config;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    private final String privateKeyBase64;

    public JwtKeyConfig(@Value("${app.jwt.private-key:}") String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    @Bean
    public RSAKey rsaKey() {
        return privateKeyBase64.isBlank() ? generateEphemeralKey() : loadFixedKey();
    }

    private RSAKey loadFixedKey() {
        try {
            byte[] der = Base64.getDecoder().decode(privateKeyBase64.replaceAll("\\s", ""));
            KeyFactory factory = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(der));
            RSAPrivateCrtKey crt = (RSAPrivateCrtKey) privateKey;
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));

            RSAKey key = toRsaKey(publicKey, privateKey);
            log.info("Đã nạp khoá RSA cố định cho JWT (kid={})", key.getKeyID());
            return key;
        } catch (Exception e) {
            // openssl mặc định xuất pkcs#1, trong khi PKCS8EncodedKeySpec chỉ đọc được pkcs#8
            throw new IllegalStateException(
                    "AUTH_JWT_PRIVATE_KEY không hợp lệ — cần base64 (một dòng) của private key RSA "
                            + "định dạng PKCS#8 DER, không phải PKCS#1. Cách tạo: xem .env.example", e);
        }
    }

    private RSAKey generateEphemeralKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            log.warn("Chưa cấu hình AUTH_JWT_PRIVATE_KEY -> sinh khoá RSA tạm cho lần chạy này. "
                    + "Mọi token đã cấp sẽ hết hiệu lực sau khi restart. Xem .env.example để tạo khoá cố định.");
            return toRsaKey((RSAPublicKey) keyPair.getPublic(), keyPair.getPrivate());
        } catch (Exception e) {
            throw new IllegalStateException("Không thể sinh cặp khoá RSA cho JWT", e);
        }
    }

    private RSAKey toRsaKey(RSAPublicKey publicKey, PrivateKey privateKey) throws Exception {
        // kid suy ra từ dấu vân tay khoá chứ không phải uuid ngẫu nhiên, nhờ vậy giữ nguyên sau khi restart
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyIDFromThumbprint()
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo JwtDecoder", e);
        }
    }
}
