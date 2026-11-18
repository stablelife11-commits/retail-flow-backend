package com.retail.flow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String INSECURE_DEFAULT_SECRET =
            "YourSuperSecretKeyForRetailFlowBackendJwtAuthenticationWithEnoughLengthForHS512AlgorithmSecureKey";

    // Renew token automatically when less than 30 days remain
    private static final long RENEWAL_THRESHOLD_MS = 30L * 24 * 60 * 60 * 1000;

    private final Environment environment;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:31536000000}")
    private long jwtExpirationInMs;

    private SecretKey signingKey;

    @PostConstruct
    public void initAndValidateSecret() {
        if (jwtSecret == null || jwtSecret.trim().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("CRITICAL SECURITY ERROR: app.jwt.secret must be at least 32 bytes (256 bits) long.");
        }

        boolean isProdProfile = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProdProfile && INSECURE_DEFAULT_SECRET.equals(jwtSecret.trim())) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY ERROR: Production profile ('prod') cannot start with the default sample JWT_SECRET.");
        }

        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.trim().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String getEmailFromJWT(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromJWT(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long getUserIdFromJWT(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String authToken) {
        try {
            getClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean shouldRenewToken(String authToken) {
        try {
            Date expiration = getClaims(authToken).getExpiration();
            if (expiration == null) return false;
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return remainingMs > 0 && remainingMs < RENEWAL_THRESHOLD_MS;
        } catch (Exception ex) {
            return false;
        }
    }
}