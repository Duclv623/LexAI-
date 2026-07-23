package com.chatboxai.auth_service.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chatboxai.auth_service.account.entity.Account;

@Service
public class JwtService {

    private final String secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(Account account) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + expirationSeconds;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = """
                {"sub":"%s","email":"%s","role":"%s","iat":%d,"exp":%d}
                """.formatted(account.getId(), account.getEmail(), account.getRole(), now, expiresAt).trim();

        String encodedHeader = base64Url(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedHeader + "." + encodedPayload);

        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public Optional<Long> extractAccountId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return Optional.empty();
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        if (isExpired(payload)) {
            return Optional.empty();
        }

        return extractStringClaim(payload, "sub").flatMap(value -> {
            try {
                return Optional.of(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        });
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private boolean isExpired(String payload) {
        return extractLongClaim(payload, "exp")
                .map(exp -> exp <= Instant.now().getEpochSecond())
                .orElse(true);
    }

    private Optional<String> extractStringClaim(String payload, String claim) {
        String marker = "\"" + claim + "\":\"";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        int valueStart = start + marker.length();
        int valueEnd = payload.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            return Optional.empty();
        }
        return Optional.of(payload.substring(valueStart, valueEnd));
    }

    private Optional<Long> extractLongClaim(String payload, String claim) {
        String marker = "\"" + claim + "\":";
        int start = payload.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        int valueStart = start + marker.length();
        int valueEnd = valueStart;
        while (valueEnd < payload.length() && Character.isDigit(payload.charAt(valueEnd))) {
            valueEnd++;
        }
        if (valueEnd == valueStart) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(payload.substring(valueStart, valueEnd)));
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign JWT", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
