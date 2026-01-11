package com.beam;

import com.beam.exception.ApplicationException;
import com.beam.exception.RateLimitException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 * - 모든 컨트롤러의 예외를 일관되게 처리
 * - 사용자 친화적인 에러 메시지 제공
 * - 보안을 위해 민감한 정보는 숨김
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 에러 응답 DTO
     */
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> validationErrors;

        public ErrorResponse() {
            this.timestamp = LocalDateTime.now();
        }

        public ErrorResponse(int status, String error, String message, String path) {
            this.timestamp = LocalDateTime.now();
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public Map<String, String> getValidationErrors() { return validationErrors; }
        public void setValidationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
        }
    }

    /**
     * 입력 검증 실패 (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "입력값이 올바르지 않습니다",
            request.getDescription(false).replace("uri=", "")
        );
        errorResponse.setValidationErrors(errors);

        logger.warn("입력 검증 실패: {}", errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 보안 예외 (파일 업로드 등)
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Security Violation",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        logger.error("보안 예외 발생: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * 인증 실패
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication Failed",
            "인증에 실패했습니다. 다시 로그인해주세요",
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("인증 실패: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 권한 부족
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            "접근 권한이 없습니다",
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("접근 거부: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * 파일 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(
            MaxUploadSizeExceededException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "File Too Large",
            "파일 크기가 최대 허용 크기를 초과했습니다 (최대 10MB)",
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("파일 크기 초과: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Argument",
            ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다",
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("잘못된 인자: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 제약 조건 위반 (Path Variable, Request Param 검증)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {

        Map<String, String> errors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                violation -> {
                    String path = violation.getPropertyPath().toString();
                    return path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                },
                ConstraintViolation::getMessage,
                (existing, replacement) -> existing
            ));

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Constraint Violation",
            "입력값 제약 조건을 위반했습니다",
            request.getDescription(false).replace("uri=", "")
        );
        errorResponse.setValidationErrors(errors);

        logger.warn("제약 조건 위반: {}", errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 데이터 무결성 위반 (유니크 제약, 외래키 등)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request) {

        String message = "데이터 무결성 오류가 발생했습니다";
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";

        // 사용자 친화적 메시지로 변환
        if (rootCauseMessage.contains("duplicate") || rootCauseMessage.contains("Duplicate")) {
            message = "이미 존재하는 데이터입니다";
        } else if (rootCauseMessage.contains("foreign key") || rootCauseMessage.contains("FOREIGN KEY")) {
            message = "참조 중인 데이터가 있어 처리할 수 없습니다";
        } else if (rootCauseMessage.contains("cannot be null") || rootCauseMessage.contains("NOT NULL")) {
            message = "필수 입력값이 누락되었습니다";
        }

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Data Integrity Violation",
            message,
            request.getDescription(false).replace("uri=", "")
        );

        logger.error("데이터 무결성 위반: {}", rootCauseMessage);

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * 엔티티를 찾을 수 없음
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Entity Not Found",
            ex.getMessage() != null ? ex.getMessage() : "요청한 데이터를 찾을 수 없습니다",
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("엔티티 없음: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * JSON 파싱 오류
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        String message = "요청 본문을 파싱할 수 없습니다";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            if (cause.getMessage().contains("JSON parse error")) {
                message = "잘못된 JSON 형식입니다";
            } else if (cause.getMessage().contains("Unrecognized field")) {
                message = "알 수 없는 필드가 포함되어 있습니다";
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Request Body",
            message,
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("요청 본문 파싱 오류: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 필수 요청 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Missing Parameter",
            String.format("필수 파라미터 '%s'이(가) 누락되었습니다", ex.getParameterName()),
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("필수 파라미터 누락: {}", ex.getParameterName());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 파라미터 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Type Mismatch",
            String.format("파라미터 '%s'의 값이 올바르지 않습니다. %s 타입이 필요합니다",
                ex.getName(), expectedType),
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("파라미터 타입 불일치: {} - expected {}", ex.getName(), expectedType);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "Method Not Allowed",
            String.format("'%s' 메서드는 지원하지 않습니다", ex.getMethod()),
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("지원하지 않는 HTTP 메서드: {}", ex.getMethod());

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 지원하지 않는 미디어 타입
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            "Unsupported Media Type",
            String.format("'%s' 미디어 타입은 지원하지 않습니다", ex.getContentType()),
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("지원하지 않는 미디어 타입: {}", ex.getContentType());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 핸들러를 찾을 수 없음 (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            String.format("요청한 경로 '%s'를 찾을 수 없습니다", ex.getRequestURL()),
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("핸들러 없음: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * 낙관적 잠금 실패 (동시 수정 충돌)
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Concurrent Modification",
            "다른 사용자가 동시에 수정했습니다. 새로고침 후 다시 시도해주세요",
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("낙관적 잠금 실패: {}", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Rate Limit 예외 (Retry-After 헤더 포함)
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(
            RateLimitException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getHttpStatusCode(),
            ex.getErrorCode().getCode(),
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );

        logger.warn("Rate limit exceeded: {} - {}", ex.getErrorCode().getCode(), ex.getDetail());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));

        return new ResponseEntity<>(errorResponse, headers, ex.getErrorCode().getHttpStatus());
    }

    /**
     * 애플리케이션 커스텀 예외 (비즈니스 로직 예외)
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getHttpStatusCode(),
            ex.getMessage(),  // error 필드에 사용자 친화적 메시지 사용
            ex.getErrorCode().getCode(),  // message 필드에 에러 코드 사용
            request.getDescription(false).replace("uri=", "")
        );

        // 4xx 에러는 warn, 5xx 에러는 error 레벨로 로깅
        if (ex.getHttpStatusCode() >= 500) {
            logger.error("Application exception: {} - {}", ex.getErrorCode().getCode(), ex.getMessage(), ex);
        } else {
            logger.warn("Application exception: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());
        }

        return new ResponseEntity<>(errorResponse, ex.getErrorCode().getHttpStatus());
    }

    /**
     * RuntimeException (일반 비즈니스 로직 예외)
     * @deprecated 새 코드에서는 ApplicationException 사용 권장
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Business Logic Error",
            ex.getMessage() != null ? ex.getMessage() : "요청을 처리할 수 없습니다",
            request.getDescription(false).replace("uri=", "")
        );

        logger.error("런타임 예외: {}", ex.getMessage(), ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 기타 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요",
            request.getDescription(false).replace("uri=", "")
        );

        // 민감한 스택 트레이스는 로그에만 기록
        logger.error("예상치 못한 예외 발생", ex);

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
