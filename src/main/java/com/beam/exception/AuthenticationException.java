package com.beam.exception;

/**
 * 인증 관련 예외
 */
public class AuthenticationException extends ApplicationException {

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    public static AuthenticationException tokenInvalid() {
        return new AuthenticationException(ErrorCode.AUTH_TOKEN_INVALID);
    }

    public static AuthenticationException accountDisabled() {
        return new AuthenticationException(ErrorCode.AUTH_ACCOUNT_DISABLED);
    }

    public static AuthenticationException accountLocked() {
        return new AuthenticationException(ErrorCode.AUTH_ACCOUNT_LOCKED);
    }

    public static AuthenticationException verificationFailed() {
        return new AuthenticationException(ErrorCode.AUTH_VERIFICATION_FAILED);
    }

    public static AuthenticationException verificationExpired() {
        return new AuthenticationException(ErrorCode.AUTH_VERIFICATION_EXPIRED);
    }
}
