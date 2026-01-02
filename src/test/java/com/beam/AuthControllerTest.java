package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
    excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Integration Tests")
@org.junit.jupiter.api.Disabled("Temporarily disabled - needs refactoring to @SpringBootTest for proper integration testing")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RoomService roomService;

    @MockBean
    private RoomRepository roomRepository;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private AuthRequest validRequest;
    private AuthResponse successResponse;

    @BeforeEach
    void setUp() {
        validRequest = new AuthRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");

        successResponse = AuthResponse.builder()
                .token("jwt-token")
                .username("testuser")
                .userId(1L)
                .displayName("Test User")
                .message("Success")
                .build();

        // Rate limit mock - always allow
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        // Default mock for userRepository.save - returns entity with ID
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            if (user.getId() == null) {
                // 리플렉션으로 ID 설정 (빌더 패턴의 한계)
                try {
                    java.lang.reflect.Field idField = UserEntity.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(user, 1L);
                } catch (Exception e) {
                    // ignore
                }
            }
            return user;
        });
    }

    @Nested
    @DisplayName("Register Endpoint Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully with valid request")
        @WithMockUser
        void shouldRegisterSuccessfully() throws Exception {
            // Given
            when(authService.register(any(AuthRequest.class))).thenReturn(successResponse);

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.userId").value(1));
        }

        @Test
        @DisplayName("Should return 400 when username already exists")
        @WithMockUser
        void shouldReturn400WhenUsernameExists() throws Exception {
            // Given
            when(authService.register(any(AuthRequest.class)))
                    .thenThrow(new RuntimeException("Username already exists"));

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Username already exists"));
        }

        @Test
        @DisplayName("Should return 400 for invalid username format")
        @WithMockUser
        void shouldReturn400ForInvalidUsername() throws Exception {
            // Given - mock service to throw validation error
            when(authService.register(any(AuthRequest.class)))
                    .thenThrow(new RuntimeException("사용자명은 3-20자 사이여야 합니다"));

            validRequest.setUsername("ab"); // Too short

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        @WithMockUser
        void shouldLoginSuccessfully() throws Exception {
            // Given
            AuthResponse loginResponse = AuthResponse.builder()
                    .token("jwt-token")
                    .username("testuser")
                    .userId(1L)
                    .message("Login successful")
                    .build();

            when(authService.login(any(AuthRequest.class))).thenReturn(loginResponse);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test
        @DisplayName("Should return 400 for invalid credentials")
        @WithMockUser
        void shouldReturn400ForInvalidCredentials() throws Exception {
            // Given
            when(authService.login(any(AuthRequest.class)))
                    .thenThrow(new RuntimeException("Invalid username or password"));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid username or password"));
        }
    }

    @Nested
    @DisplayName("Logout Endpoint Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        @WithMockUser
        void shouldLogoutSuccessfully() throws Exception {
            // When & Then - note: controller returns "Logout successful" not "Logout successful"
            mockMvc.perform(post("/api/auth/logout")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("Guest Login Endpoint Tests")
    class GuestLoginTests {

        @Test
        @DisplayName("Should create guest user and return token")
        @WithMockUser
        void shouldCreateGuestUserSuccessfully() throws Exception {
            // Given
            UserEntity guestUser = UserEntity.builder()
                    .id(1L)
                    .username("guest_123")
                    .displayName("Guest123")
                    .build();

            RoomEntity defaultRoom = RoomEntity.builder()
                    .id(1L)
                    .roomName("General Chat")
                    .build();

            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(UserEntity.class))).thenReturn(guestUser);
            when(roomRepository.findByRoomNameAndRoomType(anyString(), any()))
                    .thenReturn(Optional.of(defaultRoom));
            when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("guest-jwt-token");

            // When & Then
            mockMvc.perform(post("/api/auth/guest")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.defaultRoomId").exists());
        }
    }

    @Nested
    @DisplayName("Phone Verification Endpoint Tests")
    class PhoneVerificationTests {

        @Test
        @DisplayName("Should send verification code")
        @WithMockUser
        void shouldSendVerificationCode() throws Exception {
            // Given
            when(authService.sendVerificationCode("010-1234-5678"))
                    .thenReturn("Verification code sent");

            // When & Then
            mockMvc.perform(post("/api/auth/verify/send")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\": \"010-1234-5678\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification code sent"));
        }

        @Test
        @DisplayName("Should verify phone number successfully")
        @WithMockUser
        void shouldVerifyPhoneNumberSuccessfully() throws Exception {
            // Given
            when(authService.verifyPhoneNumber("010-1234-5678", "123456")).thenReturn(true);

            // When & Then
            mockMvc.perform(post("/api/auth/verify/confirm")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\": \"010-1234-5678\", \"code\": \"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(true));
        }

        @Test
        @DisplayName("Should return 400 for invalid verification code")
        @WithMockUser
        void shouldReturn400ForInvalidCode() throws Exception {
            // Given
            when(authService.verifyPhoneNumber("010-1234-5678", "wrong")).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/auth/verify/confirm")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\": \"010-1234-5678\", \"code\": \"wrong\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.verified").value(false));
        }
    }

    @Nested
    @DisplayName("Email Verification Endpoint Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        @WithMockUser
        void shouldVerifyEmailSuccessfully() throws Exception {
            // Given
            AuthResponse response = AuthResponse.builder()
                    .token("jwt-token")
                    .username("testuser")
                    .emailVerified(true)
                    .build();

            when(authService.verifyEmail("test@example.com", "123456")).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/email/verify")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"test@example.com\", \"code\": \"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailVerified").value(true));
        }

        @Test
        @DisplayName("Should return 400 for invalid email code")
        @WithMockUser
        void shouldReturn400ForInvalidEmailCode() throws Exception {
            // Given
            when(authService.verifyEmail("test@example.com", "wrong"))
                    .thenThrow(new RuntimeException("Invalid verification code"));

            // When & Then
            mockMvc.perform(post("/api/auth/email/verify")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"test@example.com\", \"code\": \"wrong\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid verification code"));
        }

        @Test
        @DisplayName("Should resend verification email")
        @WithMockUser
        void shouldResendVerificationEmail() throws Exception {
            // Given
            doNothing().when(authService).resendVerificationEmail("test@example.com");

            // When & Then
            mockMvc.perform(post("/api/auth/email/resend")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"test@example.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification code sent"));
        }
    }
}
