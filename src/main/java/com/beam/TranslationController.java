package com.beam;

import com.beam.dto.TranslationRequest;
import com.beam.dto.TranslationResponse;
import com.beam.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
@Tag(name = "Translation", description = "Message translation API")
public class TranslationController {

    private final TranslationService translationService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/message")
    @Operation(summary = "Translate a message", description = "Translate text to the target language")
    public ResponseEntity<TranslationResponse> translateMessage(
            @Valid @RequestBody TranslationRequest request) {

        if (!translationService.isEnabled()) {
            return ResponseEntity.ok(TranslationResponse.disabled());
        }

        try {
            String translated;
            String detectedLanguage = null;

            if (request.getSourceLanguage() != null && !request.getSourceLanguage().isEmpty()) {
                translated = translationService.translate(
                    request.getText(),
                    request.getSourceLanguage(),
                    request.getTargetLanguage()
                );
            } else {
                // Auto-detect source language
                detectedLanguage = translationService.detectLanguage(request.getText());
                translated = translationService.translate(
                    request.getText(),
                    request.getTargetLanguage()
                );
            }

            TranslationResponse response = TranslationResponse.builder()
                .success(true)
                .originalText(request.getText())
                .translatedText(translated)
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .detectedLanguage(detectedLanguage)
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Translation error: {}", e.getMessage());
            return ResponseEntity.ok(TranslationResponse.error("Translation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/batch")
    @Operation(summary = "Translate multiple messages", description = "Translate multiple texts to the target language")
    public ResponseEntity<TranslationResponse> translateBatch(
            @Valid @RequestBody TranslationRequest request) {

        if (!translationService.isEnabled()) {
            return ResponseEntity.ok(TranslationResponse.disabled());
        }

        if (request.getTexts() == null || request.getTexts().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(TranslationResponse.error("Texts list is required for batch translation"));
        }

        try {
            List<String> translated = translationService.translateBatch(
                request.getTexts(),
                request.getTargetLanguage()
            );

            TranslationResponse response = TranslationResponse.builder()
                .success(true)
                .targetLanguage(request.getTargetLanguage())
                .translatedTexts(translated)
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Batch translation error: {}", e.getMessage());
            return ResponseEntity.ok(TranslationResponse.error("Batch translation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/detect")
    @Operation(summary = "Detect language", description = "Detect the language of the given text")
    public ResponseEntity<TranslationResponse> detectLanguage(
            @RequestBody Map<String, String> request) {

        if (!translationService.isEnabled()) {
            return ResponseEntity.ok(TranslationResponse.disabled());
        }

        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(TranslationResponse.error("Text is required"));
        }

        try {
            String detected = translationService.detectLanguage(text);

            TranslationResponse response = TranslationResponse.builder()
                .success(true)
                .originalText(text)
                .detectedLanguage(detected)
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Language detection error: {}", e.getMessage());
            return ResponseEntity.ok(TranslationResponse.error("Detection failed: " + e.getMessage()));
        }
    }

    @GetMapping("/languages")
    @Operation(summary = "Get supported languages", description = "Get list of supported languages for translation")
    public ResponseEntity<TranslationResponse> getSupportedLanguages() {
        Map<String, String> languages = translationService.getSupportedLanguages();

        TranslationResponse response = TranslationResponse.builder()
            .success(true)
            .supportedLanguages(languages)
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Get translation service status", description = "Check if translation service is enabled")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", translationService.isEnabled(),
            "languagesCount", translationService.getSupportedLanguages().size()
        ));
    }

    @Operation(summary = "번역 설정 조회", description = "현재 사용자의 번역 설정을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/settings")
    public ResponseEntity<?> getTranslationSettings(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = AuthUtil.extractUserId(authHeader, jwtUtil);
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> settings = new HashMap<>();
            settings.put("autoTranslate", user.getAutoTranslate());
            settings.put("preferredLanguage", user.getPreferredLanguage());
            settings.put("serviceEnabled", translationService.isEnabled());
            settings.put("supportedLanguages", translationService.getSupportedLanguages());

            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "번역 설정 업데이트", description = "사용자의 자동 번역 설정을 업데이트합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/settings")
    public ResponseEntity<?> updateTranslationSettings(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(authHeader, jwtUtil);
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (request.containsKey("autoTranslate")) {
                user.setAutoTranslate((Boolean) request.get("autoTranslate"));
            }
            if (request.containsKey("preferredLanguage")) {
                String preferredLang = (String) request.get("preferredLanguage");
                // 지원하는 언어인지 확인
                if (preferredLang != null && !translationService.getSupportedLanguages().containsKey(preferredLang)) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Unsupported language: " + preferredLang);
                    return ResponseEntity.badRequest().body(error);
                }
                user.setPreferredLanguage(preferredLang);
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Translation settings updated");
            response.put("autoTranslate", user.getAutoTranslate());
            response.put("preferredLanguage", user.getPreferredLanguage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
