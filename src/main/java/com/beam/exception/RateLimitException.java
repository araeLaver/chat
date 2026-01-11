package com.beam.exception;

/**
 * Rate Limiting 관련 예외
 */
public class RateLimitException extends ApplicationException {

    private final long retryAfterSeconds;

    public RateLimitException(ErrorCode errorCode, long retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitException(ErrorCode errorCode, String detail, long retryAfterSeconds) {
        super(errorCode, detail);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static RateLimitException tooManyRequests(long retryAfterSeconds) {
        return new RateLimitException(ErrorCode.RATE_LIMIT_EXCEEDED, retryAfterSeconds);
    }

    public static RateLimitException tooManyAuthAttempts(long retryAfterSeconds) {
        return new RateLimitException(ErrorCode.RATE_LIMIT_AUTH_EXCEEDED, retryAfterSeconds);
    }

    public static RateLimitException tooManyAuthAttempts(String identifier, long retryAfterSeconds) {
        return new RateLimitException(ErrorCode.RATE_LIMIT_AUTH_EXCEEDED,
            "Identifier: " + identifier, retryAfterSeconds);
    }
}
