package com.beam.exception;

/**
 * 파일 관련 예외
 */
public class FileException extends ApplicationException {

    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public FileException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    public static FileException notFound(Long fileId) {
        return new FileException(ErrorCode.FILE_NOT_FOUND, "File ID: " + fileId);
    }

    public static FileException uploadFailed(String reason) {
        return new FileException(ErrorCode.FILE_UPLOAD_FAILED, reason);
    }

    public static FileException uploadFailed(String reason, Throwable cause) {
        return new FileException(ErrorCode.FILE_UPLOAD_FAILED, reason, cause);
    }

    public static FileException typeNotAllowed(String fileType) {
        return new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "Type: " + fileType);
    }

    public static FileException tooLarge(long size, long maxSize) {
        return new FileException(ErrorCode.FILE_TOO_LARGE,
            String.format("Size: %d bytes, Max: %d bytes", size, maxSize));
    }

    public static FileException empty() {
        return new FileException(ErrorCode.FILE_EMPTY);
    }

    public static FileException securityViolation(String reason) {
        return new FileException(ErrorCode.FILE_SECURITY_VIOLATION, reason);
    }
}
