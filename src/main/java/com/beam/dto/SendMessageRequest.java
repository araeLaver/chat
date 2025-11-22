package com.beam.dto;

import jakarta.validation.constraints.*;

/**
 * Send Message Request DTO
 *
 * @since 1.2.0
 */
public class SendMessageRequest {

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 5000, message = "메시지는 5000자를 초과할 수 없습니다")
    private String content;

    @Pattern(regexp = "TEXT|IMAGE|FILE|LINK", message = "메시지 타입은 TEXT, IMAGE, FILE, LINK만 가능합니다")
    private String messageType = "TEXT";

    public SendMessageRequest() {
    }

    // Getters
    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    // Setters
    public void setContent(String content) {
        this.content = content;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SendMessageRequest that = (SendMessageRequest) o;
        return java.util.Objects.equals(content, that.content) &&
               java.util.Objects.equals(messageType, that.messageType);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(content, messageType);
    }

    @Override
    public String toString() {
        return "SendMessageRequest{" +
               "content='" + content + '\'' +
               ", messageType='" + messageType + '\'' +
               '}';
    }
}
