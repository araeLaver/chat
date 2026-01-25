package com.beam;

import com.google.api.gax.rpc.ApiException;
import com.google.cloud.translate.v3.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class TranslationService {

    @Value("${translation.enabled:false}")
    private boolean translationEnabled;

    @Value("${google.cloud.project-id:}")
    private String projectId;

    @Value("${translation.timeout-seconds:10}")
    private int timeoutSeconds;

    private TranslationServiceClient translationClient;
    private LocationName parent;
    private ExecutorService translationExecutor;

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
                // 타임아웃 처리를 위한 전용 스레드풀 생성
                translationExecutor = Executors.newFixedThreadPool(5,
                        r -> {
                            Thread t = new Thread(r, "translation-worker");
                            t.setDaemon(true);
                            return t;
                        });
                log.info("Google Cloud Translation initialized for project: {} with {}s timeout",
                        projectId, timeoutSeconds);
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
        if (translationExecutor != null) {
            translationExecutor.shutdown();
            try {
                if (!translationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    translationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                translationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Translate text to target language (with caching and timeout)
     */
    @Cacheable(value = "translations", key = "#text.hashCode() + '_' + #targetLanguage",
               condition = "#text != null && #text.length() < 1000")
    public String translate(String text, String targetLanguage) {
        if (!translationEnabled || text == null || text.trim().isEmpty()) {
            return text;
        }

        try {
            // 타임아웃이 적용된 번역 실행
            Future<String> future = translationExecutor.submit(() -> {
                TranslateTextRequest request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setTargetLanguageCode(targetLanguage)
                    .addContents(text)
                    .build();

                TranslateTextResponse response = translationClient.translateText(request);

                if (!response.getTranslationsList().isEmpty()) {
                    return response.getTranslations(0).getTranslatedText();
                }
                return null;
            });

            String translatedText = future.get(timeoutSeconds, TimeUnit.SECONDS);
            if (translatedText != null) {
                log.debug("Translated '{}' to '{}': '{}' (cache miss)",
                    truncate(text, 50), targetLanguage, truncate(translatedText, 50));
                return translatedText;
            }
        } catch (TimeoutException e) {
            log.warn("Translation timeout after {}s for text: '{}'", timeoutSeconds, truncate(text, 30));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApiException) {
                log.error("Translation API error: {}", cause.getMessage());
            } else {
                log.error("Translation failed: {}", cause != null ? cause.getMessage() : e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Translation interrupted for text: '{}'", truncate(text, 30));
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
        }

        return text;
    }

    /**
     * Translate text asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<String> translateAsync(String text, String targetLanguage) {
        log.debug("Starting async translation to {}", targetLanguage);
        String result = translate(text, targetLanguage);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Translate text from source language to target language (with caching)
     */
    @Cacheable(value = "translations",
               key = "#text.hashCode() + '_' + #sourceLanguage + '_' + #targetLanguage",
               condition = "#text != null && #text.length() < 1000")
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
                log.debug("Translated from {} to {} (cache miss)", sourceLanguage, targetLanguage);
                return response.getTranslations(0).getTranslatedText();
            }
        } catch (Exception e) {
            log.error("Translation failed: {}", e.getMessage());
        }

        return text;
    }

    /**
     * Translate text asynchronously with source language
     */
    @Async("taskExecutor")
    public CompletableFuture<String> translateAsync(String text, String sourceLanguage, String targetLanguage) {
        log.debug("Starting async translation from {} to {}", sourceLanguage, targetLanguage);
        String result = translate(text, sourceLanguage, targetLanguage);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Detect the language of the text (with caching)
     */
    @Cacheable(value = "translations", key = "'detect_' + #text.hashCode()",
               condition = "#text != null && #text.length() < 500")
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
                log.debug("Detected language for '{}': {} (confidence: {}) (cache miss)",
                    truncate(text, 30), detected.getLanguageCode(), detected.getConfidence());
                return detected.getLanguageCode();
            }
        } catch (Exception e) {
            log.error("Language detection failed: {}", e.getMessage());
        }

        return "unknown";
    }

    /**
     * Detect language asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<String> detectLanguageAsync(String text) {
        log.debug("Starting async language detection");
        String result = detectLanguage(text);
        return CompletableFuture.completedFuture(result);
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
     * Translate multiple texts in batch asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<List<String>> translateBatchAsync(List<String> texts, String targetLanguage) {
        log.debug("Starting async batch translation of {} texts to {}", texts.size(), targetLanguage);
        List<String> result = translateBatch(texts, targetLanguage);
        return CompletableFuture.completedFuture(result);
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
