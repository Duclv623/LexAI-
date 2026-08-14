package com.chatboxai.auth_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jose.jwk.RSAKey;

class JwtKeyConfigTest {

    private static String privateKeyBase64() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
    }

    private static String sign(JwtKeyConfig boot, RSAKey key) {
        Instant now = Instant.now();
        return boot.jwtEncoder(boot.jwkSource(key))
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(() -> "RS256").keyId(key.getKeyID()).build(),
                        JwtClaimsSet.builder()
                                .issuer("http://localhost:8081")
                                .subject("42")
                                .issuedAt(now)
                                .expiresAt(now.plusSeconds(300))
                                .build()))
                .getTokenValue();
    }

    @Test
    @DisplayName("Khoá cố định -> kid không đổi sau restart")
    void fixedKeyKeepsSameKid() throws Exception {
        String key = privateKeyBase64();

        String kidLanChay1 = new JwtKeyConfig(key).rsaKey().getKeyID();
        String kidLanChay2 = new JwtKeyConfig(key).rsaKey().getKeyID();

        assertThat(kidLanChay2).isEqualTo(kidLanChay1);
    }

    @Test
    @DisplayName("Khoá cố định -> token cấp TRƯỚC restart vẫn verify được SAU restart")
    void tokenIssuedBeforeRestartStillVerifies() throws Exception {
        String key = privateKeyBase64();

        var lanChay1 = new JwtKeyConfig(key);
        String token = sign(lanChay1, lanChay1.rsaKey());

        var lanChay2 = new JwtKeyConfig(key);
        var decoded = lanChay2.jwtDecoder(lanChay2.rsaKey()).decode(token);

        assertThat(decoded.getSubject()).isEqualTo("42");
    }

    @Test
    @DisplayName("ĐỐI CHỨNG: không cấu hình khoá -> token cũ chết sau restart (đúng bug đã sửa)")
    void ephemeralKeyBreaksTokenAfterRestart() {
        var lanChay1 = new JwtKeyConfig("");
        RSAKey key1 = lanChay1.rsaKey();
        String token = sign(lanChay1, key1);

        var lanChay2 = new JwtKeyConfig("");
        RSAKey key2 = lanChay2.rsaKey();

        assertThat(key2.getKeyID()).isNotEqualTo(key1.getKeyID());
        assertThatThrownBy(() -> lanChay2.jwtDecoder(key2).decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Khoá cấu hình sai -> chết ngay lúc khởi động kèm thông báo rõ ràng")
    void invalidKeyFailsFast() {
        assertThatThrownBy(() -> new JwtKeyConfig("day-khong-phai-base64-hop-le!!!").rsaKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_JWT_PRIVATE_KEY");
    }
}
