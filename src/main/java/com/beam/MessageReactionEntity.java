package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_reactions", indexes = {
    @Index(name = "idx_reaction_message", columnList = "message_id, message_type"),
    @Index(name = "idx_reaction_user", columnList = "user_id"),
    @Index(name = "idx_reaction_emoji", columnList = "emoji")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_reaction_user_message", columnNames = {"user_id", "message_id", "message_type", "emoji"})
})
public class MessageReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String emoji;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MessageType {
        ROOM,
        DIRECT,
        GROUP
    }

    public MessageReactionEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmoji() {
        return emoji;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long messageId;
        private MessageType messageType;
        private Long userId;
        private String emoji;
        private LocalDateTime createdAt = LocalDateTime.now();

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

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder emoji(String emoji) {
            this.emoji = emoji;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MessageReactionEntity build() {
            MessageReactionEntity entity = new MessageReactionEntity();
            entity.id = this.id;
            entity.messageId = this.messageId;
            entity.messageType = this.messageType;
            entity.userId = this.userId;
            entity.emoji = this.emoji;
            entity.createdAt = this.createdAt;
            return entity;
        }
    }
}
