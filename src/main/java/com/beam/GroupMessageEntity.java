package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages", indexes = {
    @Index(name = "idx_room_timestamp", columnList = "roomId,timestamp"),
    @Index(name = "idx_sender", columnList = "senderId")
})
public class GroupMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(length = 1000)
    private String fileUrl;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false)
    private Integer readCount = 0;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime deletedAt;

    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        VOICE,
        VIDEO,
        SYSTEM
    }

    public GroupMessageEntity() {
    }

    public GroupMessageEntity(Long id, Long roomId, Long senderId, String content, MessageType messageType,
                              String fileUrl, LocalDateTime timestamp, Integer readCount, Boolean isDeleted,
                              LocalDateTime deletedAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.timestamp = timestamp;
        this.readCount = readCount;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        if (this.messageType == null) {
            this.messageType = MessageType.TEXT;
        }
        if (this.readCount == null) {
            this.readCount = 0;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    public void incrementReadCount() {
        this.readCount++;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long roomId;
        private Long senderId;
        private String content;
        private MessageType messageType = MessageType.TEXT;
        private String fileUrl;
        private LocalDateTime timestamp = LocalDateTime.now();
        private Integer readCount = 0;
        private Boolean isDeleted = false;
        private LocalDateTime deletedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder roomId(Long roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder senderId(Long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder readCount(Integer readCount) {
            this.readCount = readCount;
            return this;
        }

        public Builder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Builder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public GroupMessageEntity build() {
            GroupMessageEntity entity = new GroupMessageEntity();
            entity.id = this.id;
            entity.roomId = this.roomId;
            entity.senderId = this.senderId;
            entity.content = this.content;
            entity.messageType = this.messageType;
            entity.fileUrl = this.fileUrl;
            entity.timestamp = this.timestamp;
            entity.readCount = this.readCount;
            entity.isDeleted = this.isDeleted;
            entity.deletedAt = this.deletedAt;
            return entity;
        }
    }
}
