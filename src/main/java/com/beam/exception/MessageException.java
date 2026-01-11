package com.beam.exception;

/**
 * 메시지 관련 예외
 */
public class MessageException extends ApplicationException {

    public MessageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MessageException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public static MessageException notFound(Long messageId) {
        return new MessageException(ErrorCode.MESSAGE_NOT_FOUND, "Message ID: " + messageId);
    }

    public static MessageException sendFailed(String reason) {
        return new MessageException(ErrorCode.MESSAGE_SEND_FAILED, reason);
    }
}
