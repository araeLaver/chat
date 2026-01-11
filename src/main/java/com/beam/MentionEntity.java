package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mentions", indexes = {
    @Index(name = "idx_mention_message", columnList = "message_id, message_type"),
    @Index(name = "idx_mention_user", columnList = "mentioned_user_id"),
    @Index(name = "idx_mention_unread", columnList = "mentioned_user_id, is_read")
})
public class MentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "mentioned_user_id", nullable = false)
    private Long mentionedUserId;

    @Column(name = "mentioner_user_id", nullable = false)
    private Long mentionerUserId;

    @Column(name = "room_id", length = 50)
    private String roomId;

    @Column(name = "conversation_id", length = 100)
    private String conversationId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum MessageType {
        ROOM,
        DIRECT,
        GROUP
    }

    public MentionEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Long getMentionedUserId() {
        return mentionedUserId;
    }

    public void setMentionedUserId(Long mentionedUserId) {
        this.mentionedUserId = mentionedUserId;
    }

    public Long getMentionerUserId() {
        return mentionerUserId;
    }

    public void setMentionerUserId(Long mentionerUserId) {
        this.mentionerUserId = mentionerUserId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long messageId;
        private MessageType messageType;
        private Long mentionedUserId;
        private Long mentionerUserId;
        private String roomId;
        private String conversationId;
        private Boolean isRead = false;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime readAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder messageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder mentionedUserId(Long mentionedUserId) {
            this.mentionedUserId = mentionedUserId;
            return this;
        }

        public Builder mentionerUserId(Long mentionerUserId) {
            this.mentionerUserId = mentionerUserId;
            return this;
        }

        public Builder roomId(String roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder isRead(Boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }

        public MentionEntity build() {
            MentionEntity entity = new MentionEntity();
            entity.id = this.id;
            entity.messageId = this.messageId;
            entity.messageType = this.messageType;
            entity.mentionedUserId = this.mentionedUserId;
            entity.mentionerUserId = this.mentionerUserId;
            entity.roomId = this.roomId;
            entity.conversationId = this.conversationId;
            entity.isRead = this.isRead;
            entity.createdAt = this.createdAt;
            entity.readAt = this.readAt;
            return entity;
        }
    }
}
