package com.chatboxai.auth_service.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.chatboxai.auth_service.account.entity.Account;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * Cấp (ký) JWT bằng RS256. auth-service là ISSUER duy nhất.
 * Phần VERIFY không còn ở đây — gateway và downstream tự verify qua JWKS.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final String issuer;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(Account account) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(expirationSeconds, ChronoUnit.SECONDS))
                .subject(String.valueOf(account.getId()))
                .claim("email", account.getEmail())
                .claim("role", account.getRole())
                .build();

        // Gắn kid vào header để phía verify chọn đúng public key trong JWKS.
        JwsHeader header = JwsHeader.with(() -> "RS256")
                .keyId(rsaKey.getKeyID())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
