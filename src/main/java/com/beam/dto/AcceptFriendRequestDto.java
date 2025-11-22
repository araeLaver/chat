package com.beam.dto;

import jakarta.validation.constraints.*;

/**
 * Accept/Reject Friend Request DTO
 *
 * @since 1.2.0
 */
public class AcceptFriendRequestDto {

    @NotNull(message = "요청자 ID는 필수입니다")
    @Positive(message = "유효한 사용자 ID를 입력하세요")
    private Long requesterId;

    public AcceptFriendRequestDto() {
    }

    // Getters
    public Long getRequesterId() {
        return requesterId;
    }

    // Setters
    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcceptFriendRequestDto that = (AcceptFriendRequestDto) o;
        return java.util.Objects.equals(requesterId, that.requesterId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(requesterId);
    }

    @Override
    public String toString() {
        return "AcceptFriendRequestDto{" +
               "requesterId=" + requesterId +
               '}';
    }
}
