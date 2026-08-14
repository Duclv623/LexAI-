package com.chatboxai.auth_service.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    // gateway reads the same key, keep the two in sync
    public static final String TOKEN_KEY_PREFIX = "jwt:";

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final StringRedisTemplate redisTemplate;
    private final String issuer;
    private final long expirationSeconds;

    public JwtServiceImpl(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.redisTemplate = redisTemplate;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String createToken(Account account) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                // jti keeps every token unique. iat/exp only have second precision and RS256 is
                // deterministic, so two logins in the same second would otherwise sign the exact
                // same string and the newer one could not replace the older one in redis
                .id(UUID.randomUUID().toString())
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

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // one token per account: logging in elsewhere replaces this one and logs the old device out.
        // ttl matches the token lifetime so the key cannot outlive what it authorises
        redisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + account.getId(), token, Duration.ofSeconds(expirationSeconds));

        return token;
    }

    @Override
    public void revoke(Long accountId) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + accountId);
    }

    @Override
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
