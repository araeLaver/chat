package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private AuthRequest validRequest;
    private UserEntity existingUser;

    @BeforeEach
    void setUp() {
        validRequest = new AuthRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");
        validRequest.setPhoneNumber("010-1234-5678");
        validRequest.setDisplayName("Test User");

        existingUser = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .phoneNumber("010-1234-5678")
                .displayName("Test User")
                .isActive(true)
                .isPhoneVerified(false)
                .build();
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

            // When
            AuthResponse response = authService.register(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getMessage()).contains("Verification code sent");
            assertThat(response.getEmailVerified()).isFalse();

            verify(userRepository).save(any(UserEntity.class));
            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Should fail when username already exists")
        void shouldFailWhenUsernameExists() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail when email already exists")
        void shouldFailWhenEmailExists() {
            // Given
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail when email is missing")
        void shouldFailWhenEmailMissing() {
            // Given
            validRequest.setEmail(null);
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email is required");
        }

        @Test
        @DisplayName("Should generate phone number when not provided")
        void shouldGeneratePhoneNumberWhenNotProvided() {
            // Given
            validRequest.setPhoneNumber(null);
            when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

            // When
            AuthResponse response = authService.register(validRequest);

            // Then
            assertThat(response).isNotNull();
            verify(userRepository).save(argThat(user ->
                user.getPhoneNumber() != null && user.getPhoneNumber().startsWith("user_")
            ));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");
            when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);

            // When
            AuthResponse response = authService.login(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getMessage()).isEqualTo("Login successful");

            verify(userRepository).save(argThat(user -> user.getIsOnline()));
        }

        @Test
        @DisplayName("Should fail with wrong password")
        void shouldFailWithWrongPassword() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authService.login(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid username or password");
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid username or password");
        }

        @Test
        @DisplayName("Should fail when account is deactivated")
        void shouldFailWhenAccountDeactivated() {
            // Given
            existingUser.setIsActive(false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.login(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Account is deactivated");
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);

            // When
            authService.logout(1L);

            // Then
            verify(userRepository).save(argThat(user ->
                !user.getIsOnline() && user.getLastSeen() != null
            ));
        }

        @Test
        @DisplayName("Should handle logout for non-existent user gracefully")
        void shouldHandleLogoutForNonExistentUser() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then - should not throw
            assertThatCode(() -> authService.logout(999L)).doesNotThrowAnyException();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            // Given
            existingUser.setVerificationCode("123456");
            existingUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            existingUser.setIsActive(false);

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);
            when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");

            // When
            AuthResponse response = authService.verifyEmail("test@example.com", "123456");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmailVerified()).isTrue();

            verify(userRepository).save(argThat(user ->
                user.getIsActive() && user.getVerificationCode() == null
            ));
        }

        @Test
        @DisplayName("Should fail with wrong verification code")
        void shouldFailWithWrongCode() {
            // Given
            existingUser.setVerificationCode("123456");
            existingUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> authService.verifyEmail("test@example.com", "wrong"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid verification code");
        }

        @Test
        @DisplayName("Should fail with expired code")
        void shouldFailWithExpiredCode() {
            // Given
            existingUser.setVerificationCode("123456");
            existingUser.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> authService.verifyEmail("test@example.com", "123456"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Verification code expired");
        }
    }

    @Nested
    @DisplayName("Phone Verification Tests")
    class PhoneVerificationTests {

        @Test
        @DisplayName("Should send verification code")
        void shouldSendVerificationCode() {
            // Given
            when(userRepository.findByPhoneNumber("010-1234-5678")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);

            // When
            String result = authService.sendVerificationCode("010-1234-5678");

            // Then
            assertThat(result).isEqualTo("Verification code sent");
            verify(userRepository).save(argThat(user ->
                user.getVerificationCode() != null &&
                user.getVerificationCodeExpiresAt() != null
            ));
        }

        @Test
        @DisplayName("Should verify phone number successfully")
        void shouldVerifyPhoneSuccessfully() {
            // Given
            existingUser.setVerificationCode("123456");
            existingUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            when(userRepository.findByPhoneNumber("010-1234-5678")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);

            // When
            boolean result = authService.verifyPhoneNumber("010-1234-5678", "123456");

            // Then
            assertThat(result).isTrue();
            verify(userRepository).save(argThat(user ->
                user.getIsPhoneVerified() && user.getVerificationCode() == null
            ));
        }

        @Test
        @DisplayName("Should return false for wrong phone verification code")
        void shouldReturnFalseForWrongCode() {
            // Given
            existingUser.setVerificationCode("123456");
            existingUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

            when(userRepository.findByPhoneNumber("010-1234-5678")).thenReturn(Optional.of(existingUser));

            // When
            boolean result = authService.verifyPhoneNumber("010-1234-5678", "wrong");

            // Then
            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Resend Verification Email Tests")
    class ResendVerificationTests {

        @Test
        @DisplayName("Should resend verification email")
        void shouldResendVerificationEmail() {
            // Given
            existingUser.setIsActive(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);
            doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

            // When & Then
            assertThatCode(() -> authService.resendVerificationEmail("test@example.com"))
                    .doesNotThrowAnyException();

            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Should fail when email already verified")
        void shouldFailWhenAlreadyVerified() {
            // Given
            existingUser.setIsActive(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> authService.resendVerificationEmail("test@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email already verified");
        }
    }
}
