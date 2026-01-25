package com.beam.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 에러 코드 정의
 */
public enum ErrorCode {

    // 인증 관련 (AUTH_xxx)
    AUTH_INVALID_CREDENTIALS("AUTH_001", "Invalid username or password", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED("AUTH_002", "Token has expired", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("AUTH_003", "Invalid token", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_DISABLED("AUTH_004", "Account is disabled", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_LOCKED("AUTH_005", "Account is locked due to too many failed attempts", HttpStatus.LOCKED),
    AUTH_VERIFICATION_FAILED("AUTH_006", "Verification failed", HttpStatus.BAD_REQUEST),
    AUTH_VERIFICATION_EXPIRED("AUTH_007", "Verification code has expired", HttpStatus.BAD_REQUEST),

    // 사용자 관련 (USER_xxx)
    USER_NOT_FOUND("USER_001", "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_002", "Username already exists", HttpStatus.CONFLICT),
    USER_EMAIL_EXISTS("USER_003", "Email already registered", HttpStatus.CONFLICT),
    USER_PHONE_EXISTS("USER_004", "Phone number already registered", HttpStatus.CONFLICT),
    USER_BLOCKED("USER_005", "User is blocked", HttpStatus.FORBIDDEN),

    // 채팅방 관련 (ROOM_xxx)
    ROOM_NOT_FOUND("ROOM_001", "Chat room not found", HttpStatus.NOT_FOUND),
    ROOM_ACCESS_DENIED("ROOM_002", "Access to room denied", HttpStatus.FORBIDDEN),
    ROOM_FULL("ROOM_003", "Chat room is full", HttpStatus.BAD_REQUEST),
    ROOM_ALREADY_MEMBER("ROOM_004", "Already a member of this room", HttpStatus.CONFLICT),
    ROOM_NOT_MEMBER("ROOM_005", "Not a member of this room", HttpStatus.FORBIDDEN),
    ROOM_OWNER_ONLY("ROOM_006", "Only room owner can perform this action", HttpStatus.FORBIDDEN),
    ROOM_PERMISSION_DENIED("ROOM_007", "No permission to perform this action", HttpStatus.FORBIDDEN),
    ROOM_CANNOT_REMOVE_OWNER("ROOM_008", "Cannot remove room owner", HttpStatus.BAD_REQUEST),
    ROOM_USER_MUTED("ROOM_009", "You are muted in this room", HttpStatus.FORBIDDEN),

    // 메시지 관련 (MSG_xxx)
    MESSAGE_NOT_FOUND("MSG_001", "Message not found", HttpStatus.NOT_FOUND),
    MESSAGE_SEND_FAILED("MSG_002", "Failed to send message", HttpStatus.INTERNAL_SERVER_ERROR),
    MESSAGE_REACTION_EXISTS("MSG_003", "Reaction already exists", HttpStatus.CONFLICT),
    MESSAGE_READ_UNAUTHORIZED("MSG_004", "Not authorized to mark this message as read", HttpStatus.FORBIDDEN),

    // 파일 관련 (FILE_xxx)
    FILE_NOT_FOUND("FILE_001", "File not found", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED("FILE_002", "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TYPE_NOT_ALLOWED("FILE_003", "File type not allowed", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_004", "File size exceeds limit", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_EMPTY("FILE_005", "File is empty", HttpStatus.BAD_REQUEST),
    FILE_SECURITY_VIOLATION("FILE_006", "File security validation failed", HttpStatus.BAD_REQUEST),
    FILE_DELETE_DENIED("FILE_007", "No permission to delete this file", HttpStatus.FORBIDDEN),
    FILE_THUMBNAIL_NOT_FOUND("FILE_008", "Thumbnail not found", HttpStatus.NOT_FOUND),

    // 친구 관련 (FRIEND_xxx)
    FRIEND_REQUEST_EXISTS("FRIEND_001", "Friend request already exists", HttpStatus.CONFLICT),
    FRIEND_REQUEST_NOT_FOUND("FRIEND_002", "Friend request not found", HttpStatus.NOT_FOUND),
    FRIEND_ALREADY_FRIENDS("FRIEND_003", "Already friends", HttpStatus.CONFLICT),
    FRIEND_SELF_REQUEST("FRIEND_004", "Cannot send friend request to yourself", HttpStatus.BAD_REQUEST),
    FRIEND_BLOCKED_USER("FRIEND_005", "Cannot send friend request to blocked user", HttpStatus.FORBIDDEN),
    FRIEND_REQUEST_NOT_PENDING("FRIEND_006", "Friend request is not pending", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_UNAUTHORIZED("FRIEND_007", "Not authorized to handle this friend request", HttpStatus.FORBIDDEN),

    // Rate Limiting (RATE_xxx)
    RATE_LIMIT_EXCEEDED("RATE_001", "Too many requests", HttpStatus.TOO_MANY_REQUESTS),
    RATE_LIMIT_AUTH_EXCEEDED("RATE_002", "Too many authentication attempts", HttpStatus.TOO_MANY_REQUESTS),

    // 암호화 관련 (CRYPTO_xxx)
    CRYPTO_KEY_GENERATION_FAILED("CRYPTO_001", "Key generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    CRYPTO_ENCRYPTION_FAILED("CRYPTO_002", "Encryption failed", HttpStatus.INTERNAL_SERVER_ERROR),
    CRYPTO_DECRYPTION_FAILED("CRYPTO_003", "Decryption failed", HttpStatus.INTERNAL_SERVER_ERROR),
    CRYPTO_KEY_DERIVATION_FAILED("CRYPTO_004", "Key derivation failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // 일반 에러 (GENERAL_xxx)
    INVALID_INPUT("GEN_001", "Invalid input", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("GEN_002", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("GEN_003", "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
