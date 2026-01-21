package com.beam.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 보안 이벤트 구조화 로깅 유틸리티
 *
 * <p>인증, 권한, 보안 관련 이벤트를 일관된 형식으로 로깅합니다.
 * MDC를 활용하여 JSON 로그에 구조화된 데이터를 포함합니다.
 */
public class SecurityEventLogger {

    private static final Logger logger = LoggerFactory.getLogger("com.beam.security");

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        TOKEN_EXPIRED,
        TOKEN_INVALID,
        ACCESS_DENIED,
        RATE_LIMIT_EXCEEDED,
        SUSPICIOUS_ACTIVITY,
        FILE_UPLOAD_BLOCKED,
        BRUTE_FORCE_DETECTED
    }

    private SecurityEventLogger() {}

    /**
     * 로그인 성공 이벤트
     */
    public static void logLoginSuccess(String username, Long userId, String clientIp) {
        try {
            MDC.put("eventType", EventType.LOGIN_SUCCESS.name());
            MDC.put("username", username);
            MDC.put("userId", String.valueOf(userId));
            MDC.put("clientIp", clientIp);
            logger.info("User login successful: username={}", username);
        } finally {
            clearMdc();
        }
    }

    /**
     * 로그인 실패 이벤트
     */
    public static void logLoginFailure(String username, String clientIp, String reason) {
        try {
            MDC.put("eventType", EventType.LOGIN_FAILURE.name());
            MDC.put("username", username);
            MDC.put("clientIp", clientIp);
            MDC.put("reason", reason);
            logger.warn("User login failed: username={}, reason={}", username, reason);
        } finally {
            clearMdc();
        }
    }

    /**
     * 토큰 만료 이벤트
     */
    public static void logTokenExpired(String clientIp) {
        try {
            MDC.put("eventType", EventType.TOKEN_EXPIRED.name());
            MDC.put("clientIp", clientIp);
            logger.info("JWT token expired");
        } finally {
            clearMdc();
        }
    }

    /**
     * 토큰 유효하지 않음 이벤트
     */
    public static void logTokenInvalid(String clientIp, String reason) {
        try {
            MDC.put("eventType", EventType.TOKEN_INVALID.name());
            MDC.put("clientIp", clientIp);
            MDC.put("reason", reason);
            logger.warn("Invalid JWT token: reason={}", reason);
        } finally {
            clearMdc();
        }
    }

    /**
     * 접근 거부 이벤트
     */
    public static void logAccessDenied(Long userId, String resource, String clientIp) {
        try {
            MDC.put("eventType", EventType.ACCESS_DENIED.name());
            MDC.put("userId", userId != null ? String.valueOf(userId) : "anonymous");
            MDC.put("resource", resource);
            MDC.put("clientIp", clientIp);
            logger.warn("Access denied: userId={}, resource={}", userId, resource);
        } finally {
            clearMdc();
        }
    }

    /**
     * Rate Limit 초과 이벤트
     */
    public static void logRateLimitExceeded(String clientIp, String endpoint) {
        try {
            MDC.put("eventType", EventType.RATE_LIMIT_EXCEEDED.name());
            MDC.put("clientIp", clientIp);
            MDC.put("endpoint", endpoint);
            logger.warn("Rate limit exceeded: ip={}, endpoint={}", clientIp, endpoint);
        } finally {
            clearMdc();
        }
    }

    /**
     * 의심스러운 활동 이벤트
     */
    public static void logSuspiciousActivity(String clientIp, String description) {
        try {
            MDC.put("eventType", EventType.SUSPICIOUS_ACTIVITY.name());
            MDC.put("clientIp", clientIp);
            MDC.put("description", description);
            logger.error("Suspicious activity detected: ip={}, description={}", clientIp, description);
        } finally {
            clearMdc();
        }
    }

    /**
     * 파일 업로드 차단 이벤트
     */
    public static void logFileUploadBlocked(Long userId, String filename, String reason, String clientIp) {
        try {
            MDC.put("eventType", EventType.FILE_UPLOAD_BLOCKED.name());
            MDC.put("userId", userId != null ? String.valueOf(userId) : "anonymous");
            MDC.put("filename", filename);
            MDC.put("reason", reason);
            MDC.put("clientIp", clientIp);
            logger.warn("File upload blocked: userId={}, filename={}, reason={}", userId, filename, reason);
        } finally {
            clearMdc();
        }
    }

    /**
     * Brute Force 공격 감지 이벤트
     */
    public static void logBruteForceDetected(String clientIp, String targetUsername, int attemptCount) {
        try {
            MDC.put("eventType", EventType.BRUTE_FORCE_DETECTED.name());
            MDC.put("clientIp", clientIp);
            MDC.put("targetUsername", targetUsername);
            MDC.put("attemptCount", String.valueOf(attemptCount));
            logger.error("Brute force attack detected: ip={}, target={}, attempts={}",
                clientIp, targetUsername, attemptCount);
        } finally {
            clearMdc();
        }
    }

    private static void clearMdc() {
        MDC.remove("eventType");
        MDC.remove("username");
        MDC.remove("userId");
        MDC.remove("clientIp");
        MDC.remove("reason");
        MDC.remove("resource");
        MDC.remove("endpoint");
        MDC.remove("description");
        MDC.remove("filename");
        MDC.remove("targetUsername");
        MDC.remove("attemptCount");
    }
}
