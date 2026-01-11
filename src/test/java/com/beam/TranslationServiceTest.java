package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranslationService 테스트")
class TranslationServiceTest {

    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = new TranslationService();
        // 기본적으로 번역 비활성화 상태로 테스트
        ReflectionTestUtils.setField(translationService, "translationEnabled", false);
        ReflectionTestUtils.setField(translationService, "projectId", "");
    }

    @Nested
    @DisplayName("번역 비활성화 상태 테스트")
    class DisabledTranslationTests {

        @Test
        @DisplayName("번역 비활성화 시 원본 텍스트를 반환해야 함")
        void shouldReturnOriginalTextWhenDisabled() {
            // given
            String originalText = "Hello World";

            // when
            String result = translationService.translate(originalText, "ko");

            // then
            assertThat(result).isEqualTo(originalText);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            // when
            String result = translationService.translate(input, "ko");

            // then
            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("비활성화 시 isEnabled()가 false를 반환해야 함")
        void shouldReturnFalseWhenDisabled() {
            assertThat(translationService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("비활성화 시 언어 감지는 'unknown'을 반환해야 함")
        void shouldReturnUnknownForLanguageDetection() {
            // given
            String text = "Hello World";

            // when
            String result = translationService.detectLanguage(text);

            // then
            assertThat(result).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("번역 메서드 테스트")
    class TranslateMethodTests {

        @Test
        @DisplayName("소스 언어와 타겟 언어가 같으면 원본을 반환해야 함")
        void shouldReturnOriginalWhenSameLanguage() {
            // given
            ReflectionTestUtils.setField(translationService, "translationEnabled", true);
            String text = "안녕하세요";

            // when
            String result = translationService.translate(text, "ko", "ko");

            // then
            assertThat(result).isEqualTo(text);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("번역 시 null 또는 빈 텍스트는 그대로 반환")
        void shouldReturnNullOrEmptyTextAsIs(String input) {
            // when
            String result = translationService.translate(input, "en", "ko");

            // then
            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("공백만 있는 텍스트는 그대로 반환해야 함")
        void shouldReturnWhitespaceTextAsIs() {
            // given
            String whitespaceText = "   ";

            // when
            String result = translationService.translate(whitespaceText, "ko");

            // then
            assertThat(result).isEqualTo(whitespaceText);
        }
    }

    @Nested
    @DisplayName("비동기 번역 테스트")
    class AsyncTranslationTests {

        @Test
        @DisplayName("비동기 번역이 CompletableFuture를 반환해야 함")
        void shouldReturnCompletableFuture() {
            // given
            String text = "Hello";

            // when
            CompletableFuture<String> future = translationService.translateAsync(text, "ko");

            // then
            assertThat(future).isNotNull();
            assertThat(future.join()).isEqualTo(text); // 비활성화 상태이므로 원본 반환
        }

        @Test
        @DisplayName("소스 언어 포함 비동기 번역이 CompletableFuture를 반환해야 함")
        void shouldReturnCompletableFutureWithSourceLanguage() {
            // given
            String text = "Hello";

            // when
            CompletableFuture<String> future = translationService.translateAsync(text, "en", "ko");

            // then
            assertThat(future).isNotNull();
            assertThat(future.join()).isEqualTo(text);
        }

        @Test
        @DisplayName("비동기 언어 감지가 CompletableFuture를 반환해야 함")
        void shouldReturnCompletableFutureForDetection() {
            // given
            String text = "Hello World";

            // when
            CompletableFuture<String> future = translationService.detectLanguageAsync(text);

            // then
            assertThat(future).isNotNull();
            assertThat(future.join()).isEqualTo("unknown"); // 비활성화 상태
        }
    }

    @Nested
    @DisplayName("배치 번역 테스트")
    class BatchTranslationTests {

        @Test
        @DisplayName("비활성화 시 배치 번역은 원본 리스트를 반환해야 함")
        void shouldReturnOriginalListWhenDisabled() {
            // given
            List<String> texts = Arrays.asList("Hello", "World", "Test");

            // when
            List<String> result = translationService.translateBatch(texts, "ko");

            // then
            assertThat(result).isEqualTo(texts);
        }

        @Test
        @DisplayName("null 리스트는 null을 반환해야 함")
        void shouldReturnNullForNullList() {
            // when
            List<String> result = translationService.translateBatch(null, "ko");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 리스트는 빈 리스트를 반환해야 함")
        void shouldReturnEmptyListForEmptyInput() {
            // given
            List<String> emptyList = Collections.emptyList();

            // when
            List<String> result = translationService.translateBatch(emptyList, "ko");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("비동기 배치 번역이 CompletableFuture를 반환해야 함")
        void shouldReturnCompletableFutureForBatch() {
            // given
            List<String> texts = Arrays.asList("Hello", "World");

            // when
            CompletableFuture<List<String>> future = translationService.translateBatchAsync(texts, "ko");

            // then
            assertThat(future).isNotNull();
            assertThat(future.join()).isEqualTo(texts);
        }
    }

    @Nested
    @DisplayName("지원 언어 테스트")
    class SupportedLanguagesTests {

        @Test
        @DisplayName("기본 지원 언어 목록을 반환해야 함")
        void shouldReturnDefaultSupportedLanguages() {
            // when
            Map<String, String> languages = translationService.getSupportedLanguages();

            // then
            assertThat(languages).isNotEmpty();
            assertThat(languages).containsKey("ko");
            assertThat(languages).containsKey("en");
            assertThat(languages).containsKey("ja");
            assertThat(languages).containsKey("zh");
        }

        @Test
        @DisplayName("기본 언어 목록에 10개 언어가 포함되어야 함")
        void shouldHave10DefaultLanguages() {
            // when
            Map<String, String> languages = translationService.getSupportedLanguages();

            // then
            assertThat(languages).hasSize(10);
        }

        @Test
        @DisplayName("한국어 언어 코드가 올바른 이름을 가져야 함")
        void shouldHaveCorrectKoreanLanguageName() {
            // when
            Map<String, String> languages = translationService.getSupportedLanguages();

            // then
            assertThat(languages.get("ko")).isEqualTo("Korean");
        }

        @Test
        @DisplayName("영어 언어 코드가 올바른 이름을 가져야 함")
        void shouldHaveCorrectEnglishLanguageName() {
            // when
            Map<String, String> languages = translationService.getSupportedLanguages();

            // then
            assertThat(languages.get("en")).isEqualTo("English");
        }
    }

    @Nested
    @DisplayName("언어 감지 테스트")
    class LanguageDetectionTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 텍스트의 언어 감지는 'unknown'을 반환해야 함")
        void shouldReturnUnknownForNullOrEmpty(String input) {
            // when
            String result = translationService.detectLanguage(input);

            // then
            assertThat(result).isEqualTo("unknown");
        }

        @Test
        @DisplayName("공백만 있는 텍스트의 언어 감지는 'unknown'을 반환해야 함")
        void shouldReturnUnknownForWhitespace() {
            // when
            String result = translationService.detectLanguage("   ");

            // then
            assertThat(result).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("isEnabled() 테스트")
    class IsEnabledTests {

        @Test
        @DisplayName("translationEnabled가 false이면 isEnabled()가 false를 반환")
        void shouldReturnFalseWhenTranslationDisabled() {
            // given
            ReflectionTestUtils.setField(translationService, "translationEnabled", false);

            // then
            assertThat(translationService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("translationClient가 null이면 isEnabled()가 false를 반환")
        void shouldReturnFalseWhenClientIsNull() {
            // given
            ReflectionTestUtils.setField(translationService, "translationEnabled", true);
            ReflectionTestUtils.setField(translationService, "translationClient", null);

            // then
            assertThat(translationService.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("truncate 유틸리티 테스트")
    class TruncateTests {

        @Test
        @DisplayName("null 입력은 null을 반환해야 함")
        void shouldReturnNullForNullInput() throws Exception {
            // given
            java.lang.reflect.Method truncateMethod = TranslationService.class.getDeclaredMethod("truncate", String.class, int.class);
            truncateMethod.setAccessible(true);

            // when
            String result = (String) truncateMethod.invoke(translationService, null, 10);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("최대 길이 이하의 텍스트는 그대로 반환")
        void shouldReturnTextAsIsWhenShorterThanMaxLength() throws Exception {
            // given
            java.lang.reflect.Method truncateMethod = TranslationService.class.getDeclaredMethod("truncate", String.class, int.class);
            truncateMethod.setAccessible(true);
            String shortText = "Hello";

            // when
            String result = (String) truncateMethod.invoke(translationService, shortText, 10);

            // then
            assertThat(result).isEqualTo(shortText);
        }

        @Test
        @DisplayName("최대 길이 초과 텍스트는 잘라서 '...' 추가")
        void shouldTruncateAndAddEllipsis() throws Exception {
            // given
            java.lang.reflect.Method truncateMethod = TranslationService.class.getDeclaredMethod("truncate", String.class, int.class);
            truncateMethod.setAccessible(true);
            String longText = "Hello World, this is a long text";

            // when
            String result = (String) truncateMethod.invoke(translationService, longText, 10);

            // then
            assertThat(result).isEqualTo("Hello Worl...");
            assertThat(result).hasSize(13); // 10 + 3 for "..."
        }

        @Test
        @DisplayName("정확히 최대 길이인 텍스트는 그대로 반환")
        void shouldReturnTextAsIsWhenExactlyMaxLength() throws Exception {
            // given
            java.lang.reflect.Method truncateMethod = TranslationService.class.getDeclaredMethod("truncate", String.class, int.class);
            truncateMethod.setAccessible(true);
            String exactText = "1234567890";

            // when
            String result = (String) truncateMethod.invoke(translationService, exactText, 10);

            // then
            assertThat(result).isEqualTo(exactText);
        }
    }
}
