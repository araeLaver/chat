package com.beam;

import com.beam.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("VerificationService Unit Tests")
class VerificationServiceTest {

    private VerificationService verificationService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService();
        user = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("setVerificationCode")
    class SetVerificationCodeTests {

        @Test
        @DisplayName("should set verification code and expiry time")
        void shouldSetCodeAndExpiry() {
            String code = "123456";

            verificationService.setVerificationCode(user, code);

            assertThat(user.getVerificationCode()).isEqualTo(code);
            assertThat(user.getVerificationCodeExpiresAt()).isNotNull();
            assertThat(user.getVerificationCodeExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(user.getVerificationCodeExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(6));
        }
    }

    @Nested
    @DisplayName("validateVerificationCode")
    class ValidateVerificationCodeTests {

        @Test
        @DisplayName("should succeed with valid code")
        void shouldSucceedWithValidCode() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            assertThatCode(() -> verificationService.validateVerificationCode(user, "123456"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw exception for wrong code")
        void shouldThrowForWrongCode() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            assertThatThrownBy(() -> verificationService.validateVerificationCode(user, "000000"))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("should throw exception for null code")
        void shouldThrowForNullCode() {
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            assertThatThrownBy(() -> verificationService.validateVerificationCode(user, "123456"))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("should throw exception for expired code")
        void shouldThrowForExpiredCode() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() -> verificationService.validateVerificationCode(user, "123456"))
                    .isInstanceOf(AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("isValidCode")
    class IsValidCodeTests {

        @Test
        @DisplayName("should return true for valid code")
        void shouldReturnTrueForValidCode() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            assertThat(verificationService.isValidCode(user, "123456")).isTrue();
        }

        @Test
        @DisplayName("should return false for wrong code")
        void shouldReturnFalseForWrongCode() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            assertThat(verificationService.isValidCode(user, "000000")).isFalse();
        }

        @Test
        @DisplayName("should return false for null code")
        void shouldReturnFalseForNullCode() {
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            assertThat(verificationService.isValidCode(user, "123456")).isFalse();
        }

        @Test
        @DisplayName("should return false for expired code")
        void shouldReturnFalseForExpiredCode() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

            assertThat(verificationService.isValidCode(user, "123456")).isFalse();
        }

        @Test
        @DisplayName("should return false for null expiry")
        void shouldReturnFalseForNullExpiry() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(null);

            assertThat(verificationService.isValidCode(user, "123456")).isFalse();
        }
    }

    @Nested
    @DisplayName("clearVerificationCode")
    class ClearVerificationCodeTests {

        @Test
        @DisplayName("should clear code and expiry")
        void shouldClearCodeAndExpiry() {
            user.setVerificationCode("123456");
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            verificationService.clearVerificationCode(user);

            assertThat(user.getVerificationCode()).isNull();
            assertThat(user.getVerificationCodeExpiresAt()).isNull();
        }
    }

    @Nested
    @DisplayName("generateAndSetCode")
    class GenerateAndSetCodeTests {

        @Test
        @DisplayName("should generate and set 6-digit code")
        void shouldGenerateSixDigitCode() {
            String code = verificationService.generateAndSetCode(user);

            assertThat(code).hasSize(6);
            assertThat(code).matches("\\d{6}");
            assertThat(user.getVerificationCode()).isEqualTo(code);
            assertThat(user.getVerificationCodeExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate different codes each time")
        void shouldGenerateDifferentCodes() {
            String code1 = verificationService.generateAndSetCode(user);
            String code2 = verificationService.generateAndSetCode(user);
            String code3 = verificationService.generateAndSetCode(user);

            // At least 2 of 3 should be different (extremely low probability of all same)
            boolean allSame = code1.equals(code2) && code2.equals(code3);
            assertThat(allSame).isFalse();
        }
    }
}
