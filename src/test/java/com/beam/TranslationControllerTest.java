package com.beam;

import com.beam.dto.TranslationRequest;
import com.beam.dto.TranslationResponse;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("TranslationController Tests")
class TranslationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TranslationService translationService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final Long USER_ID = 1L;

    private UserEntity testUser;
    private Map<String, String> supportedLanguages;

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(USER_ID);

        testUser = UserEntity.builder()
                .username("testuser")
                .displayName("Test User")
                .password("password")
                .email("test@test.com")
                .phoneNumber("01012345678")
                .autoTranslate(true)
                .preferredLanguage("ko")
                .build();
        testUser.setId(USER_ID);

        supportedLanguages = new HashMap<>();
        supportedLanguages.put("en", "English");
        supportedLanguages.put("ko", "Korean");
        supportedLanguages.put("ja", "Japanese");
        when(translationService.getSupportedLanguages()).thenReturn(supportedLanguages);
    }

    @Nested
    @DisplayName("POST /api/translate/message Tests")
    class TranslateMessageTests {

        @Test
        @DisplayName("Should translate message with source language")
        void shouldTranslateMessageWithSourceLanguage() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);
            when(translationService.translate("Hello", "en", "ko")).thenReturn("안녕하세요");

            TranslationRequest request = new TranslationRequest();
            request.setText("Hello");
            request.setSourceLanguage("en");
            request.setTargetLanguage("ko");

            mockMvc.perform(post("/api/v1/translate/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.translatedText").value("안녕하세요"));
        }

        @Test
        @DisplayName("Should auto-detect language when source not specified")
        void shouldAutoDetectLanguage() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);
            when(translationService.detectLanguage("Hello")).thenReturn("en");
            when(translationService.translate("Hello", "ko")).thenReturn("안녕하세요");

            TranslationRequest request = new TranslationRequest();
            request.setText("Hello");
            request.setTargetLanguage("ko");

            mockMvc.perform(post("/api/v1/translate/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.detectedLanguage").value("en"));
        }

        @Test
        @DisplayName("Should return disabled response when service disabled")
        void shouldReturnDisabledResponse() throws Exception {
            when(translationService.isEnabled()).thenReturn(false);

            TranslationRequest request = new TranslationRequest();
            request.setText("Hello");
            request.setTargetLanguage("ko");

            mockMvc.perform(post("/api/v1/translate/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should handle translation error")
        void shouldHandleTranslationError() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);
            when(translationService.translate(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("API error"));

            TranslationRequest request = new TranslationRequest();
            request.setText("Hello");
            request.setSourceLanguage("en");
            request.setTargetLanguage("ko");

            mockMvc.perform(post("/api/v1/translate/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/translate/batch Tests")
    class TranslateBatchTests {

        @Test
        @DisplayName("Should translate batch of messages")
        void shouldTranslateBatch() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);
            List<String> translated = Arrays.asList("안녕", "세계");
            when(translationService.translateBatch(anyList(), eq("ko"))).thenReturn(translated);

            TranslationRequest request = new TranslationRequest();
            request.setText("batch"); // Required by validation
            request.setTexts(Arrays.asList("Hello", "World"));
            request.setTargetLanguage("ko");

            mockMvc.perform(post("/api/v1/translate/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.translatedTexts").isArray());
        }

        @Test
        @DisplayName("Should return error when texts list is empty")
        void shouldReturnErrorWhenTextsEmpty() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);

            TranslationRequest request = new TranslationRequest();
            request.setText("batch"); // Required by validation
            request.setTexts(Arrays.asList());
            request.setTargetLanguage("ko");

            mockMvc.perform(post("/api/v1/translate/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/translate/detect Tests")
    class DetectLanguageTests {

        @Test
        @DisplayName("Should detect language")
        void shouldDetectLanguage() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);
            when(translationService.detectLanguage("Hello")).thenReturn("en");

            Map<String, String> request = new HashMap<>();
            request.put("text", "Hello");

            mockMvc.perform(post("/api/v1/translate/detect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.detectedLanguage").value("en"));
        }

        @Test
        @DisplayName("Should return error when text is empty")
        void shouldReturnErrorWhenTextEmpty() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);

            Map<String, String> request = new HashMap<>();
            request.put("text", "");

            mockMvc.perform(post("/api/v1/translate/detect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/translate/languages Tests")
    class GetSupportedLanguagesTests {

        @Test
        @DisplayName("Should get supported languages")
        void shouldGetSupportedLanguages() throws Exception {
            mockMvc.perform(get("/api/v1/translate/languages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.supportedLanguages").isMap());
        }
    }

    @Nested
    @DisplayName("GET /api/translate/status Tests")
    class GetStatusTests {

        @Test
        @DisplayName("Should get translation status")
        void shouldGetTranslationStatus() throws Exception {
            when(translationService.isEnabled()).thenReturn(true);

            mockMvc.perform(get("/api/v1/translate/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.languagesCount").value(3));
        }
    }

    @Nested
    @DisplayName("GET /api/translate/settings Tests")
    class GetSettingsTests {

        @Test
        @DisplayName("Should get translation settings")
        void shouldGetTranslationSettings() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(translationService.isEnabled()).thenReturn(true);

            mockMvc.perform(get("/api/v1/translate/settings")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.autoTranslate").value(true))
                    .andExpect(jsonPath("$.preferredLanguage").value("ko"))
                    .andExpect(jsonPath("$.serviceEnabled").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/translate/settings Tests")
    class UpdateSettingsTests {

        @Test
        @DisplayName("Should update translation settings")
        void shouldUpdateTranslationSettings() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            Map<String, Object> request = new HashMap<>();
            request.put("autoTranslate", false);
            request.put("preferredLanguage", "en");

            mockMvc.perform(put("/api/v1/translate/settings")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should reject unsupported language")
        void shouldRejectUnsupportedLanguage() throws Exception {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            Map<String, Object> request = new HashMap<>();
            request.put("preferredLanguage", "xyz");

            mockMvc.perform(put("/api/v1/translate/settings")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Unsupported language: xyz"));
        }
    }
}
