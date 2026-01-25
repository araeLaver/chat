package com.beam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class SendDirectMessageRequest {

    @NotNull(message = "수신자 ID는 필수입니다")
    @Positive(message = "수신자 ID는 양수여야 합니다")
    private Long receiverId;

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 5000, message = "메시지는 5000자를 초과할 수 없습니다")
    private String content;

    public SendDirectMessageRequest() {}

    public SendDirectMessageRequest(Long receiverId, String content) {
        this.receiverId = receiverId;
        this.content = content;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
