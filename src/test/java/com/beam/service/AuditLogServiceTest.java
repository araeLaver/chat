package com.beam.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService 테스트")
class AuditLogServiceTest {

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService();
        ReflectionTestUtils.setField(auditLogService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(auditLogService, "lockoutDurationMinutes", 15);
    }

    @Nested
    @DisplayName("인증 실패 추적 테스트")
    class AuthFailureTrackingTests {

        @Test
        @DisplayName("첫 번째 인증 실패 시 실패 횟수가 1이 되어야 함")
        void shouldTrackFirstFailedAttempt() {
            // given
            String ipAddress = "192.168.1.1";

            // when
            auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");

            // then
            assertThat(auditLogService.getFailedAttempts(ipAddress)).isEqualTo(1);
        }

        @Test
        @DisplayName("연속 실패 시 실패 횟수가 누적되어야 함")
        void shouldAccumulateFailedAttempts() {
            // given
            String ipAddress = "192.168.1.2";

            // when
            auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");

            // then
            assertThat(auditLogService.getFailedAttempts(ipAddress)).isEqualTo(3);
        }

        @Test
        @DisplayName("최대 실패 횟수 초과 시 IP가 잠금 상태가 되어야 함")
        void shouldLockIpAfterMaxFailedAttempts() {
            // given
            String ipAddress = "192.168.1.3";

            // when - 5회 실패
            for (int i = 0; i < 5; i++) {
                auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            }

            // then
            assertThat(auditLogService.isIpLocked(ipAddress)).isTrue();
        }

        @Test
        @DisplayName("최대 실패 횟수 미만이면 IP가 잠금되지 않아야 함")
        void shouldNotLockIpBeforeMaxFailedAttempts() {
            // given
            String ipAddress = "192.168.1.4";

            // when - 4회 실패 (최대 5회 미만)
            for (int i = 0; i < 4; i++) {
                auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            }

            // then
            assertThat(auditLogService.isIpLocked(ipAddress)).isFalse();
        }

