package com.beam;

import com.beam.exception.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AuthRateLimiter Tests")
class AuthRateLimiterTest {

    private AuthRateLimiter authRateLimiter;

    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PHONE = "01012345678";

    @BeforeEach
    void setUp() {
        authRateLimiter = new AuthRateLimiter();
        ReflectionTestUtils.setField(authRateLimiter, "maxAttempts", 3);
        ReflectionTestUtils.setField(authRateLimiter, "lockoutMinutes", 15);
        ReflectionTestUtils.setField(authRateLimiter, "verificationMaxAttempts", 3);
        ReflectionTestUtils.setField(authRateLimiter, "verificationLockoutMinutes", 60);
    }

    @Nested
    @DisplayName("Login Rate Limiting Tests")
    class LoginRateLimitingTests {

        @Test
        @DisplayName("Should allow login attempt when no previous failures")
        void shouldAllowLoginAttemptWhenNoPreviousFailures() {
            assertThatCode(() -> authRateLimiter.checkLoginAttempt(TEST_IP))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow login after recording failures below max")
        void shouldAllowLoginAfterRecordingFailuresBelowMax() {
            authRateLimiter.recordLoginFailure(TEST_IP);
            authRateLimiter.recordLoginFailure(TEST_IP);

            assertThatCode(() -> authRateLimiter.checkLoginAttempt(TEST_IP))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should block login after max failures")
        void shouldBlockLoginAfterMaxFailures() {
            // Record max failures to trigger lockout
            for (int i = 0; i < 3; i++) {
                authRateLimiter.recordLoginFailure(TEST_IP);
            }

            assertThatThrownBy(() -> authRateLimiter.checkLoginAttempt(TEST_IP))
                    .isInstanceOf(RateLimitException.class);
        }

        @Test
        @DisplayName("Should reset after login success")
        void shouldResetAfterLoginSuccess() {
            authRateLimiter.recordLoginFailure(TEST_IP);
            authRateLimiter.recordLoginFailure(TEST_IP);
            authRateLimiter.recordLoginSuccess(TEST_IP);

            // After success, should be able to fail again without immediate lockout
            authRateLimiter.recordLoginFailure(TEST_IP);
            assertThatCode(() -> authRateLimiter.checkLoginAttempt(TEST_IP))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Verification Request Rate Limiting Tests")
    class VerificationRequestTests {

        @Test
        @DisplayName("Should allow verification request when no previous attempts")
        void shouldAllowVerificationRequestWhenNoPreviousAttempts() {
            assertThatCode(() -> authRateLimiter.checkVerificationRequest(TEST_EMAIL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow verification request after attempts below max")
        void shouldAllowVerificationRequestAfterAttemptsBelowMax() {
            authRateLimiter.recordVerificationRequest(TEST_EMAIL);
            authRateLimiter.recordVerificationRequest(TEST_EMAIL);

            assertThatCode(() -> authRateLimiter.checkVerificationRequest(TEST_EMAIL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should block verification request after max attempts")
        void shouldBlockVerificationRequestAfterMaxAttempts() {
            for (int i = 0; i < 3; i++) {
                authRateLimiter.recordVerificationRequest(TEST_EMAIL);
            }

            assertThatThrownBy(() -> authRateLimiter.checkVerificationRequest(TEST_EMAIL))
                    .isInstanceOf(RateLimitException.class);
        }
    }

    @Nested
    @DisplayName("Verify Code Rate Limiting Tests")
    class VerifyCodeTests {

        @Test
        @DisplayName("Should allow verify code attempt when no previous failures")
        void shouldAllowVerifyCodeAttemptWhenNoPreviousFailures() {
            assertThatCode(() -> authRateLimiter.checkVerifyCodeAttempt(TEST_PHONE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow verify code after failures below max")
        void shouldAllowVerifyCodeAfterFailuresBelowMax() {
            authRateLimiter.recordVerifyCodeFailure(TEST_PHONE);
            authRateLimiter.recordVerifyCodeFailure(TEST_PHONE);

            assertThatCode(() -> authRateLimiter.checkVerifyCodeAttempt(TEST_PHONE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should block verify code after max failures")
        void shouldBlockVerifyCodeAfterMaxFailures() {
            for (int i = 0; i < 3; i++) {
                authRateLimiter.recordVerifyCodeFailure(TEST_PHONE);
            }

            assertThatThrownBy(() -> authRateLimiter.checkVerifyCodeAttempt(TEST_PHONE))
                    .isInstanceOf(RateLimitException.class);
        }

        @Test
        @DisplayName("Should reset both verification maps after success")
        void shouldResetBothVerificationMapsAfterSuccess() {
            authRateLimiter.recordVerificationRequest(TEST_PHONE);
            authRateLimiter.recordVerifyCodeFailure(TEST_PHONE);
            authRateLimiter.recordVerifyCodeSuccess(TEST_PHONE);

            // After success, both maps should be cleared for this identifier
            assertThatCode(() -> authRateLimiter.checkVerifyCodeAttempt(TEST_PHONE))
                    .doesNotThrowAnyException();
            assertThatCode(() -> authRateLimiter.checkVerificationRequest(TEST_PHONE))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should run cleanup without error")
        void shouldRunCleanupWithoutError() {
            authRateLimiter.recordLoginFailure(TEST_IP);
            authRateLimiter.recordVerificationRequest(TEST_EMAIL);
            authRateLimiter.recordVerifyCodeFailure(TEST_PHONE);

            assertThatCode(() -> authRateLimiter.cleanupExpiredEntries())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Multiple IPs/Identifiers Tests")
    class MultipleIdentifiersTests {

        @Test
        @DisplayName("Should track different IPs separately")
        void shouldTrackDifferentIpsSeparately() {
            String ip1 = "192.168.1.1";
            String ip2 = "192.168.1.2";

            // Lockout ip1
            for (int i = 0; i < 3; i++) {
                authRateLimiter.recordLoginFailure(ip1);
            }

            // ip1 should be blocked
            assertThatThrownBy(() -> authRateLimiter.checkLoginAttempt(ip1))
                    .isInstanceOf(RateLimitException.class);

            // ip2 should still be allowed
            assertThatCode(() -> authRateLimiter.checkLoginAttempt(ip2))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should track different emails separately")
        void shouldTrackDifferentEmailsSeparately() {
            String email1 = "user1@example.com";
            String email2 = "user2@example.com";

            // Lockout email1
            for (int i = 0; i < 3; i++) {
                authRateLimiter.recordVerificationRequest(email1);
            }

            // email1 should be blocked
            assertThatThrownBy(() -> authRateLimiter.checkVerificationRequest(email1))
                    .isInstanceOf(RateLimitException.class);

            // email2 should still be allowed
            assertThatCode(() -> authRateLimiter.checkVerificationRequest(email2))
                    .doesNotThrowAnyException();
        }
    }
}
