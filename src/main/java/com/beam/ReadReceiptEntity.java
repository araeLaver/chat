package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "read_receipts", indexes = {
    @Index(name = "idx_message_user", columnList = "messageId,userId"),
    @Index(name = "idx_group_message_user", columnList = "groupMessageId,userId")
})
public class ReadReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long messageId;

    @Column
    private Long groupMessageId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();

    public ReadReceiptEntity() {
    }

    public ReadReceiptEntity(Long id, Long messageId, Long groupMessageId, Long userId, LocalDateTime readAt) {
        this.id = id;
        this.messageId = messageId;
        this.groupMessageId = groupMessageId;
        this.userId = userId;
        this.readAt = readAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getGroupMessageId() {
        return groupMessageId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public void setGroupMessageId(Long groupMessageId) {
        this.groupMessageId = groupMessageId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
        private Long groupMessageId;
        private Long userId;
        private LocalDateTime readAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder messageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder groupMessageId(Long groupMessageId) {
            this.groupMessageId = groupMessageId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }

        public ReadReceiptEntity build() {
            ReadReceiptEntity entity = new ReadReceiptEntity();
            entity.id = this.id;
            entity.messageId = this.messageId;
            entity.groupMessageId = this.groupMessageId;
            entity.userId = this.userId;
            entity.readAt = this.readAt;
            return entity;
        }
    }
}
