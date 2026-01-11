package com.beam.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InputSanitizer 테스트")
class InputSanitizerTest {

    private InputSanitizer inputSanitizer;

    @BeforeEach
    void setUp() {
        inputSanitizer = new InputSanitizer();
    }

    @Nested
    @DisplayName("기본 sanitize() 테스트")
    class BasicSanitizeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.sanitize(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("일반 텍스트는 HTML 이스케이프되어야 함")
        void shouldEscapeNormalText() {
            // given
            String input = "Hello <World> & \"Test\"";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result).contains("&lt;").contains("&gt;").contains("&amp;").contains("&quot;");
        }

        @Test
        @DisplayName("script 태그가 제거되어야 함")
        void shouldRemoveScriptTags() {
            // given
            String input = "Hello<script>alert('XSS')</script>World";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result).doesNotContain("<script>").doesNotContain("</script>");
            assertThat(result).doesNotContain("alert");
        }

        @Test
        @DisplayName("style 태그가 제거되어야 함")
        void shouldRemoveStyleTags() {
            // given
            String input = "Hello<style>body{display:none}</style>World";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result).doesNotContain("<style>").doesNotContain("</style>");
        }

        @Test
        @DisplayName("iframe 태그가 제거되어야 함")
        void shouldRemoveIframeTags() {
            // given
            String input = "Hello<iframe src='evil.com'></iframe>World";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result).doesNotContain("<iframe").doesNotContain("</iframe>");
        }

        @Test
        @DisplayName("이벤트 핸들러가 제거되어야 함")
        void shouldRemoveEventHandlers() {
            // given
            String input = "<img src='x' onerror=\"alert('XSS')\">";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result).doesNotContain("onerror");
            assertThat(result).doesNotContain("alert");
        }

        @Test
        @DisplayName("javascript: 프로토콜이 제거되어야 함")
        void shouldRemoveJavascriptProtocol() {
            // given
            String input = "<a href='javascript:alert(1)'>Click</a>";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result.toLowerCase()).doesNotContain("javascript:");
        }

        @Test
        @DisplayName("vbscript: 프로토콜이 제거되어야 함")
        void shouldRemoveVbscriptProtocol() {
            // given
            String input = "<a href='vbscript:msgbox(1)'>Click</a>";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result.toLowerCase()).doesNotContain("vbscript:");
        }

        @Test
        @DisplayName("CSS expression이 제거되어야 함")
        void shouldRemoveCssExpression() {
            // given
            String input = "background: expression(alert('XSS'))";

            // when
            String result = inputSanitizer.sanitize(input);

            // then
            assertThat(result.toLowerCase()).doesNotContain("expression(");
        }
    }

    @Nested
    @DisplayName("엄격한 sanitizeStrict() 테스트")
    class StrictSanitizeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.sanitizeStrict(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("모든 HTML 태그가 제거되어야 함")
        void shouldRemoveAllHtmlTags() {
            // given
            String input = "<div><p>Hello <strong>World</strong></p></div>";

            // when
            String result = inputSanitizer.sanitizeStrict(input);

            // then
            assertThat(result).doesNotContain("<").doesNotContain(">");
        }

        @Test
        @DisplayName("앞뒤 공백이 제거되어야 함")
        void shouldTrimWhitespace() {
            // given
            String input = "  Hello World  ";

            // when
            String result = inputSanitizer.sanitizeStrict(input);

            // then
            assertThat(result).doesNotStartWith(" ").doesNotEndWith(" ");
        }
    }

    @Nested
    @DisplayName("HTML 이스케이프 테스트")
    class HtmlEscapeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.escapeHtml(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("& 문자가 이스케이프되어야 함")
        void shouldEscapeAmpersand() {
            assertThat(inputSanitizer.escapeHtml("Tom & Jerry")).isEqualTo("Tom &amp; Jerry");
        }

        @Test
        @DisplayName("< 문자가 이스케이프되어야 함")
        void shouldEscapeLessThan() {
            assertThat(inputSanitizer.escapeHtml("a < b")).isEqualTo("a &lt; b");
        }

        @Test
        @DisplayName("> 문자가 이스케이프되어야 함")
        void shouldEscapeGreaterThan() {
            assertThat(inputSanitizer.escapeHtml("a > b")).isEqualTo("a &gt; b");
        }

        @Test
        @DisplayName("큰따옴표가 이스케이프되어야 함")
        void shouldEscapeDoubleQuotes() {
            assertThat(inputSanitizer.escapeHtml("say \"hello\"")).isEqualTo("say &quot;hello&quot;");
        }

        @Test
        @DisplayName("작은따옴표가 이스케이프되어야 함")
        void shouldEscapeSingleQuotes() {
            assertThat(inputSanitizer.escapeHtml("it's")).isEqualTo("it&#x27;s");
        }

        @Test
        @DisplayName("슬래시가 이스케이프되어야 함")
        void shouldEscapeSlash() {
            assertThat(inputSanitizer.escapeHtml("a/b")).isEqualTo("a&#x2F;b");
        }

        @Test
        @DisplayName("백틱이 이스케이프되어야 함")
        void shouldEscapeBacktick() {
            assertThat(inputSanitizer.escapeHtml("code `here`")).isEqualTo("code &#x60;here&#x60;");
        }

        @Test
        @DisplayName("등호가 이스케이프되어야 함")
        void shouldEscapeEquals() {
            assertThat(inputSanitizer.escapeHtml("a=b")).isEqualTo("a&#x3D;b");
        }
    }

    @Nested
    @DisplayName("HTML 언이스케이프 테스트")
    class HtmlUnescapeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.unescapeHtml(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("이스케이프 후 언이스케이프하면 원본과 같아야 함")
        void shouldRoundTripCorrectly() {
            // given
            String original = "Tom & Jerry <3 \"quotes\" 'apostrophe' /slash `backtick` =equals";

            // when
            String escaped = inputSanitizer.escapeHtml(original);
            String unescaped = inputSanitizer.unescapeHtml(escaped);

            // then
            assertThat(unescaped).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("URL 살균 테스트")
    class UrlSanitizeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.sanitizeUrl(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("javascript: URL은 빈 문자열을 반환해야 함")
        void shouldBlockJavascriptUrl() {
            assertThat(inputSanitizer.sanitizeUrl("javascript:alert(1)")).isEmpty();
            assertThat(inputSanitizer.sanitizeUrl("JAVASCRIPT:alert(1)")).isEmpty();
            assertThat(inputSanitizer.sanitizeUrl("JavaScript:alert(1)")).isEmpty();
        }

        @Test
        @DisplayName("vbscript: URL은 빈 문자열을 반환해야 함")
        void shouldBlockVbscriptUrl() {
            assertThat(inputSanitizer.sanitizeUrl("vbscript:msgbox(1)")).isEmpty();
        }

        @Test
        @DisplayName("data: URL은 빈 문자열을 반환해야 함 (이미지 제외)")
        void shouldBlockDataUrl() {
            assertThat(inputSanitizer.sanitizeUrl("data:text/html,<script>alert(1)</script>")).isEmpty();
        }

        @Test
        @DisplayName("data:image/ URL은 허용되어야 함")
        void shouldAllowDataImageUrl() {
            String imageUrl = "data:image/png;base64,abc123";
            assertThat(inputSanitizer.sanitizeUrl(imageUrl)).isEqualTo(imageUrl);
        }

        @Test
        @DisplayName("http:// URL은 허용되어야 함")
        void shouldAllowHttpUrl() {
            String url = "http://example.com";
            assertThat(inputSanitizer.sanitizeUrl(url)).isEqualTo(url);
        }

        @Test
        @DisplayName("https:// URL은 허용되어야 함")
        void shouldAllowHttpsUrl() {
            String url = "https://example.com";
            assertThat(inputSanitizer.sanitizeUrl(url)).isEqualTo(url);
        }

        @Test
        @DisplayName("mailto: URL은 허용되어야 함")
        void shouldAllowMailtoUrl() {
            String url = "mailto:test@example.com";
            assertThat(inputSanitizer.sanitizeUrl(url)).isEqualTo(url);
        }

        @Test
        @DisplayName("상대 경로는 허용되어야 함")
        void shouldAllowRelativePath() {
            assertThat(inputSanitizer.sanitizeUrl("/path/to/page")).isEqualTo("/path/to/page");
        }

        @Test
        @DisplayName("앵커 링크는 허용되어야 함")
        void shouldAllowAnchorLink() {
            assertThat(inputSanitizer.sanitizeUrl("#section")).isEqualTo("#section");
        }

        @Test
        @DisplayName("프로토콜 없는 URL에는 https://가 추가되어야 함")
        void shouldAddHttpsToUrlWithoutProtocol() {
            assertThat(inputSanitizer.sanitizeUrl("example.com")).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("URL 앞뒤 공백이 제거되어야 함")
        void shouldTrimUrl() {
            assertThat(inputSanitizer.sanitizeUrl("  https://example.com  ")).isEqualTo("https://example.com");
        }
    }

    @Nested
    @DisplayName("파일명 살균 테스트")
    class FileNameSanitizeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.sanitizeFileName(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("경로 구분자가 제거되어야 함")
        void shouldRemovePathSeparators() {
            assertThat(inputSanitizer.sanitizeFileName("..\\..\\etc\\passwd")).doesNotContain("\\").doesNotContain("/");
            assertThat(inputSanitizer.sanitizeFileName("../../../etc/passwd")).doesNotContain("/");
        }

        @Test
        @DisplayName("상위 디렉토리 이동 패턴이 제거되어야 함")
        void shouldRemoveParentDirectoryTraversal() {
            assertThat(inputSanitizer.sanitizeFileName("..test.txt")).doesNotContain("..");
        }

        @Test
        @DisplayName("null 문자가 제거되어야 함")
        void shouldRemoveNullCharacter() {
            assertThat(inputSanitizer.sanitizeFileName("test\0.txt")).doesNotContain("\0");
        }

        @Test
        @DisplayName("파일명 앞의 점이 제거되어야 함")
        void shouldRemoveLeadingDots() {
            assertThat(inputSanitizer.sanitizeFileName(".hidden")).isEqualTo("hidden");
            assertThat(inputSanitizer.sanitizeFileName("...hidden")).isEqualTo("hidden");
        }

        @Test
        @DisplayName("정상 파일명은 그대로 유지되어야 함")
        void shouldKeepNormalFileName() {
            assertThat(inputSanitizer.sanitizeFileName("document.pdf")).isEqualTo("document.pdf");
            assertThat(inputSanitizer.sanitizeFileName("my-file_v2.0.txt")).isEqualTo("my-file_v2.0.txt");
        }
    }

    @Nested
    @DisplayName("SQL 키워드 검사 테스트")
    class SqlKeywordTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 false를 반환해야 함")
        void shouldReturnFalseForNullOrEmpty(String input) {
            assertThat(inputSanitizer.containsSqlKeywords(input)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "DROP TABLE users",
            "DROP DATABASE test",
            "DELETE FROM users",
            "TRUNCATE TABLE users",
            "INSERT INTO users",
            "UPDATE SET password",
            "UNION SELECT * FROM",
            "OR 1=1",
            "'; DROP TABLE users--",
            "/* comment */",
            "EXEC sp_executesql"
        })
        @DisplayName("위험한 SQL 패턴이 감지되어야 함")
        void shouldDetectDangerousSqlPatterns(String input) {
            assertThat(inputSanitizer.containsSqlKeywords(input)).isTrue();
        }

        @Test
        @DisplayName("일반 텍스트는 위험하지 않음")
        void shouldNotFlagNormalText() {
            assertThat(inputSanitizer.containsSqlKeywords("Hello World")).isFalse();
            assertThat(inputSanitizer.containsSqlKeywords("Normal message content")).isFalse();
        }

        @Test
        @DisplayName("대소문자 구분 없이 감지되어야 함")
        void shouldDetectCaseInsensitive() {
            assertThat(inputSanitizer.containsSqlKeywords("drop table users")).isTrue();
            assertThat(inputSanitizer.containsSqlKeywords("Drop Table Users")).isTrue();
        }
    }

    @Nested
    @DisplayName("검색 쿼리 살균 테스트")
    class SearchQuerySanitizeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.sanitizeSearchQuery(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("Lucene 특수 문자가 이스케이프되어야 함")
        void shouldEscapeLuceneSpecialChars() {
            // given
            String query = "test + search - query";

            // when
            String result = inputSanitizer.sanitizeSearchQuery(query);

            // then
            assertThat(result).contains("\\+").contains("\\-");
        }

        @Test
        @DisplayName("앞뒤 공백이 제거되어야 함")
        void shouldTrimQuery() {
            assertThat(inputSanitizer.sanitizeSearchQuery("  test  ").trim()).isEqualTo(
                inputSanitizer.sanitizeSearchQuery("test")
            );
        }
    }

    @Nested
    @DisplayName("안전성 검사 테스트")
    class SafetyCheckTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 안전함")
        void shouldBeSafeForNullOrEmpty(String input) {
            assertThat(inputSanitizer.isSafe(input)).isTrue();
        }

        @Test
        @DisplayName("일반 텍스트는 안전함")
        void shouldBeSafeForNormalText() {
            assertThat(inputSanitizer.isSafe("Hello World!")).isTrue();
            assertThat(inputSanitizer.isSafe("일반 메시지입니다.")).isTrue();
        }

        @Test
        @DisplayName("script 태그가 포함되면 안전하지 않음")
        void shouldNotBeSafeWithScriptTag() {
            assertThat(inputSanitizer.isSafe("<script>alert('XSS')</script>")).isFalse();
        }

        @Test
        @DisplayName("이벤트 핸들러가 포함되면 안전하지 않음")
        void shouldNotBeSafeWithEventHandler() {
            assertThat(inputSanitizer.isSafe("<img onerror=\"alert(1)\">")).isFalse();
            assertThat(inputSanitizer.isSafe("<div onclick='evil()'>")).isFalse();
        }

        @Test
        @DisplayName("javascript: 프로토콜이 포함되면 안전하지 않음")
        void shouldNotBeSafeWithJavascriptProtocol() {
            assertThat(inputSanitizer.isSafe("javascript:alert(1)")).isFalse();
        }

        @Test
        @DisplayName("vbscript: 프로토콜이 포함되면 안전하지 않음")
        void shouldNotBeSafeWithVbscriptProtocol() {
            assertThat(inputSanitizer.isSafe("vbscript:msgbox(1)")).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON 살균 테스트")
    class JsonSanitizeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 그대로 반환해야 함")
        void shouldReturnNullOrEmptyAsIs(String input) {
            assertThat(inputSanitizer.sanitizeJson(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("백슬래시가 이스케이프되어야 함")
        void shouldEscapeBackslash() {
            assertThat(inputSanitizer.sanitizeJson("path\\to\\file")).isEqualTo("path\\\\to\\\\file");
        }

        @Test
        @DisplayName("큰따옴표가 이스케이프되어야 함")
        void shouldEscapeDoubleQuotes() {
            assertThat(inputSanitizer.sanitizeJson("say \"hello\"")).isEqualTo("say \\\"hello\\\"");
        }

        @Test
        @DisplayName("개행 문자가 이스케이프되어야 함")
        void shouldEscapeNewlines() {
            assertThat(inputSanitizer.sanitizeJson("line1\nline2")).isEqualTo("line1\\nline2");
        }

        @Test
        @DisplayName("캐리지 리턴이 이스케이프되어야 함")
        void shouldEscapeCarriageReturn() {
            assertThat(inputSanitizer.sanitizeJson("line1\rline2")).isEqualTo("line1\\rline2");
        }

        @Test
        @DisplayName("탭 문자가 이스케이프되어야 함")
        void shouldEscapeTab() {
            assertThat(inputSanitizer.sanitizeJson("col1\tcol2")).isEqualTo("col1\\tcol2");
        }
    }

    @Nested
    @DisplayName("XSS 공격 패턴 테스트")
    class XssAttackPatternTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "<script>alert('XSS')</script>",
            "<SCRIPT>alert('XSS')</SCRIPT>",
            "<ScRiPt>alert('XSS')</ScRiPt>",
            "<script src='evil.js'></script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "<body onload=alert('XSS')>",
            "<iframe src='javascript:alert(1)'></iframe>",
            "<object data='javascript:alert(1)'>",
            "<embed src='javascript:alert(1)'>",
            "<a href='javascript:alert(1)'>click</a>",
            "<div style='background:url(javascript:alert(1))'>",
            "javascript:alert(document.cookie)",
            "<img src=x onerror=\"alert('XSS')\">",
            "<input onfocus=alert('XSS') autofocus>"
        })
        @DisplayName("다양한 XSS 패턴이 무력화되어야 함")
        void shouldNeutralizeXssPatterns(String xssPayload) {
            // when
            String sanitized = inputSanitizer.sanitize(xssPayload);

            // then - 위험한 패턴이 제거되거나 이스케이프되어야 함
            assertThat(sanitized.toLowerCase())
                .doesNotContain("<script")
                .doesNotContain("javascript:")
                .doesNotContain("onerror=")
                .doesNotContain("onload=")
                .doesNotContain("onfocus=");
        }
    }
}