        @Test
        @DisplayName("등록되지 않은 IP는 실패 횟수가 0이어야 함")
        void shouldReturnZeroForUnknownIp() {
            // given
            String unknownIp = "10.0.0.1";

            // then
            assertThat(auditLogService.getFailedAttempts(unknownIp)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("인증 성공 테스트")
    class AuthSuccessTests {

        @Test
        @DisplayName("인증 성공 시 실패 기록이 초기화되어야 함")
        void shouldResetFailedAttemptsOnSuccess() {
            // given
            String ipAddress = "192.168.1.5";
            auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            assertThat(auditLogService.getFailedAttempts(ipAddress)).isEqualTo(2);

            // when
            auditLogService.logAuthSuccess("testUser", ipAddress, "password");

            // then
            assertThat(auditLogService.getFailedAttempts(ipAddress)).isEqualTo(0);
        }

        @Test
        @DisplayName("인증 성공 시 의심스러운 IP 목록에서 제거되어야 함")
        void shouldRemoveFromSuspiciousIpsOnSuccess() {
            // given
            String ipAddress = "192.168.1.6";
            // 5회 실패로 잠금
            for (int i = 0; i < 5; i++) {
                auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            }
            assertThat(auditLogService.isIpLocked(ipAddress)).isTrue();

            // when
            auditLogService.logAuthSuccess("testUser", ipAddress, "password");

            // then
            assertThat(auditLogService.isIpLocked(ipAddress)).isFalse();
        }
    }

    @Nested
    @DisplayName("IP 잠금 테스트")
    class IpLockoutTests {

        @Test
        @DisplayName("등록되지 않은 IP는 잠금 상태가 아니어야 함")
        void shouldNotBeLocked_whenIpNotRegistered() {
            // given
            String unknownIp = "10.0.0.2";

            // then
            assertThat(auditLogService.isIpLocked(unknownIp)).isFalse();
        }

        @Test
        @DisplayName("잠금 시간이 지나면 자동으로 잠금 해제되어야 함")
        void shouldAutoUnlockAfterDuration() {
            // given
            String ipAddress = "192.168.1.7";

            // 잠금 시간을 과거로 설정 (테스트를 위해)
            Map<String, LocalDateTime> suspiciousIps =
                (Map<String, LocalDateTime>) ReflectionTestUtils.getField(auditLogService, "suspiciousIps");
            suspiciousIps.put(ipAddress, LocalDateTime.now().minusMinutes(20)); // 20분 전 잠금

            // when & then
            assertThat(auditLogService.isIpLocked(ipAddress)).isFalse();
        }

        @Test
        @DisplayName("잠금 시간 내에는 여전히 잠금 상태여야 함")
        void shouldRemainLockedWithinDuration() {
            // given
            String ipAddress = "192.168.1.8";

            // 5회 실패로 잠금
            for (int i = 0; i < 5; i++) {
                auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            }

            // when & then - 잠금 직후이므로 여전히 잠금 상태
            assertThat(auditLogService.isIpLocked(ipAddress)).isTrue();
        }

        @Test
        @DisplayName("수동 잠금 해제가 정상 동작해야 함")
        void shouldUnlockIpManually() {
            // given
            String ipAddress = "192.168.1.9";
            for (int i = 0; i < 5; i++) {
                auditLogService.logAuthFailure("testUser", ipAddress, "잘못된 비밀번호");
            }
            assertThat(auditLogService.isIpLocked(ipAddress)).isTrue();

            // when
            auditLogService.unlockIp(ipAddress);

            // then
            assertThat(auditLogService.isIpLocked(ipAddress)).isFalse();
            assertThat(auditLogService.getFailedAttempts(ipAddress)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("의심스러운 IP 관리 테스트")
    class SuspiciousIpTests {

        @Test
        @DisplayName("보안 위반 시 의심스러운 IP로 등록되어야 함")
        void shouldRegisterSuspiciousIpOnSecurityViolation() {
            // given
            String ipAddress = "192.168.1.10";

            // when
            auditLogService.logSecurityViolation("testUser", ipAddress, "/api/admin", "권한 없는 접근 시도");

            // then
            Map<String, LocalDateTime> suspiciousIps = auditLogService.getSuspiciousIps();
            assertThat(suspiciousIps).containsKey(ipAddress);
        }

        @Test
        @DisplayName("의심스러운 활동 시 의심스러운 IP로 등록되어야 함")
        void shouldRegisterSuspiciousIpOnSuspiciousActivity() {
            // given
            String ipAddress = "192.168.1.11";

            // when
            auditLogService.logSuspiciousActivity("testUser", ipAddress, "/api/data", "비정상적인 요청 패턴");

            // then
            Map<String, LocalDateTime> suspiciousIps = auditLogService.getSuspiciousIps();
            assertThat(suspiciousIps).containsKey(ipAddress);
        }

        @Test
        @DisplayName("의심스러운 IP 목록은 불변 복사본이어야 함")
        void shouldReturnImmutableCopyOfSuspiciousIps() {
            // given
            String ipAddress = "192.168.1.12";
            auditLogService.logSecurityViolation("testUser", ipAddress, "/api/admin", "위반");

            // when
            Map<String, LocalDateTime> suspiciousIps = auditLogService.getSuspiciousIps();

            // then
            assertThat(suspiciousIps).isNotEmpty();
            // 불변 맵이므로 수정 시도 시 예외 발생
            org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> suspiciousIps.put("new.ip", LocalDateTime.now())
            );
        }
    }

    @Nested
    @DisplayName("감사 이벤트 타입 테스트")
    class AuditEventTypeTests {

        @Test
        @DisplayName("모든 이벤트 타입이 설명을 가지고 있어야 함")
        void shouldHaveDescriptionForAllEventTypes() {
            for (AuditLogService.AuditEventType eventType : AuditLogService.AuditEventType.values()) {
                assertThat(eventType.getDescription()).isNotBlank();
            }
        }

        @Test
        @DisplayName("이벤트 타입 개수가 올바라야 함")
        void shouldHaveCorrectNumberOfEventTypes() {
            assertThat(AuditLogService.AuditEventType.values()).hasSize(21);
        }
    }

    @Nested
    @DisplayName("로깅 메서드 테스트")
    class LoggingMethodTests {

        @Test
        @DisplayName("접근 거부 로깅이 예외 없이 실행되어야 함")
        void shouldLogAccessDeniedWithoutException() {
            // when & then - no exception
            auditLogService.logAccessDenied("user1", "192.168.1.1", "/api/admin", "권한 없음");
        }

        @Test
        @DisplayName("Rate Limit 초과 로깅이 예외 없이 실행되어야 함")
        void shouldLogRateLimitHitWithoutException() {
            // when & then - no exception
            auditLogService.logRateLimitHit("user1", "192.168.1.1", "/api/messages", 100);
        }

        @Test
        @DisplayName("파일 업로드 로깅이 예외 없이 실행되어야 함")
        void shouldLogFileUploadWithoutException() {
            // when & then - no exception
            auditLogService.logFileUpload("user1", "192.168.1.1", "test.pdf", 1024L, true);
            auditLogService.logFileUpload("user1", "192.168.1.1", "test.pdf", 1024L, false);
        }

        @Test
        @DisplayName("파일 다운로드 로깅이 예외 없이 실행되어야 함")
        void shouldLogFileDownloadWithoutException() {
            // when & then - no exception
            auditLogService.logFileDownload("user1", "192.168.1.1", 1L, "test.pdf");
        }

        @Test
        @DisplayName("데이터 수정 로깅이 예외 없이 실행되어야 함")
        void shouldLogDataModificationWithoutException() {
            // when & then - no exception
            auditLogService.logDataModification("user1", "192.168.1.1", "Message", "123", "수정됨");
        }

        @Test
        @DisplayName("데이터 삭제 로깅이 예외 없이 실행되어야 함")
        void shouldLogDataDeletionWithoutException() {
            // when & then - no exception
            auditLogService.logDataDeletion("user1", "192.168.1.1", "Message", "123");
        }

        @Test
        @DisplayName("관리자 작업 로깅이 예외 없이 실행되어야 함")
        void shouldLogAdminActionWithoutException() {
            // when & then - no exception
            auditLogService.logAdminAction("admin1", "192.168.1.1", "사용자 차단", "/api/users/123");
        }

        @Test
        @DisplayName("WebSocket 연결 로깅이 예외 없이 실행되어야 함")
        void shouldLogWebSocketConnectWithoutException() {
            // when & then - no exception
            auditLogService.logWebSocketConnect("user1", "192.168.1.1", "session-123");
        }

        @Test
        @DisplayName("WebSocket 연결 해제 로깅이 예외 없이 실행되어야 함")
        void shouldLogWebSocketDisconnectWithoutException() {
            // when & then - no exception
            auditLogService.logWebSocketDisconnect("user1", "192.168.1.1", "session-123", "정상 종료");
        }

        @Test
        @DisplayName("비밀번호 변경 로깅이 예외 없이 실행되어야 함")
        void shouldLogPasswordChangeWithoutException() {
            // when & then - no exception
            auditLogService.logPasswordChange("user1", "192.168.1.1", true);
            auditLogService.logPasswordChange("user1", "192.168.1.1", false);
        }
    }

    @Nested
    @DisplayName("Null 안전성 테스트")
    class NullSafetyTests {

        @Test
        @DisplayName("null 사용자 ID로 로깅해도 예외가 발생하지 않아야 함")
        void shouldHandleNullUserId() {
            // when & then - no exception
            auditLogService.logAuditEvent(
                AuditLogService.AuditEventType.AUTH_FAILURE,
                null, "192.168.1.1", "/api/login", "실패", false
            );
        }

        @Test
        @DisplayName("null IP 주소로 로깅해도 예외가 발생하지 않아야 함")
        void shouldHandleNullIpAddress() {
            // when & then - no exception
            auditLogService.logAuditEvent(
                AuditLogService.AuditEventType.AUTH_FAILURE,
                "user1", null, "/api/login", "실패", false
            );
        }

        @Test
        @DisplayName("null 리소스로 로깅해도 예외가 발생하지 않아야 함")
        void shouldHandleNullResource() {
            // when & then - no exception
            auditLogService.logAuditEvent(
                AuditLogService.AuditEventType.DATA_ACCESS,
                "user1", "192.168.1.1", null, "접근", true
            );
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTests {

        @Test
        @DisplayName("동시에 여러 IP에서 실패해도 정확히 추적되어야 함")
        void shouldTrackMultipleIpsConcurrently() throws InterruptedException {
            // given
            String[] ips = {"192.168.1.100", "192.168.1.101", "192.168.1.102"};

            // when - 동시에 각 IP에서 3회씩 실패
            Thread[] threads = new Thread[ips.length];
            for (int i = 0; i < ips.length; i++) {
                final String ip = ips[i];
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 3; j++) {
                        auditLogService.logAuthFailure("user", ip, "실패");
                    }
                });
                threads[i].start();
            }

            // 모든 스레드 완료 대기
            for (Thread thread : threads) {
                thread.join();
            }

            // then - 각 IP별로 3회씩 실패가 기록되어야 함
            for (String ip : ips) {
                assertThat(auditLogService.getFailedAttempts(ip)).isEqualTo(3);
            }
        }

        @Test
        @DisplayName("동일 IP에서 동시에 실패해도 모두 카운트되어야 함")
        void shouldTrackConcurrentFailuresFromSameIp() throws InterruptedException {
            // given
            String ipAddress = "192.168.1.200";
            int threadCount = 10;

            // when - 10개의 스레드에서 동시에 실패
            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    auditLogService.logAuthFailure("user", ipAddress, "실패");
                });
                threads[i].start();
            }

            // 모든 스레드 완료 대기
            for (Thread thread : threads) {
                thread.join();
            }

            // then - 10회 모두 기록되어야 함
            assertThat(auditLogService.getFailedAttempts(ipAddress)).isEqualTo(10);
        }
    }
}
