package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "friends", indexes = {
    @Index(name = "idx_user_friend", columnList = "userId,friendId"),
    @Index(name = "idx_status", columnList = "status")
})
public class FriendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long friendId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendStatus status = FriendStatus.PENDING;

    @Column
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column
    private LocalDateTime blockedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    public enum FriendStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        BLOCKED
    }

    public FriendEntity() {
    }

    public FriendEntity(Long id, Long userId, Long friendId, FriendStatus status, LocalDateTime requestedAt,
                        LocalDateTime acceptedAt, LocalDateTime blockedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.blockedAt = blockedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = FriendStatus.PENDING;
        }
        if (this.requestedAt == null && this.status == FriendStatus.PENDING) {
            this.requestedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == FriendStatus.ACCEPTED && this.acceptedAt == null) {
            this.acceptedAt = LocalDateTime.now();
        }
        if (this.status == FriendStatus.BLOCKED && this.blockedAt == null) {
            this.blockedAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getFriendId() {
        return friendId;
    }

    public FriendStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public void setStatus(FriendStatus status) {
        this.status = status;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
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
        private Long userId;
        private Long friendId;
        private FriendStatus status = FriendStatus.PENDING;
        private LocalDateTime requestedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime blockedAt;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder friendId(Long friendId) {
            this.friendId = friendId;
            return this;
        }

        public Builder status(FriendStatus status) {
            this.status = status;
            return this;
        }

        public Builder requestedAt(LocalDateTime requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }

        public Builder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public Builder blockedAt(LocalDateTime blockedAt) {
            this.blockedAt = blockedAt;
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

        public FriendEntity build() {
            FriendEntity entity = new FriendEntity();
            entity.id = this.id;
            entity.userId = this.userId;
            entity.friendId = this.friendId;
            entity.status = this.status;
            entity.requestedAt = this.requestedAt;
            entity.acceptedAt = this.acceptedAt;
            entity.blockedAt = this.blockedAt;
            entity.createdAt = this.createdAt;
            entity.updatedAt = this.updatedAt;
            return entity;
        }
    }
}
