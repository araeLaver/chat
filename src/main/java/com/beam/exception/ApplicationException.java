package com.beam.exception;

/**
 * 애플리케이션 기본 예외 클래스
 * 모든 비즈니스 예외의 상위 클래스
 */
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    protected ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    protected ApplicationException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
        this.detail = detail;
    }

    protected ApplicationException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }

    public int getHttpStatusCode() {
        return errorCode.getHttpStatus().value();
    }
}
