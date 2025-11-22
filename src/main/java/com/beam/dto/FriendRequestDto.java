package com.beam.dto;

import jakarta.validation.constraints.*;

/**
 * Friend Request DTO
 *
 * @since 1.2.0
 */
public class FriendRequestDto {

    @NotNull(message = "친구 ID는 필수입니다")
    @Positive(message = "유효한 사용자 ID를 입력하세요")
    private Long friendId;

    public FriendRequestDto() {
    }

    // Getters
    public Long getFriendId() {
        return friendId;
    }

    // Setters
    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendRequestDto that = (FriendRequestDto) o;
        return java.util.Objects.equals(friendId, that.friendId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(friendId);
    }

    @Override
    public String toString() {
        return "FriendRequestDto{" +
               "friendId=" + friendId +
               '}';
    }
}
