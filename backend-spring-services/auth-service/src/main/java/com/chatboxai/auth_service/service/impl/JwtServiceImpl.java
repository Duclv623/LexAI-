package com.chatboxai.auth_service.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.chatboxai.auth_service.entity.Account;
import com.chatboxai.auth_service.service.JwtService;
import com.nimbusds.jose.jwk.RSAKey;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final String issuer;
    private final long expirationSeconds;

    public JwtServiceImpl(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
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

        // kid lets the verifier pick the right public key from the jwks
        JwsHeader header = JwsHeader.with(() -> "RS256")
                .keyId(rsaKey.getKeyID())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
