package com.beam.dto;

import jakarta.validation.constraints.*;

/**
 * Block User Request DTO
 *
 * @since 1.2.0
 */
public class BlockUserRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다")
    @Positive(message = "유효한 사용자 ID를 입력하세요")
    private Long userId;

    public BlockUserRequestDto() {
    }

    // Getters
    public Long getUserId() {
        return userId;
    }

    // Setters
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockUserRequestDto that = (BlockUserRequestDto) o;
        return java.util.Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "BlockUserRequestDto{" +
               "userId=" + userId +
               '}';
    }
}
