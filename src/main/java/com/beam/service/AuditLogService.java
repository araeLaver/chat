package com.beam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 보안 감사 로깅 서비스
 * - 인증 실패 로깅
 * - 권한 거부 로깅
 * - 민감한 작업 로깅
 * - 보안 이벤트 로깅
 * - 의심스러운 활동 탐지
 */
@Service
public class AuditLogService {

    // 보안 감사 전용 로거
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // 실패한 인증 시도 추적 (IP -> 실패 횟수)
    private final Map<String, AtomicInteger> failedAuthAttempts = new ConcurrentHashMap<>();

    // 의심스러운 IP 추적
    private final Map<String, LocalDateTime> suspiciousIps = new ConcurrentHashMap<>();

    @Value("${security.audit.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.audit.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    /**
     * 감사 이벤트 타입
     */
    public enum AuditEventType {
        AUTH_SUCCESS("인증 성공"),
        AUTH_FAILURE("인증 실패"),
        AUTH_LOCKOUT("계정 잠금"),
        ACCESS_DENIED("접근 거부"),
        PERMISSION_DENIED("권한 거부"),
        RATE_LIMIT_HIT("Rate Limit 초과"),
        SUSPICIOUS_ACTIVITY("의심스러운 활동"),
        DATA_ACCESS("데이터 접근"),
        DATA_MODIFICATION("데이터 수정"),
        DATA_DELETION("데이터 삭제"),
        FILE_UPLOAD("파일 업로드"),
        FILE_DOWNLOAD("파일 다운로드"),
        ADMIN_ACTION("관리자 작업"),
        CONFIG_CHANGE("설정 변경"),
        PASSWORD_CHANGE("비밀번호 변경"),
        PROFILE_UPDATE("프로필 수정"),
        SESSION_CREATED("세션 생성"),
        SESSION_DESTROYED("세션 종료"),
        WEBSOCKET_CONNECT("WebSocket 연결"),
        WEBSOCKET_DISCONNECT("WebSocket 연결 해제"),
        SECURITY_VIOLATION("보안 위반");

        private final String description;

        AuditEventType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 감사 로그 기록 (비동기)
     */
    @Async
    public void logAuditEvent(AuditEventType eventType, String userId, String ipAddress,
                              String resource, String detail, boolean success) {
        try {
            MDC.put("auditEventType", eventType.name());
            MDC.put("userId", userId != null ? userId : "anonymous");
            MDC.put("ipAddress", ipAddress != null ? ipAddress : "unknown");
            MDC.put("resource", resource != null ? resource : "N/A");
            MDC.put("success", String.valueOf(success));

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String status = success ? "SUCCESS" : "FAILURE";

            String logMessage = String.format(
                "[%s] [%s] [%s] User: %s | IP: %s | Resource: %s | Detail: %s",
                timestamp, eventType.name(), status, userId, ipAddress, resource, detail
            );

            if (success) {
                auditLogger.info(logMessage);
            } else {
                auditLogger.warn(logMessage);
            }

        } finally {
            MDC.clear();
        }
    }

    /**
     * 인증 성공 로깅
     */
    public void logAuthSuccess(String username, String ipAddress, String method) {
        // 이전 실패 기록 초기화
        failedAuthAttempts.remove(ipAddress);
        suspiciousIps.remove(ipAddress);

        logAuditEvent(
            AuditEventType.AUTH_SUCCESS,
            username,
            ipAddress,
            "/auth/" + method,
            "인증 방식: " + method,
            true
        );
    }

    /**
     * 인증 실패 로깅 및 실패 횟수 추적
     */
    public void logAuthFailure(String username, String ipAddress, String reason) {
        // 실패 횟수 증가
        AtomicInteger attempts = failedAuthAttempts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        logAuditEvent(
            AuditEventType.AUTH_FAILURE,
            username,
            ipAddress,
            "/auth/login",
            String.format("실패 사유: %s | 연속 실패: %d회", reason, currentAttempts),
            false
        );

        // 최대 실패 횟수 초과 시 잠금 및 의심스러운 IP로 등록
        if (currentAttempts >= maxFailedAttempts) {
            suspiciousIps.put(ipAddress, LocalDateTime.now());
            logAuditEvent(
                AuditEventType.AUTH_LOCKOUT,
                username,
                ipAddress,
                "/auth/login",
                String.format("연속 %d회 인증 실패로 잠금. 잠금 해제까지 %d분", currentAttempts, lockoutDurationMinutes),
                false
            );
        }
    }

    /**
     * IP가 잠금 상태인지 확인
     */
    public boolean isIpLocked(String ipAddress) {
        LocalDateTime lockTime = suspiciousIps.get(ipAddress);
        if (lockTime == null) {
            return false;
        }

        // 잠금 시간이 지났으면 해제
        if (lockTime.plusMinutes(lockoutDurationMinutes).isBefore(LocalDateTime.now())) {
            suspiciousIps.remove(ipAddress);
            failedAuthAttempts.remove(ipAddress);
            return false;
        }

        return true;
    }

    /**
     * 접근 거부 로깅
     */
    public void logAccessDenied(String userId, String ipAddress, String resource, String reason) {
        logAuditEvent(
            AuditEventType.ACCESS_DENIED,
            userId,
            ipAddress,
            resource,
            reason,
            false
        );
    }

    /**
     * Rate Limit 초과 로깅
     */
    public void logRateLimitHit(String userId, String ipAddress, String endpoint, int limit) {
        logAuditEvent(
            AuditEventType.RATE_LIMIT_HIT,
            userId,
            ipAddress,
            endpoint,
            String.format("Rate limit 초과 (제한: %d)", limit),
            false
        );
    }

    /**
     * 파일 업로드 로깅
     */
    public void logFileUpload(String userId, String ipAddress, String fileName, long fileSize, boolean success) {
        logAuditEvent(
            AuditEventType.FILE_UPLOAD,
            userId,
            ipAddress,
            "/api/files/upload",
            String.format("파일: %s (크기: %d bytes)", fileName, fileSize),
            success
        );
    }

    /**
     * 파일 다운로드 로깅
     */
    public void logFileDownload(String userId, String ipAddress, Long fileId, String fileName) {
        logAuditEvent(
            AuditEventType.FILE_DOWNLOAD,
            userId,
            ipAddress,
            "/api/files/download/" + fileId,
            String.format("파일: %s", fileName),
            true
        );
    }

    /**
     * 데이터 수정 로깅
     */
    public void logDataModification(String userId, String ipAddress, String entityType,
                                    String entityId, String action) {
        logAuditEvent(
            AuditEventType.DATA_MODIFICATION,
            userId,
            ipAddress,
            entityType + "/" + entityId,
            action,
            true
        );
    }

    /**
     * 데이터 삭제 로깅
     */
    public void logDataDeletion(String userId, String ipAddress, String entityType, String entityId) {
        logAuditEvent(
            AuditEventType.DATA_DELETION,
            userId,
            ipAddress,
            entityType + "/" + entityId,
            "삭제됨",
            true
        );
    }

    /**
     * 관리자 작업 로깅
     */
    public void logAdminAction(String adminId, String ipAddress, String action, String targetResource) {
        logAuditEvent(
            AuditEventType.ADMIN_ACTION,
            adminId,
            ipAddress,
            targetResource,
            action,
            true
        );
    }

    /**
     * 보안 위반 로깅
     */
    public void logSecurityViolation(String userId, String ipAddress, String resource, String violationType) {
        logAuditEvent(
            AuditEventType.SECURITY_VIOLATION,
            userId,
            ipAddress,
            resource,
            violationType,
            false
        );

        // 의심스러운 IP로 등록
        suspiciousIps.put(ipAddress, LocalDateTime.now());
    }

    /**
     * WebSocket 연결 로깅
     */
    public void logWebSocketConnect(String userId, String ipAddress, String sessionId) {
        logAuditEvent(
            AuditEventType.WEBSOCKET_CONNECT,
            userId,
            ipAddress,
            "websocket/" + sessionId,
            "WebSocket 연결 수립",
            true
        );
    }

    /**
     * WebSocket 연결 해제 로깅
     */
    public void logWebSocketDisconnect(String userId, String ipAddress, String sessionId, String reason) {
        logAuditEvent(
            AuditEventType.WEBSOCKET_DISCONNECT,
            userId,
            ipAddress,
            "websocket/" + sessionId,
            reason,
            true
        );
    }

    /**
     * 비밀번호 변경 로깅
     */
    public void logPasswordChange(String userId, String ipAddress, boolean success) {
        logAuditEvent(
            AuditEventType.PASSWORD_CHANGE,
            userId,
            ipAddress,
            "/api/user/password",
            success ? "비밀번호 변경 완료" : "비밀번호 변경 실패",
            success
        );
    }

    /**
     * 의심스러운 활동 로깅
     */
    public void logSuspiciousActivity(String userId, String ipAddress, String resource, String description) {
        logAuditEvent(
            AuditEventType.SUSPICIOUS_ACTIVITY,
            userId,
            ipAddress,
            resource,
            description,
            false
        );

        // 의심스러운 IP로 등록
        suspiciousIps.put(ipAddress, LocalDateTime.now());
    }

    /**
     * 현재 의심스러운 IP 목록 조회
     */
    public Map<String, LocalDateTime> getSuspiciousIps() {
        return Map.copyOf(suspiciousIps);
    }

    /**
     * IP별 실패 횟수 조회
     */
    public int getFailedAttempts(String ipAddress) {
        AtomicInteger attempts = failedAuthAttempts.get(ipAddress);
        return attempts != null ? attempts.get() : 0;
    }

    /**
     * 수동 잠금 해제
     */
    public void unlockIp(String ipAddress) {
        suspiciousIps.remove(ipAddress);
        failedAuthAttempts.remove(ipAddress);
        logger.info("IP {} 수동 잠금 해제", ipAddress);
    }
}
