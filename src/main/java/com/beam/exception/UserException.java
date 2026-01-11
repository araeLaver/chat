package com.beam.exception;

/**
 * 사용자 관련 예외
 */
public class UserException extends ApplicationException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static UserException notFound(Long userId) {
        return new UserException(ErrorCode.USER_NOT_FOUND, "User ID: " + userId);
    }

    public static UserException notFoundByUsername(String username) {
        return new UserException(ErrorCode.USER_NOT_FOUND, "Username: " + username);
    }

    public static UserException notFoundByEmail(String email) {
        return new UserException(ErrorCode.USER_NOT_FOUND, "Email: " + email);
    }

    public static UserException usernameExists(String username) {
        return new UserException(ErrorCode.USER_ALREADY_EXISTS, username);
    }

    public static UserException emailExists(String email) {
        return new UserException(ErrorCode.USER_EMAIL_EXISTS, email);
    }

    public static UserException phoneExists(String phone) {
        return new UserException(ErrorCode.USER_PHONE_EXISTS, phone);
    }

    public static UserException blocked(Long userId) {
        return new UserException(ErrorCode.USER_BLOCKED, "User ID: " + userId);
    }
}
