package com.beam.controller;

import com.beam.JwtUtil;
import com.beam.UserEntity;
import com.beam.UserRepository;
import com.beam.dto.PrivacySettingsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PrivacySettingsController Tests")
class PrivacySettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(USER_ID);

        testUser = UserEntity.builder()
                .username("testuser")
                .displayName("Test User")
                .password("password")
                .email("test@test.com")
                .phoneNumber("01012345678")
                .profileImage("profile.jpg")
                .ghostMode(false)
                .disableIpLogging(false)
                .autoTranslate(true)
                .preferredLanguage("ko")
                .isAnonymous(false)
                .isOnline(true)
                .lastSeen(LocalDateTime.now())
                .build();
        testUser.setId(USER_ID);
    }

    @Nested
    @DisplayName("GET /api/privacy/settings Tests")
    class GetPrivacySettingsTests {

        @Test
        @DisplayName("Should get privacy settings successfully")
        void shouldGetPrivacySettingsSuccessfully() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            mockMvc.perform(get("/api/v1/privacy/settings")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ghostMode").value(false))
                    .andExpect(jsonPath("$.disableIpLogging").value(false))
                    .andExpect(jsonPath("$.autoTranslate").value(true))
                    .andExpect(jsonPath("$.preferredLanguage").value("ko"));
        }

        @Test
        @DisplayName("Should return error when user not found")
        void shouldReturnErrorWhenUserNotFound() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/privacy/settings")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/privacy/settings Tests")
    class UpdatePrivacySettingsTests {

        @Test
        @DisplayName("Should update privacy settings successfully")
        void shouldUpdatePrivacySettingsSuccessfully() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            PrivacySettingsRequest request = new PrivacySettingsRequest();
            request.setGhostMode(true);
            request.setDisableIpLogging(true);
            request.setAutoTranslate(false);
            request.setPreferredLanguage("en");

            mockMvc.perform(put("/api/v1/privacy/settings")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Privacy settings updated"));

            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should update partial settings")
        void shouldUpdatePartialSettings() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            PrivacySettingsRequest request = new PrivacySettingsRequest();
            request.setGhostMode(true);
            // Only update ghostMode, leave others null

            mockMvc.perform(put("/api/v1/privacy/settings")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(userRepository).save(any(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/privacy/ghost-mode Tests")
    class ToggleGhostModeTests {

        @Test
        @DisplayName("Should enable ghost mode when disabled")
        void shouldEnableGhostModeWhenDisabled() throws Exception {
            testUser.setGhostMode(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            mockMvc.perform(put("/api/v1/privacy/ghost-mode")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.ghostMode").value(true))
                    .andExpect(jsonPath("$.message").value("Ghost mode enabled"));
        }

        @Test
        @DisplayName("Should disable ghost mode when enabled")
        void shouldDisableGhostModeWhenEnabled() throws Exception {
            testUser.setGhostMode(true);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            mockMvc.perform(put("/api/v1/privacy/ghost-mode")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.ghostMode").value(false))
                    .andExpect(jsonPath("$.message").value("Ghost mode disabled"));
        }
    }

    @Nested
    @DisplayName("GET /api/privacy/user-status/{targetUserId} Tests")
    class GetUserStatusTests {

        @Test
        @DisplayName("Should get user status when not in ghost mode")
        void shouldGetUserStatusWhenNotInGhostMode() throws Exception {
            UserEntity targetUser = UserEntity.builder()
                    .username("targetuser")
                    .displayName("Target User")
                    .password("password")
                    .email("target@test.com")
                    .phoneNumber("01012345679")
                    .profileImage("target-profile.jpg")
                    .ghostMode(false)
                    .isOnline(true)
                    .lastSeen(LocalDateTime.now())
                    .build();
            targetUser.setId(TARGET_USER_ID);
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

            mockMvc.perform(get("/api/v1/privacy/user-status/{targetUserId}", TARGET_USER_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(TARGET_USER_ID))
                    .andExpect(jsonPath("$.displayName").value("Target User"))
                    .andExpect(jsonPath("$.isOnline").value(true));
        }

        @Test
        @DisplayName("Should hide online status when ghost mode enabled")
        void shouldHideOnlineStatusWhenGhostModeEnabled() throws Exception {
            UserEntity targetUser = UserEntity.builder()
                    .username("targetuser")
                    .displayName("Target User")
                    .password("password")
                    .email("target@test.com")
                    .phoneNumber("01012345679")
                    .ghostMode(true)
                    .isOnline(true)
                    .lastSeen(LocalDateTime.now())
                    .build();
            targetUser.setId(TARGET_USER_ID);
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(targetUser));

            mockMvc.perform(get("/api/v1/privacy/user-status/{targetUserId}", TARGET_USER_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(TARGET_USER_ID))
                    .andExpect(jsonPath("$.isOnline").value(false))
                    .andExpect(jsonPath("$.lastSeen").isEmpty());
        }

        @Test
        @DisplayName("Should return error when target user not found")
        void shouldReturnErrorWhenTargetUserNotFound() throws Exception {
            when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/privacy/user-status/{targetUserId}", TARGET_USER_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }
}
