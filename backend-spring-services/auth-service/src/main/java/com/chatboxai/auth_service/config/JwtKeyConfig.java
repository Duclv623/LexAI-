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

/**
 * Cấu hình khoá RSA cho JWT (RS256).
 *
 * auth-service là ISSUER duy nhất giữ private key để KÝ token.
 * Public key được publish qua endpoint /.well-known/jwks.json để
 * gateway và các service khác VERIFY (zero-trust, không ai cần secret).
 *
 * Khoá được nạp CỐ ĐỊNH từ biến môi trường AUTH_JWT_PRIVATE_KEY (xem .env),
 * nhờ vậy token đã cấp vẫn còn hiệu lực sau khi restart auth-service.
 * Nếu biến này để trống thì sinh khoá tạm cho lần chạy đó — tiện khi mới clone
 * về, nhưng restart là mọi token cũ chết.
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    /** Private key RSA dạng PKCS#8 DER đã base64 về một dòng. Rỗng = sinh khoá tạm. */
    private final String privateKeyBase64;

    public JwtKeyConfig(@Value("${app.jwt.private-key:}") String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    /**
     * Cặp RSA 2048-bit gói trong Nimbus RSAKey kèm keyID (kid).
     * kid giúp phía verify chọn đúng public key trong JWKS — xem JwtService gắn nó vào header.
     */
    @Bean
    public RSAKey rsaKey() {
        return privateKeyBase64.isBlank() ? generateEphemeralKey() : loadFixedKey();
    }

    /**
     * Dựng RSAKey từ private key cố định trong cấu hình.
     *
     * PKCS#8 của RSA chứa sẵn modulus và public exponent (CRT params), nên public key
     * suy ra được từ private key — .env chỉ phải giữ MỘT giá trị bí mật thay vì hai.
     */
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
            // Bẫy hay gặp: openssl mặc định trả PKCS#1, còn PKCS8EncodedKeySpec chỉ hiểu PKCS#8
            // (triệu chứng: "algid parse error, not a sequence"). Phải qua bước pkcs8 -topk8.
            throw new IllegalStateException(
                    "AUTH_JWT_PRIVATE_KEY không hợp lệ — cần base64 (một dòng) của private key RSA "
                            + "định dạng PKCS#8 DER, không phải PKCS#1. Cách tạo: xem .env.example", e);
        }
    }

    /** Khoá dùng-một-lần khi chưa cấu hình gì, để app vẫn chạy được ngay sau khi clone. */
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

    /**
     * kid tính bằng thumbprint (RFC 7638) của chính public key, KHÔNG phải UUID ngẫu nhiên.
     * Đây là mấu chốt: khoá cố định mà kid vẫn đổi mỗi lần khởi động thì token cũ
     * mang kid cũ sẽ không khớp entry nào trong JWKS mới và vẫn bị từ chối.
     */
    private RSAKey toRsaKey(RSAPublicKey publicKey, PrivateKey privateKey) throws Exception {
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyIDFromThumbprint()
                .build();
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
