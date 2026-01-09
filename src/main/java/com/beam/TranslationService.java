package com.beam;

import com.google.cloud.translate.v3.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TranslationService {

    @Value("${translation.enabled:false}")
    private boolean translationEnabled;

    @Value("${google.cloud.project-id:}")
    private String projectId;

    private TranslationServiceClient translationClient;
    private LocationName parent;

    // Supported languages cache
    private final Map<String, String> supportedLanguages = new ConcurrentHashMap<>();

    // Language code mapping
    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
        "ko", "Korean",
        "en", "English",
        "ja", "Japanese",
        "zh", "Chinese",
        "es", "Spanish",
        "fr", "French",
        "de", "German",
        "pt", "Portuguese",
        "ru", "Russian",
        "ar", "Arabic"
    );

    @PostConstruct
    public void init() {
        if (translationEnabled && projectId != null && !projectId.isEmpty()) {
            try {
                translationClient = TranslationServiceClient.create();
                parent = LocationName.of(projectId, "global");
                log.info("Google Cloud Translation initialized for project: {}", projectId);
                loadSupportedLanguages();
            } catch (IOException e) {
                log.error("Failed to initialize Google Cloud Translation: {}", e.getMessage());
                translationEnabled = false;
            }
        } else {
            log.info("Translation service is disabled or not configured");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (translationClient != null) {
            translationClient.close();
        }
    }

    /**
     * Translate text to target language
     */
    public String translate(String text, String targetLanguage) {
        if (!translationEnabled || text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            TranslateTextRequest request = TranslateTextRequest.newBuilder()
                .setParent(parent.toString())
                .setMimeType("text/plain")
                .setTargetLanguageCode(targetLanguage)
                .addContents(text)
                .build();

            TranslateTextResponse response = translationClient.translateText(request);

            if (!response.getTranslationsList().isEmpty()) {
                String translatedText = response.getTranslations(0).getTranslatedText();
                log.debug("Translated '{}' to '{}': '{}'",
                    truncate(text, 50), targetLanguage, truncate(translatedText, 50));
                return translatedText;
            }
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
        }

        return text;
    }

    /**
     * Translate text from source language to target language
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (!translationEnabled || text == null || text.trim().isEmpty()) {
            return text;
        }

        // Skip if same language
        if (sourceLanguage != null && sourceLanguage.equals(targetLanguage)) {
            return text;
        }

        try {
            TranslateTextRequest.Builder requestBuilder = TranslateTextRequest.newBuilder()
                .setParent(parent.toString())
                .setMimeType("text/plain")
                .setTargetLanguageCode(targetLanguage)
                .addContents(text);

            if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
                requestBuilder.setSourceLanguageCode(sourceLanguage);
            }

            TranslateTextResponse response = translationClient.translateText(requestBuilder.build());

            if (!response.getTranslationsList().isEmpty()) {
                return response.getTranslations(0).getTranslatedText();
            }
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
        }

        return text;
    }

    /**
     * Detect the language of the text
     */
    public String detectLanguage(String text) {
        if (!translationEnabled || text == null || text.trim().isEmpty()) {
            return "unknown";
        }

        try {
            DetectLanguageRequest request = DetectLanguageRequest.newBuilder()
                .setParent(parent.toString())
                .setMimeType("text/plain")
                .setContent(text)
                .build();

            DetectLanguageResponse response = translationClient.detectLanguage(request);

            if (!response.getLanguagesList().isEmpty()) {
                DetectedLanguage detected = response.getLanguages(0);
                log.debug("Detected language for '{}': {} (confidence: {})",
                    truncate(text, 30), detected.getLanguageCode(), detected.getConfidence());
                return detected.getLanguageCode();
            }
        } catch (Exception e) {
            log.error("Language detection failed: {}", e.getMessage());
        }

        return "unknown";
    }

    /**
     * Translate multiple texts in batch
     */
    public List<String> translateBatch(List<String> texts, String targetLanguage) {
        if (!translationEnabled || texts == null || texts.isEmpty()) {
            return texts;
        }

        try {
            TranslateTextRequest request = TranslateTextRequest.newBuilder()
                .setParent(parent.toString())
                .setMimeType("text/plain")
                .setTargetLanguageCode(targetLanguage)
                .addAllContents(texts)
                .build();

            TranslateTextResponse response = translationClient.translateText(request);

            List<String> translatedTexts = new ArrayList<>();
            for (Translation translation : response.getTranslationsList()) {
                translatedTexts.add(translation.getTranslatedText());
            }

            log.debug("Batch translated {} texts to {}", texts.size(), targetLanguage);
            return translatedTexts;
        } catch (Exception e) {
            log.error("Batch translation failed: {}", e.getMessage());
        }

        return texts;
    }

    /**
     * Get list of supported languages
     */
    public Map<String, String> getSupportedLanguages() {
        if (supportedLanguages.isEmpty()) {
            // Return default languages if not loaded
            return LANGUAGE_NAMES;
        }
        return Collections.unmodifiableMap(supportedLanguages);
    }

    /**
     * Check if translation service is enabled and configured
     */
    public boolean isEnabled() {
        return translationEnabled && translationClient != null;
    }

    /**
     * Load supported languages from Google Cloud Translation API
     */
    private void loadSupportedLanguages() {
        if (!translationEnabled || translationClient == null) {
            return;
        }

        try {
            GetSupportedLanguagesRequest request = GetSupportedLanguagesRequest.newBuilder()
                .setParent(parent.toString())
                .setDisplayLanguageCode("en")
                .build();

            SupportedLanguages response = translationClient.getSupportedLanguages(request);

            for (SupportedLanguage language : response.getLanguagesList()) {
                supportedLanguages.put(language.getLanguageCode(), language.getDisplayName());
            }

            log.info("Loaded {} supported languages", supportedLanguages.size());
        } catch (Exception e) {
            log.warn("Failed to load supported languages: {}", e.getMessage());
            // Use default languages
            supportedLanguages.putAll(LANGUAGE_NAMES);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
