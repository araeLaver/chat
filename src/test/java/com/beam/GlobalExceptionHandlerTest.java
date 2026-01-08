package com.beam;

import com.beam.GlobalExceptionHandler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Nested
    @DisplayName("ErrorResponse Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should create ErrorResponse with default constructor")
        void shouldCreateErrorResponseWithDefaultConstructor() {
            ErrorResponse response = new ErrorResponse();

            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("Should create ErrorResponse with all args constructor")
        void shouldCreateErrorResponseWithAllArgsConstructor() {
            ErrorResponse response = new ErrorResponse(400, "Bad Request", "Test message", "/api/test");

            assertEquals(400, response.getStatus());
            assertEquals("Bad Request", response.getError());
            assertEquals("Test message", response.getMessage());
            assertEquals("/api/test", response.getPath());
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("Should set and get validationErrors")
        void shouldSetAndGetValidationErrors() {
            ErrorResponse response = new ErrorResponse();
            response.setValidationErrors(java.util.Map.of("field", "error"));

            assertNotNull(response.getValidationErrors());
            assertEquals("error", response.getValidationErrors().get("field"));
        }
    }

    @Nested
    @DisplayName("handleSecurityException Tests")
    class HandleSecurityExceptionTests {

        @Test
        @DisplayName("Should handle SecurityException")
        void shouldHandleSecurityException() {
            SecurityException ex = new SecurityException("Security violation");

            ResponseEntity<ErrorResponse> response = handler.handleSecurityException(ex, webRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Security violation", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleAuthenticationException Tests")
    class HandleAuthenticationExceptionTests {

        @Test
        @DisplayName("Should handle AuthenticationException")
        void shouldHandleAuthenticationException() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<ErrorResponse> response = handler.handleAuthenticationException(ex, webRequest);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("인증에 실패했습니다. 다시 로그인해주세요", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleAccessDeniedException Tests")
    class HandleAccessDeniedExceptionTests {

        @Test
        @DisplayName("Should handle AccessDeniedException")
        void shouldHandleAccessDeniedException() {
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, webRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("접근 권한이 없습니다", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleMaxSizeException Tests")
    class HandleMaxSizeExceptionTests {

        @Test
        @DisplayName("Should handle MaxUploadSizeExceededException")
        void shouldHandleMaxUploadSizeExceededException() {
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10485760L);

            ResponseEntity<ErrorResponse> response = handler.handleMaxSizeException(ex, webRequest);

            assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("10MB"));
        }
    }

    @Nested
    @DisplayName("handleIllegalArgumentException Tests")
    class HandleIllegalArgumentExceptionTests {

        @Test
        @DisplayName("Should handle IllegalArgumentException with message")
        void shouldHandleIllegalArgumentExceptionWithMessage() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, webRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Invalid argument", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException without message")
        void shouldHandleIllegalArgumentExceptionWithoutMessage() {
            IllegalArgumentException ex = new IllegalArgumentException();

            ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, webRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("잘못된 요청입니다", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleRuntimeException Tests")
    class HandleRuntimeExceptionTests {

        @Test
        @DisplayName("Should handle RuntimeException with message")
        void shouldHandleRuntimeExceptionWithMessage() {
            RuntimeException ex = new RuntimeException("Runtime error");

            ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex, webRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Runtime error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle RuntimeException without message")
        void shouldHandleRuntimeExceptionWithoutMessage() {
            RuntimeException ex = new RuntimeException();

            ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex, webRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("요청을 처리할 수 없습니다", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleGlobalException Tests")
    class HandleGlobalExceptionTests {

        @Test
        @DisplayName("Should handle generic Exception")
        void shouldHandleGenericException() {
            Exception ex = new Exception("Something went wrong");

            ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, webRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("handleValidationExceptions Tests")
    class HandleValidationExceptionsTests {

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException")
        void shouldHandleMethodArgumentNotValidException() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
            bindingResult.addError(new FieldError("testObject", "fieldName", "must not be blank"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, webRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody().getValidationErrors());
            assertEquals("must not be blank", response.getBody().getValidationErrors().get("fieldName"));
        }
    }
}
