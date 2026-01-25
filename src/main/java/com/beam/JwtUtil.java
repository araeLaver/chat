package com.beam;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final int MIN_SECRET_LENGTH = 32; // 256 bits
    private static final String DEFAULT_SECRET_PREFIX = "beam-";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is not configured. Set the JWT_SECRET environment variable.");
        }

        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters (256 bits). " +
                "Current length: " + secret.length());
        }

        if (secret.startsWith(DEFAULT_SECRET_PREFIX)) {
            if (isTestEnvironment()) {
                logger.info("Using test JWT secret for testing environment");
            } else {
                throw new IllegalStateException(
                    "Default JWT secret (beam-*) is not allowed. " +
                    "Set a secure JWT_SECRET environment variable. " +
                    "Generate one with: openssl rand -base64 64");
            }
        }

        if (isWeakSecret(secret)) {
            throw new IllegalStateException(
                "JWT secret is too weak. Avoid repeated characters or simple patterns. " +
                "Generate a secure one with: openssl rand -base64 64");
        }

        logger.info("JWT configuration validated successfully");
    }

    private boolean isTestEnvironment() {
        return "test".equalsIgnoreCase(activeProfile);
    }

    private boolean isWeakSecret(String secret) {
        if (secret.length() < MIN_SECRET_LENGTH) {
            return true;
        }

        // Check for repeated single character (e.g., "aaaaaaa...")
        char first = secret.charAt(0);
        boolean allSame = secret.chars().allMatch(c -> c == first);
        if (allSame) {
            return true;
        }

        // Check for low entropy (less than 10 unique characters)
        long uniqueChars = secret.chars().distinct().count();
        if (uniqueChars < 10) {
            return true;
        }

        return false;
    }

    private boolean isProductionEnvironment() {
        return "prod".equalsIgnoreCase(activeProfile) ||
               "production".equalsIgnoreCase(activeProfile);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}