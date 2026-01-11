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

        if (secret.startsWith(DEFAULT_SECRET_PREFIX)) {
            if (isProductionEnvironment()) {
                throw new IllegalStateException(
                    "Default JWT secret detected in production environment. " +
                    "Set a secure JWT_SECRET environment variable. " +
                    "Generate one with: openssl rand -base64 64");
            }
            logger.warn("⚠️ Using default JWT secret. This is only acceptable for development.");
        }

        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters (256 bits). " +
                "Current length: " + secret.length());
        }

        logger.info("✅ JWT configuration validated successfully");
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