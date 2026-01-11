package com.beam;

import com.beam.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인증 엔드포인트 전용 Rate Limiter
 * - 로그인, 회원가입, 인증 코드 관련 엔드포인트 보호
 * - 무차별 대입 공격 방지
 * - IP 및 사용자 식별자 기반 제한
 */
@Component
public class AuthRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(AuthRateLimiter.class);

    // 설정값 (application.properties에서 설정 가능)
    @Value("${rate.limit.auth.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate.limit.auth.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${rate.limit.auth.verification-max-attempts:3}")
    private int verificationMaxAttempts;

    @Value("${rate.limit.auth.verification-lockout-minutes:60}")
    private int verificationLockoutMinutes;

    // IP별 로그인 시도 추적
    private final Map<String, AuthAttempt> loginAttempts = new ConcurrentHashMap<>();

    // 이메일/전화번호별 인증 코드 요청 추적
    private final Map<String, AuthAttempt> verificationAttempts = new ConcurrentHashMap<>();

    // 인증 코드 검증 시도 추적
    private final Map<String, AuthAttempt> verifyCodeAttempts = new ConcurrentHashMap<>();

    /**
     * 로그인 시도 체크 (IP 기반)
     */
    public void checkLoginAttempt(String ipAddress) {
        AuthAttempt attempt = loginAttempts.get(ipAddress);

        if (attempt != null && attempt.isLocked()) {
            long remainingSeconds = attempt.getRemainingLockSeconds();
            logger.warn("로그인 차단됨 - IP: {}, 남은 시간: {}초", ipAddress, remainingSeconds);
            throw RateLimitException.tooManyAuthAttempts(ipAddress, remainingSeconds);
        }
    }

    /**
     * 로그인 실패 기록
     */
    public void recordLoginFailure(String ipAddress) {
        AuthAttempt attempt = loginAttempts.computeIfAbsent(ipAddress, k -> new AuthAttempt());
        attempt.recordFailure(maxAttempts, lockoutMinutes);

        if (attempt.isLocked()) {
            logger.warn("로그인 잠금 - IP: {}, 실패 횟수: {}", ipAddress, attempt.getFailedCount());
        }
    }

    /**
     * 로그인 성공 시 실패 기록 초기화
     */
    public void recordLoginSuccess(String ipAddress) {
        loginAttempts.remove(ipAddress);
    }

    /**
     * 인증 코드 발송 요청 체크 (이메일/전화번호 기반)
     */
    public void checkVerificationRequest(String identifier) {
        AuthAttempt attempt = verificationAttempts.get(identifier);

        if (attempt != null && attempt.isLocked()) {
            long remainingSeconds = attempt.getRemainingLockSeconds();
            logger.warn("인증 코드 요청 차단됨 - 식별자: {}, 남은 시간: {}초",
                maskIdentifier(identifier), remainingSeconds);
            throw RateLimitException.tooManyAuthAttempts(remainingSeconds);
        }
    }

    /**
     * 인증 코드 발송 기록
     */
    public void recordVerificationRequest(String identifier) {
        AuthAttempt attempt = verificationAttempts.computeIfAbsent(identifier, k -> new AuthAttempt());
        attempt.recordFailure(verificationMaxAttempts, verificationLockoutMinutes);

        if (attempt.isLocked()) {
            logger.warn("인증 코드 요청 잠금 - 식별자: {}", maskIdentifier(identifier));
        }
    }

    /**
     * 인증 코드 검증 시도 체크
     */
    public void checkVerifyCodeAttempt(String identifier) {
        AuthAttempt attempt = verifyCodeAttempts.get(identifier);

        if (attempt != null && attempt.isLocked()) {
            long remainingSeconds = attempt.getRemainingLockSeconds();
            logger.warn("인증 코드 검증 차단됨 - 식별자: {}, 남은 시간: {}초",
                maskIdentifier(identifier), remainingSeconds);
            throw RateLimitException.tooManyAuthAttempts(remainingSeconds);
        }
    }

    /**
     * 인증 코드 검증 실패 기록
     */
    public void recordVerifyCodeFailure(String identifier) {
        AuthAttempt attempt = verifyCodeAttempts.computeIfAbsent(identifier, k -> new AuthAttempt());
        attempt.recordFailure(maxAttempts, lockoutMinutes);

        if (attempt.isLocked()) {
            logger.warn("인증 코드 검증 잠금 - 식별자: {}, 실패 횟수: {}",
                maskIdentifier(identifier), attempt.getFailedCount());
        }
    }

    /**
     * 인증 코드 검증 성공 시 초기화
     */
    public void recordVerifyCodeSuccess(String identifier) {
        verifyCodeAttempts.remove(identifier);
        verificationAttempts.remove(identifier);
    }

    /**
     * 5분마다 만료된 항목 정리
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();

        cleanupMap(loginAttempts, now);
        cleanupMap(verificationAttempts, now);
        cleanupMap(verifyCodeAttempts, now);
    }

    private void cleanupMap(Map<String, AuthAttempt> map, LocalDateTime now) {
        map.entrySet().removeIf(entry -> {
            AuthAttempt attempt = entry.getValue();
            // 잠금이 풀렸고 마지막 시도로부터 1시간 이상 지났으면 제거
            return !attempt.isLocked() &&
                   attempt.getLastAttemptTime().plusHours(1).isBefore(now);
        });
    }

    /**
     * 식별자 마스킹 (로그용)
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        if (identifier.contains("@")) {
            // 이메일: test@example.com -> te***@example.com
            int atIndex = identifier.indexOf("@");
            if (atIndex > 2) {
                return identifier.substring(0, 2) + "***" + identifier.substring(atIndex);
            }
        }
        // 일반: 앞 3글자만 표시
        return identifier.substring(0, 3) + "***";
    }

    /**
     * 인증 시도 추적 클래스
     */
    private static class AuthAttempt {
        private int failedCount;
        private LocalDateTime lockedUntil;
        private LocalDateTime lastAttemptTime;

        public AuthAttempt() {
            this.failedCount = 0;
            this.lockedUntil = null;
            this.lastAttemptTime = LocalDateTime.now();
        }

        public synchronized void recordFailure(int maxAttempts, int lockoutMinutes) {
            this.failedCount++;
            this.lastAttemptTime = LocalDateTime.now();

            if (failedCount >= maxAttempts) {
                this.lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
            }
        }

        public boolean isLocked() {
            if (lockedUntil == null) {
                return false;
            }
            if (LocalDateTime.now().isAfter(lockedUntil)) {
                // 잠금 해제
                reset();
                return false;
            }
            return true;
        }

        public long getRemainingLockSeconds() {
            if (lockedUntil == null) {
                return 0;
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(lockedUntil)) {
                return 0;
            }
            return java.time.Duration.between(now, lockedUntil).getSeconds();
        }

        public int getFailedCount() {
            return failedCount;
        }

        public LocalDateTime getLastAttemptTime() {
            return lastAttemptTime;
        }

        private void reset() {
            this.failedCount = 0;
            this.lockedUntil = null;
        }
    }
}
