package com.beam.dto;

import jakarta.validation.constraints.*;

/**
 * Create Room Request DTO
 *
 * @since 1.2.0
 */
public class CreateRoomRequest {

    @NotBlank(message = "방 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "방 이름은 2-50자 사이여야 합니다")
    private String roomName;

    @Size(max = 200, message = "설명은 200자를 초과할 수 없습니다")
    private String description;

    @Pattern(regexp = "PUBLIC|PRIVATE", message = "방 타입은 PUBLIC 또는 PRIVATE만 가능합니다")
    private String roomType = "PUBLIC";

    @Min(value = 2, message = "최소 인원은 2명입니다")
    @Max(value = 1000, message = "최대 인원은 1000명입니다")
    private Integer maxMembers = 100;

    public CreateRoomRequest() {
    }

    // Getters
    public String getRoomName() {
        return roomName;
    }

    public String getDescription() {
        return description;
    }

    public String getRoomType() {
        return roomType;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    // Setters
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateRoomRequest that = (CreateRoomRequest) o;
        return java.util.Objects.equals(roomName, that.roomName) &&
               java.util.Objects.equals(description, that.description) &&
               java.util.Objects.equals(roomType, that.roomType) &&
               java.util.Objects.equals(maxMembers, that.maxMembers);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(roomName, description, roomType, maxMembers);
    }

    @Override
    public String toString() {
        return "CreateRoomRequest{" +
               "roomName='" + roomName + '\'' +
               ", description='" + description + '\'' +
               ", roomType='" + roomType + '\'' +
               ", maxMembers=" + maxMembers +
               '}';
    }
}
