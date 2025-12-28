package com.beam.util;

import java.util.HashMap;
import java.util.Map;

/**
 * API 응답 생성 유틸리티
 * 일관된 응답 형식을 제공
 */
public final class ResponseHelper {

    private ResponseHelper() {
        // 유틸리티 클래스
    }

    public static Map<String, Object> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    public static Map<String, Object> success(String message, String key, Object value) {
        Map<String, Object> response = success(message);
        response.put(key, value);
        return response;
    }

    public static Map<String, Object> success(String message, Map<String, Object> data) {
        Map<String, Object> response = success(message);
        response.putAll(data);
        return response;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    public static Map<String, Object> error(String message, String errorCode) {
        Map<String, Object> response = error(message);
        response.put("errorCode", errorCode);
        return response;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();
        private boolean success = true;
        private String message;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder put(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            if (message != null) {
                response.put("message", message);
            }
            response.putAll(data);
            return response;
        }
    }
}
