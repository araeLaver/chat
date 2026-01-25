package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("ChatController Integration Tests")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        // Mock rate limit service
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        // Mock email service
        when(emailService.sendVerificationEmail(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(emailService.sendWelcomeEmail(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));

    }

    @Nested
    @DisplayName("Email Send Code Endpoint Tests")
    class EmailSendCodeTests {

        @Test
        @DisplayName("Should send email verification code successfully")
        void shouldSendEmailVerificationCodeSuccessfully() throws Exception {
            String requestBody = """
                {
                    "email": "newuser@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("인증번호가 이메일로 발송되었습니다"));

            verify(emailService).sendVerificationEmail(eq("newuser@example.com"), anyString());
        }

        @Test
        @DisplayName("Should return error for already registered email")
        void shouldReturnErrorForAlreadyRegisteredEmail() throws Exception {
            // Create existing user
            UserEntity existingUser = UserEntity.builder()
                    .username("existinguser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("existing@example.com")
                    .phoneNumber("01011111111")
                    .displayName("Existing User")
                    .isActive(true)
                    .build();
            userRepository.save(existingUser);

            String requestBody = """
                {
                    "email": "existing@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다"));
        }
    }

    @Nested
    @DisplayName("Email Register Endpoint Tests")
    class EmailRegisterTests {

        @Test
        @DisplayName("Should complete email registration successfully")
        void shouldCompleteEmailRegistrationSuccessfully() throws Exception {
            // Create verified user awaiting registration
            UserEntity user = UserEntity.builder()
                    .username("temp_user")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("register@example.com")
                    .phoneNumber("01022222222")
                    .displayName("Temp User")
                    .isPhoneVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "register@example.com",
                    "displayName": "New User",
                    "username": "newusername"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"))
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.username").value("newusername"));

            verify(emailService).sendWelcomeEmail(eq("register@example.com"), eq("New User"));
        }

        @Test
        @DisplayName("Should return error for unverified email")
        void shouldReturnErrorForUnverifiedEmail() throws Exception {
            // Create unverified user
            UserEntity user = UserEntity.builder()
                    .username("temp_user")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("unverified@example.com")
                    .phoneNumber("01033333333")
                    .displayName("Temp User")
                    .isPhoneVerified(false)
                    .isActive(false)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "unverified@example.com",
                    "displayName": "New User",
                    "username": "newusername"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이메일 인증을 먼저 완료해주세요"));
        }

        @Test
        @DisplayName("Should return error for duplicate username")
        void shouldReturnErrorForDuplicateUsername() throws Exception {
            // Create existing user with username
            UserEntity existingUser = UserEntity.builder()
                    .username("existingname")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("existing2@example.com")
                    .phoneNumber("01044444444")
                    .displayName("Existing User")
                    .isActive(true)
                    .build();
            userRepository.save(existingUser);

            // Create verified user awaiting registration
            UserEntity user = UserEntity.builder()
                    .username("temp_user2")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("register2@example.com")
                    .phoneNumber("01055555555")
                    .displayName("Temp User")
                    .isPhoneVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "register2@example.com",
                    "displayName": "New User",
                    "username": "existingname"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 사용중인 사용자명입니다"));
        }
    }

    @Nested
    @DisplayName("Email Login Endpoint Tests")
    class EmailLoginTests {

        @Test
        @DisplayName("Should send login verification code successfully")
        void shouldSendLoginVerificationCodeSuccessfully() throws Exception {
            // Create active user
            UserEntity user = UserEntity.builder()
                    .username("loginuser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("login@example.com")
                    .phoneNumber("01066666666")
                    .displayName("Login User")
                    .isPhoneVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "login@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("로그인 인증번호가 이메일로 발송되었습니다"));

            verify(emailService).sendVerificationEmail(eq("login@example.com"), anyString());
        }

        @Test
        @DisplayName("Should return error for unregistered email")
        void shouldReturnErrorForUnregisteredEmail() throws Exception {
            String requestBody = """
                {
                    "email": "unknown@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("등록되지 않은 이메일입니다"));
        }

        @Test
        @DisplayName("Should return error for inactive account")
        void shouldReturnErrorForInactiveAccount() throws Exception {
            // Create inactive user
            UserEntity user = UserEntity.builder()
                    .username("inactiveuser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("inactive@example.com")
                    .phoneNumber("01077777777")
                    .displayName("Inactive User")
                    .isPhoneVerified(false)
                    .isActive(false)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "inactive@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("계정이 활성화되지 않았습니다"));
        }
    }

    @Nested
    @DisplayName("Email Login Verify Endpoint Tests")
    class EmailLoginVerifyTests {

        @Test
        @DisplayName("Should verify email login successfully")
        void shouldVerifyEmailLoginSuccessfully() throws Exception {
            // Create user with verification code
            UserEntity user = UserEntity.builder()
                    .username("verifyuser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("verify@example.com")
                    .phoneNumber("01088888888")
                    .displayName("Verify User")
                    .verificationCode("123456")
                    .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5))
                    .isPhoneVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "verify@example.com",
                    "verificationCode": "123456"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/login/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("로그인 성공"))
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.email").value("verify@example.com"));
        }

        @Test
        @DisplayName("Should return error for wrong verification code")
        void shouldReturnErrorForWrongVerificationCode() throws Exception {
            // Create user with verification code
            UserEntity user = UserEntity.builder()
                    .username("wrongcodeuser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("wrongcode@example.com")
                    .phoneNumber("01099999999")
                    .displayName("Wrong Code User")
                    .verificationCode("123456")
                    .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5))
                    .isPhoneVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "wrongcode@example.com",
                    "verificationCode": "654321"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/login/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("인증번호가 일치하지 않습니다"));
        }

        @Test
        @DisplayName("Should return error for expired verification code")
        void shouldReturnErrorForExpiredVerificationCode() throws Exception {
            // Create user with expired verification code
            UserEntity user = UserEntity.builder()
                    .username("expireduser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("expired@example.com")
                    .phoneNumber("01010101010")
                    .displayName("Expired User")
                    .verificationCode("123456")
                    .verificationCodeExpiresAt(LocalDateTime.now().minusMinutes(5))
                    .isPhoneVerified(true)
                    .isActive(true)
                    .build();
            userRepository.save(user);

            String requestBody = """
                {
                    "email": "expired@example.com",
                    "verificationCode": "123456"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/email/login/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("인증번호가 만료되었습니다"));
        }
    }

    @Nested
    @DisplayName("Phone Send Code Endpoint Tests")
    class PhoneSendCodeTests {

        @Test
        @DisplayName("Should return error for already registered phone")
        void shouldReturnErrorForAlreadyRegisteredPhone() throws Exception {
            // Create existing user with phone number
            UserEntity existingUser = UserEntity.builder()
                    .username("phoneuser")
                    .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                    .email("phone@example.com")
                    .phoneNumber("01012345678")
                    .displayName("Phone User")
                    .isActive(true)
                    .build();
            userRepository.save(existingUser);

            String requestBody = """
                {
                    "phoneNumber": "01012345678"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/phone/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 가입된 휴대폰 번호입니다"));
        }
    }
}
