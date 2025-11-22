package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_user1_user2", columnList = "user1Id,user2Id")
})
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String conversationId;

    @Column(nullable = false)
    private Long user1Id;

    @Column(nullable = false)
    private Long user2Id;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    @Column
    private LocalDateTime lastMessageTime;

    @Column
    private Long lastMessageSenderId;

    @Column(nullable = false)
    private Integer unreadCountUser1 = 0;

    @Column(nullable = false)
    private Integer unreadCountUser2 = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    public ConversationEntity() {
    }

    public ConversationEntity(Long id, String conversationId, Long user1Id, Long user2Id, String lastMessage,
                              LocalDateTime lastMessageTime, Long lastMessageSenderId, Integer unreadCountUser1,
                              Integer unreadCountUser2, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageSenderId = lastMessageSenderId;
        this.unreadCountUser1 = unreadCountUser1;
        this.unreadCountUser2 = unreadCountUser2;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.unreadCountUser1 == null) {
            this.unreadCountUser1 = 0;
        }
        if (this.unreadCountUser2 == null) {
            this.unreadCountUser2 = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementUnreadCount(Long userId) {
        if (userId.equals(user1Id)) {
            this.unreadCountUser1++;
        } else if (userId.equals(user2Id)) {
            this.unreadCountUser2++;
        }
    }

    public void resetUnreadCount(Long userId) {
        if (userId.equals(user1Id)) {
            this.unreadCountUser1 = 0;
        } else if (userId.equals(user2Id)) {
            this.unreadCountUser2 = 0;
        }
    }

    public Integer getUnreadCount(Long userId) {
        if (userId.equals(user1Id)) {
            return unreadCountUser1;
        } else if (userId.equals(user2Id)) {
            return unreadCountUser2;
        }
        return 0;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Long getUser1Id() {
        return user1Id;
    }

    public Long getUser2Id() {
        return user2Id;
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

    public Integer getUnreadCountUser1() {
        return unreadCountUser1;
    }

    public Integer getUnreadCountUser2() {
        return unreadCountUser2;
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

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }

    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
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

    public void setUnreadCountUser1(Integer unreadCountUser1) {
        this.unreadCountUser1 = unreadCountUser1;
    }

    public void setUnreadCountUser2(Integer unreadCountUser2) {
        this.unreadCountUser2 = unreadCountUser2;
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
        private String conversationId;
        private Long user1Id;
        private Long user2Id;
        private String lastMessage;
        private LocalDateTime lastMessageTime;
        private Long lastMessageSenderId;
        private Integer unreadCountUser1 = 0;
        private Integer unreadCountUser2 = 0;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder user1Id(Long user1Id) {
            this.user1Id = user1Id;
            return this;
        }

        public Builder user2Id(Long user2Id) {
            this.user2Id = user2Id;
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

        public Builder unreadCountUser1(Integer unreadCountUser1) {
            this.unreadCountUser1 = unreadCountUser1;
            return this;
        }

        public Builder unreadCountUser2(Integer unreadCountUser2) {
            this.unreadCountUser2 = unreadCountUser2;
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

        public ConversationEntity build() {
            ConversationEntity entity = new ConversationEntity();
            entity.id = this.id;
            entity.conversationId = this.conversationId;
            entity.user1Id = this.user1Id;
            entity.user2Id = this.user2Id;
            entity.lastMessage = this.lastMessage;
            entity.lastMessageTime = this.lastMessageTime;
            entity.lastMessageSenderId = this.lastMessageSenderId;
            entity.unreadCountUser1 = this.unreadCountUser1;
            entity.unreadCountUser2 = this.unreadCountUser2;
            entity.createdAt = this.createdAt;
            entity.updatedAt = this.updatedAt;
            return entity;
        }
    }
}
