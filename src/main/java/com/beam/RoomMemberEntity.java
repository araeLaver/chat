package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_members", indexes = {
    @Index(name = "idx_room_user", columnList = "roomId,userId"),
    @Index(name = "idx_user_rooms", columnList = "userId")
})
public class RoomMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.MEMBER;

    @Column(nullable = false)
    private Boolean isMuted = false;

    @Column
    private LocalDateTime mutedUntil;

    @Column(nullable = false)
    private Integer unreadCount = 0;

    @Column
    private LocalDateTime lastReadTime;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column
    private LocalDateTime leftAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    public enum MemberRole {
        OWNER,
        ADMIN,
        MEMBER
    }

    public RoomMemberEntity() {
    }

    public RoomMemberEntity(Long id, Long roomId, Long userId, MemberRole role, Boolean isMuted,
                            LocalDateTime mutedUntil, Integer unreadCount, LocalDateTime lastReadTime,
                            LocalDateTime joinedAt, LocalDateTime leftAt, Boolean isActive) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.role = role;
        this.isMuted = isMuted;
        this.mutedUntil = mutedUntil;
        this.unreadCount = unreadCount;
        this.lastReadTime = lastReadTime;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.isActive = isActive;
    }

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = MemberRole.MEMBER;
        }
        if (this.isMuted == null) {
            this.isMuted = false;
        }
        if (this.unreadCount == null) {
            this.unreadCount = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    public void resetUnreadCount() {
        this.unreadCount = 0;
        this.lastReadTime = LocalDateTime.now();
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public Boolean getIsMuted() {
        return isMuted;
    }

    public LocalDateTime getMutedUntil() {
        return mutedUntil;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public LocalDateTime getLastReadTime() {
        return lastReadTime;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public void setIsMuted(Boolean isMuted) {
        this.isMuted = isMuted;
    }

    public void setMutedUntil(LocalDateTime mutedUntil) {
        this.mutedUntil = mutedUntil;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setLastReadTime(LocalDateTime lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long roomId;
        private Long userId;
        private MemberRole role = MemberRole.MEMBER;
        private Boolean isMuted = false;
        private LocalDateTime mutedUntil;
        private Integer unreadCount = 0;
        private LocalDateTime lastReadTime;
        private LocalDateTime joinedAt = LocalDateTime.now();
        private LocalDateTime leftAt;
        private Boolean isActive = true;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder roomId(Long roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder role(MemberRole role) {
            this.role = role;
            return this;
        }

        public Builder isMuted(Boolean isMuted) {
            this.isMuted = isMuted;
            return this;
        }

        public Builder mutedUntil(LocalDateTime mutedUntil) {
            this.mutedUntil = mutedUntil;
            return this;
        }

        public Builder unreadCount(Integer unreadCount) {
            this.unreadCount = unreadCount;
            return this;
        }

        public Builder lastReadTime(LocalDateTime lastReadTime) {
            this.lastReadTime = lastReadTime;
            return this;
        }

        public Builder joinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }

        public Builder leftAt(LocalDateTime leftAt) {
            this.leftAt = leftAt;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public RoomMemberEntity build() {
            RoomMemberEntity entity = new RoomMemberEntity();
            entity.id = this.id;
            entity.roomId = this.roomId;
            entity.userId = this.userId;
            entity.role = this.role;
            entity.isMuted = this.isMuted;
            entity.mutedUntil = this.mutedUntil;
            entity.unreadCount = this.unreadCount;
            entity.lastReadTime = this.lastReadTime;
            entity.joinedAt = this.joinedAt;
            entity.leftAt = this.leftAt;
            entity.isActive = this.isActive;
            return entity;
        }
    }
}
