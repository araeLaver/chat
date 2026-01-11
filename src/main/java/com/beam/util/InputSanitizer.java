package com.beam.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 입력값 살균 유틸리티
 * - XSS 공격 방지
 * - HTML 태그 제거/이스케이프
 * - 스크립트 인젝션 방지
 * - SQL 인젝션 방지 (기본적인 패턴)
 */
@Component
public class InputSanitizer {

    // 위험한 HTML 태그 패턴
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern STYLE_PATTERN = Pattern.compile(
        "<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_PATTERN = Pattern.compile(
        "<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OBJECT_PATTERN = Pattern.compile(
        "<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EMBED_PATTERN = Pattern.compile(
        "<embed[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORM_PATTERN = Pattern.compile(
        "<form[^>]*>.*?</form>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern INPUT_PATTERN = Pattern.compile(
        "<input[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<link[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_PATTERN = Pattern.compile(
        "<meta[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE_PATTERN = Pattern.compile(
        "<base[^>]*>", Pattern.CASE_INSENSITIVE);

    // 이벤트 핸들러 패턴 (onclick, onerror 등)
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile(
        "on\\w+\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE);

    // javascript: 프로토콜 패턴
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile(
        "javascript\\s*:", Pattern.CASE_INSENSITIVE);

    // data: 프로토콜 패턴 (이미지 제외)
    private static final Pattern DATA_PROTOCOL_PATTERN = Pattern.compile(
        "data\\s*:(?!image/)", Pattern.CASE_INSENSITIVE);

    // vbscript: 프로토콜 패턴
    private static final Pattern VBSCRIPT_PROTOCOL_PATTERN = Pattern.compile(
        "vbscript\\s*:", Pattern.CASE_INSENSITIVE);

    // expression() CSS 패턴 (IE 전용 XSS)
    private static final Pattern CSS_EXPRESSION_PATTERN = Pattern.compile(
        "expression\\s*\\(", Pattern.CASE_INSENSITIVE);

    // 모든 HTML 태그 패턴
    private static final Pattern ALL_TAGS_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * 기본 XSS 살균 (메시지 내용용)
     * HTML 특수문자를 이스케이프하고 위험한 패턴 제거
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // 위험한 태그 제거
        result = removeScriptTags(result);

        // 이벤트 핸들러 제거
        result = EVENT_HANDLER_PATTERN.matcher(result).replaceAll("");

        // 위험한 프로토콜 제거
        result = JAVASCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        result = VBSCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        result = DATA_PROTOCOL_PATTERN.matcher(result).replaceAll("");

        // CSS expression 제거
        result = CSS_EXPRESSION_PATTERN.matcher(result).replaceAll("");

        // HTML 특수문자 이스케이프
        result = escapeHtml(result);

        return result;
    }

    /**
     * 엄격한 살균 (사용자 이름, 방 이름 등)
     * 모든 HTML 태그를 제거하고 특수문자 이스케이프
     */
    public String sanitizeStrict(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // 모든 HTML 태그 제거
        result = ALL_TAGS_PATTERN.matcher(result).replaceAll("");

        // 위험한 프로토콜 제거
        result = JAVASCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        result = VBSCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        result = DATA_PROTOCOL_PATTERN.matcher(result).replaceAll("");

        // HTML 특수문자 이스케이프
        result = escapeHtml(result);

        // 앞뒤 공백 제거
        result = result.trim();

        return result;
    }

    /**
     * HTML 이스케이프만 수행 (태그 제거 없이)
     */
    public String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .replace("`", "&#x60;")
            .replace("=", "&#x3D;");
    }

    /**
     * HTML 이스케이프 해제 (디스플레이용)
     */
    public String unescapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
            .replace("&#x60;", "`")
            .replace("&#x3D;", "=");
    }

    /**
     * 위험한 스크립트 태그 제거
     */
    public String removeScriptTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = STYLE_PATTERN.matcher(result).replaceAll("");
        result = IFRAME_PATTERN.matcher(result).replaceAll("");
        result = OBJECT_PATTERN.matcher(result).replaceAll("");
        result = EMBED_PATTERN.matcher(result).replaceAll("");
        result = FORM_PATTERN.matcher(result).replaceAll("");
        result = INPUT_PATTERN.matcher(result).replaceAll("");
        result = LINK_PATTERN.matcher(result).replaceAll("");
        result = META_PATTERN.matcher(result).replaceAll("");
        result = BASE_PATTERN.matcher(result).replaceAll("");

        return result;
    }

    /**
     * URL 살균 (링크용)
     * javascript:, data: 등 위험한 프로토콜 제거
     */
    public String sanitizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        String trimmed = url.trim().toLowerCase();

        // 위험한 프로토콜 체크
        if (trimmed.startsWith("javascript:") ||
            trimmed.startsWith("vbscript:") ||
            trimmed.startsWith("data:") && !trimmed.startsWith("data:image/")) {
            return "";
        }

        // 허용된 프로토콜만 통과
        if (!trimmed.startsWith("http://") &&
            !trimmed.startsWith("https://") &&
            !trimmed.startsWith("mailto:") &&
            !trimmed.startsWith("/") &&
            !trimmed.startsWith("#")) {
            // 프로토콜이 없으면 https:// 추가
            if (!trimmed.contains("://")) {
                return "https://" + url.trim();
            }
            return "";
        }

        return url.trim();
    }

    /**
     * 파일명 살균
     * 경로 탐색 공격 방지
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        // 경로 구분자 제거
        String result = fileName
            .replace("\\", "")
            .replace("/", "")
            .replace("..", "")
            .replace("\0", "");

        // 앞뒤 공백 및 점 제거
        result = result.trim();
        while (result.startsWith(".")) {
            result = result.substring(1);
        }

        return result;
    }

    /**
     * SQL 키워드 체크 (기본적인 방어)
     * 실제로는 PreparedStatement 사용이 필수
     */
    public boolean containsSqlKeywords(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String upper = input.toUpperCase();
        String[] dangerousPatterns = {
            "DROP TABLE", "DROP DATABASE", "DELETE FROM", "TRUNCATE TABLE",
            "INSERT INTO", "UPDATE SET", "UNION SELECT", "OR 1=1",
            "'; DROP", "--", "/*", "*/", "EXEC ", "EXECUTE "
        };

        for (String pattern : dangerousPatterns) {
            if (upper.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 검색 쿼리 살균
     */
    public String sanitizeSearchQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        // 기본 XSS 살균
        String result = sanitize(query);

        // 특수 검색 문자 이스케이프 (Lucene/Elasticsearch용)
        String[] specialChars = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]",
                                  "^", "\"", "~", "*", "?", ":", "\\"};

        for (String special : specialChars) {
            result = result.replace(special, "\\" + special);
        }

        return result.trim();
    }

    /**
     * 입력값이 안전한지 검증
     */
    public boolean isSafe(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }

        // 스크립트 태그 체크
        if (SCRIPT_PATTERN.matcher(input).find()) {
            return false;
        }

        // 이벤트 핸들러 체크
        if (EVENT_HANDLER_PATTERN.matcher(input).find()) {
            return false;
        }

        // 위험한 프로토콜 체크
        if (JAVASCRIPT_PROTOCOL_PATTERN.matcher(input).find() ||
            VBSCRIPT_PROTOCOL_PATTERN.matcher(input).find()) {
            return false;
        }

        return true;
    }

    /**
     * JSON 문자열 살균
     */
    public String sanitizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        return json
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
