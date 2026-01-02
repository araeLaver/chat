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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

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
    private SmsService smsService;

    @MockBean
    private RateLimitService rateLimitService;

    private AuthRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AuthRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");

        // Mock email service
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Mock rate limit service to always allow
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);
    }

    @Nested
    @DisplayName("Register Endpoint Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully with valid request")
        void shouldRegisterSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.message").exists());

            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        }

        @Test
        @DisplayName("Should return 400 when username already exists")
        void shouldReturn400WhenUsernameExists() throws Exception {
            // First registration
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());

            // Second registration with same username
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Username already exists"));
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400WhenEmailExists() throws Exception {
            // First registration
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());

            // Second registration with different username but same email
            validRequest.setUsername("anotheruser");
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Email already registered"));
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginTests {

        @BeforeEach
        void createUser() throws Exception {
            // Register and verify user first
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());

            // Manually activate user for login test
            UserEntity user = userRepository.findByUsername("testuser").orElseThrow();
            user.setIsActive(true);
            userRepository.save(user);
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            AuthRequest loginRequest = new AuthRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test
        @DisplayName("Should return 400 for wrong password")
        void shouldReturn400ForWrongPassword() throws Exception {
            AuthRequest loginRequest = new AuthRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid username or password"));
        }

        @Test
        @DisplayName("Should return 400 for non-existent user")
        void shouldReturn400ForNonExistentUser() throws Exception {
            AuthRequest loginRequest = new AuthRequest();
            loginRequest.setUsername("nonexistent");
            loginRequest.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid username or password"));
        }
    }

    @Nested
    @DisplayName("Logout Endpoint Tests")
    class LogoutTests {

        private String token;

        @BeforeEach
        void loginUser() throws Exception {
            // Register and activate user
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)));

            UserEntity user = userRepository.findByUsername("testuser").orElseThrow();
            user.setIsActive(true);
            userRepository.save(user);

            token = jwtUtil.generateToken("testuser", user.getId());
        }

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful"));
        }
    }

    @Nested
    @DisplayName("Guest Login Endpoint Tests")
    class GuestLoginTests {

        @Test
        @DisplayName("Should create guest user and return token")
        void shouldCreateGuestUserSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/guest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.displayName").exists())
                    .andExpect(jsonPath("$.defaultRoomId").exists());
        }

        @Test
        @DisplayName("Multiple guests should have different usernames")
        void multipleGuestsShouldHaveDifferentUsernames() throws Exception {
            String response1 = mockMvc.perform(post("/api/auth/guest"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Thread.sleep(10); // Ensure different timestamp

            String response2 = mockMvc.perform(post("/api/auth/guest"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Parse and compare usernames
            var mapper = new ObjectMapper();
            var json1 = mapper.readTree(response1);
            var json2 = mapper.readTree(response2);

            Assertions.assertNotEquals(
                json1.get("user").get("username").asText(),
                json2.get("user").get("username").asText()
            );
        }
    }

    @Nested
    @DisplayName("Email Verification Endpoint Tests")
    class EmailVerificationTests {

        @BeforeEach
        void registerUser() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)));
        }

        @Test
        @DisplayName("Should verify email with correct code")
        void shouldVerifyEmailSuccessfully() throws Exception {
            UserEntity user = userRepository.findByEmail("test@example.com").orElseThrow();
            String code = user.getVerificationCode();

            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"test@example.com\", \"code\": \"" + code + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailVerified").value(true))
                    .andExpect(jsonPath("$.token").exists());

            // Verify user is now active
            UserEntity verifiedUser = userRepository.findByEmail("test@example.com").orElseThrow();
            Assertions.assertTrue(verifiedUser.getIsActive());
        }

        @Test
        @DisplayName("Should return 400 for invalid verification code")
        void shouldReturn400ForInvalidCode() throws Exception {
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"test@example.com\", \"code\": \"wrongcode\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid verification code"));
        }

        @Test
        @DisplayName("Should resend verification email")
        void shouldResendVerificationEmail() throws Exception {
            mockMvc.perform(post("/api/auth/email/resend")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"test@example.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification code sent"));

            verify(emailService, times(2)).sendVerificationEmail(eq("test@example.com"), anyString());
        }
    }
}
