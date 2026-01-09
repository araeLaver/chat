package com.beam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {

    private boolean success;
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private String detectedLanguage;
    private String error;

    // For batch translation
    private List<String> translatedTexts;

    // For supported languages
    private Map<String, String> supportedLanguages;

    public static TranslationResponse success(String original, String translated,
                                               String source, String target) {
        return TranslationResponse.builder()
            .success(true)
            .originalText(original)
            .translatedText(translated)
            .sourceLanguage(source)
            .targetLanguage(target)
            .build();
    }

    public static TranslationResponse error(String message) {
        return TranslationResponse.builder()
            .success(false)
            .error(message)
            .build();
    }

    public static TranslationResponse disabled() {
        return TranslationResponse.builder()
            .success(false)
            .error("Translation service is not enabled")
            .build();
    }
}
