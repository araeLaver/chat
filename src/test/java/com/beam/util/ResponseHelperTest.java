package com.beam.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResponseHelper Tests")
class ResponseHelperTest {

    @Nested
    @DisplayName("success() Tests")
    class SuccessTests {

        @Test
        @DisplayName("Should create success response with message")
        void shouldCreateSuccessResponseWithMessage() {
            Map<String, Object> response = ResponseHelper.success("Operation completed");

            assertThat(response.get("success")).isEqualTo(true);
            assertThat(response.get("message")).isEqualTo("Operation completed");
        }

        @Test
        @DisplayName("Should create success response with message and single value")
        void shouldCreateSuccessResponseWithSingleValue() {
            Map<String, Object> response = ResponseHelper.success("Created", "id", 123);

            assertThat(response.get("success")).isEqualTo(true);
            assertThat(response.get("message")).isEqualTo("Created");
            assertThat(response.get("id")).isEqualTo(123);
        }

        @Test
        @DisplayName("Should create success response with message and map data")
        void shouldCreateSuccessResponseWithMapData() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Test");
            data.put("count", 5);

            Map<String, Object> response = ResponseHelper.success("Retrieved", data);

            assertThat(response.get("success")).isEqualTo(true);
            assertThat(response.get("message")).isEqualTo("Retrieved");
            assertThat(response.get("name")).isEqualTo("Test");
            assertThat(response.get("count")).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("error() Tests")
    class ErrorTests {

        @Test
        @DisplayName("Should create error response with message")
        void shouldCreateErrorResponseWithMessage() {
            Map<String, Object> response = ResponseHelper.error("Something went wrong");

            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).isEqualTo("Something went wrong");
        }

        @Test
        @DisplayName("Should create error response with message and error code")
        void shouldCreateErrorResponseWithErrorCode() {
            Map<String, Object> response = ResponseHelper.error("Validation failed", "VALIDATION_ERROR");

            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).isEqualTo("Validation failed");
            assertThat(response.get("errorCode")).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("Should create error response from exception")
        void shouldCreateErrorResponseFromException() {
            Exception exception = new RuntimeException("Database connection failed");
            Map<String, Object> response = ResponseHelper.errorFromException(exception);

            assertThat(response.get("error")).isEqualTo("Database connection failed");
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build success response using builder")
        void shouldBuildSuccessResponse() {
            Map<String, Object> response = ResponseHelper.builder()
                    .success(true)
                    .message("Success")
                    .put("data", "value")
                    .build();

            assertThat(response.get("success")).isEqualTo(true);
            assertThat(response.get("message")).isEqualTo("Success");
            assertThat(response.get("data")).isEqualTo("value");
        }

        @Test
        @DisplayName("Should build failure response using builder")
        void shouldBuildFailureResponse() {
            Map<String, Object> response = ResponseHelper.builder()
                    .success(false)
                    .message("Failed")
                    .build();

            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).isEqualTo("Failed");
        }

        @Test
        @DisplayName("Should build response with multiple values")
        void shouldBuildResponseWithMultipleValues() {
            Map<String, Object> response = ResponseHelper.builder()
                    .put("key1", "value1")
                    .put("key2", 123)
                    .put("key3", true)
                    .build();

            assertThat(response.get("key1")).isEqualTo("value1");
            assertThat(response.get("key2")).isEqualTo(123);
            assertThat(response.get("key3")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should build response without message")
        void shouldBuildResponseWithoutMessage() {
            Map<String, Object> response = ResponseHelper.builder()
                    .success(true)
                    .build();

            assertThat(response.get("success")).isEqualTo(true);
            assertThat(response.containsKey("message")).isFalse();
        }

        @Test
        @DisplayName("Should default to success true")
        void shouldDefaultToSuccessTrue() {
            Map<String, Object> response = ResponseHelper.builder()
                    .build();

            assertThat(response.get("success")).isEqualTo(true);
        }
    }
}
