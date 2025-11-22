package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_room_type", columnList = "roomType"),
    @Index(name = "idx_created_by", columnList = "createdBy")
})
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String roomName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType roomType = RoomType.PUBLIC;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Integer maxMembers = 100;

    @Column(nullable = false)
    private Integer currentMembers = 0;

    @Column(length = 500)
    private String roomImageUrl;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    @Column
    private LocalDateTime lastMessageTime;

    @Column
    private Long lastMessageSenderId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    public enum RoomType {
        PUBLIC,
        PRIVATE,
        SECRET
    }

    public RoomEntity() {
    }

    public RoomEntity(Long id, String roomName, String description, RoomType roomType, Long createdBy,
                      Integer maxMembers, Integer currentMembers, String roomImageUrl, Boolean isActive,
                      String lastMessage, LocalDateTime lastMessageTime, Long lastMessageSenderId,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.roomName = roomName;
        this.description = description;
        this.roomType = roomType;
        this.createdBy = createdBy;
        this.maxMembers = maxMembers;
        this.currentMembers = currentMembers;
        this.roomImageUrl = roomImageUrl;
        this.isActive = isActive;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageSenderId = lastMessageSenderId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.roomType == null) {
            this.roomType = RoomType.PUBLIC;
        }
        if (this.maxMembers == null) {
            this.maxMembers = 100;
        }
        if (this.currentMembers == null) {
            this.currentMembers = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementMemberCount() {
        this.currentMembers++;
    }

    public void decrementMemberCount() {
        if (this.currentMembers > 0) {
            this.currentMembers--;
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getDescription() {
        return description;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Integer getMaxMembers() {
        return maxMembers;
    }

    public Integer getCurrentMembers() {
        return currentMembers;
    }

    public String getRoomImageUrl() {
        return roomImageUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public Long getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }

    public void setCurrentMembers(Integer currentMembers) {
        this.currentMembers = currentMembers;
    }

    public void setRoomImageUrl(String roomImageUrl) {
        this.roomImageUrl = roomImageUrl;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public void setLastMessageSenderId(Long lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String roomName;
        private String description;
        private RoomType roomType = RoomType.PUBLIC;
        private Long createdBy;
        private Integer maxMembers = 100;
        private Integer currentMembers = 0;
        private String roomImageUrl;
        private Boolean isActive = true;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private Long lastMessageSenderId;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder roomName(String roomName) {
            this.roomName = roomName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder roomType(RoomType roomType) {
            this.roomType = roomType;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder maxMembers(Integer maxMembers) {
            this.maxMembers = maxMembers;
            return this;
        }

        public Builder currentMembers(Integer currentMembers) {
            this.currentMembers = currentMembers;
            return this;
        }

        public Builder roomImageUrl(String roomImageUrl) {
            this.roomImageUrl = roomImageUrl;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder lastMessage(String lastMessage) {
            this.lastMessage = lastMessage;
            return this;
        }

        public Builder lastMessageTime(LocalDateTime lastMessageTime) {
            this.lastMessageTime = lastMessageTime;
            return this;
        }

        public Builder lastMessageSenderId(Long lastMessageSenderId) {
            this.lastMessageSenderId = lastMessageSenderId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RoomEntity build() {
            RoomEntity entity = new RoomEntity();
            entity.id = this.id;
            entity.roomName = this.roomName;
            entity.description = this.description;
            entity.roomType = this.roomType;
            entity.createdBy = this.createdBy;
            entity.maxMembers = this.maxMembers;
            entity.currentMembers = this.currentMembers;
            entity.roomImageUrl = this.roomImageUrl;
            entity.isActive = this.isActive;
            entity.lastMessage = this.lastMessage;
            entity.lastMessageTime = this.lastMessageTime;
            entity.lastMessageSenderId = this.lastMessageSenderId;
            entity.createdAt = this.createdAt;
            entity.updatedAt = this.updatedAt;
            return entity;
        }
    }
}
