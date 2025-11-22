package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "direct_messages", indexes = {
    @Index(name = "idx_sender_receiver", columnList = "senderId,receiverId"),
    @Index(name = "idx_conversation", columnList = "conversationId"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class DirectMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String conversationId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column
    private LocalDateTime readAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime deletedAt;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(length = 500)
    private String fileUrl;

    @Column(length = 200)
    private String fileName;

    @Column
    private Long fileSize;

    public DirectMessageEntity() {
    }

    public DirectMessageEntity(Long id, String conversationId, Long senderId, Long receiverId, String content,
                               LocalDateTime timestamp, Boolean isRead, LocalDateTime readAt, Boolean isDeleted,
                               LocalDateTime deletedAt, MessageType messageType, String fileUrl, String fileName, Long fileSize) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.readAt = readAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        VOICE,
        VIDEO
    }

    public static String generateConversationId(Long userId1, Long userId2) {
        long smaller = Math.min(userId1, userId2);
        long larger = Math.max(userId1, userId2);
        return "dm_" + smaller + "_" + larger;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String conversationId;
        private Long senderId;
        private Long receiverId;
        private String content;
        private LocalDateTime timestamp = LocalDateTime.now();
        private Boolean isRead = false;
        private LocalDateTime readAt;
        private Boolean isDeleted = false;
        private LocalDateTime deletedAt;
        private MessageType messageType;
        private String fileUrl;
        private String fileName;
        private Long fileSize;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder senderId(Long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder receiverId(Long receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder isRead(Boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
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

        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public DirectMessageEntity build() {
            DirectMessageEntity entity = new DirectMessageEntity();
            entity.id = this.id;
            entity.conversationId = this.conversationId;
            entity.senderId = this.senderId;
            entity.receiverId = this.receiverId;
            entity.content = this.content;
            entity.timestamp = this.timestamp;
            entity.isRead = this.isRead;
            entity.readAt = this.readAt;
            entity.isDeleted = this.isDeleted;
            entity.deletedAt = this.deletedAt;
            entity.messageType = this.messageType;
            entity.fileUrl = this.fileUrl;
            entity.fileName = this.fileName;
            entity.fileSize = this.fileSize;
            return entity;
        }
    }
}
