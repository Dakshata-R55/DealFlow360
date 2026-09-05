package com.dealflow360.auth.security;

import com.dealflow360.auth.model.User;
import com.dealflow360.auth.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${dealflow360.jwt.secret}") String secret,
            @Value("${dealflow360.jwt.ttl-seconds}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        var builder = Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key);
        if (user.companyId() != null) {
            builder.claim("companyId", user.companyId());
        }
        return builder.compact();
    }

    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        long userId = Long.parseLong(claims.getSubject());
        Number companyId = claims.get("companyId", Number.class);
        String role = claims.get("role", String.class);
        Long company = companyId == null ? null : companyId.longValue();
        return new AuthPrincipal(userId, company, UserRole.valueOf(role));
    }
}