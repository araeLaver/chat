package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_tokens", indexes = {
    @Index(name = "idx_device_tokens_user", columnList = "user_id"),
    @Index(name = "idx_device_tokens_active", columnList = "user_id, is_active")
})
public class DeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_token", nullable = false, unique = true, length = 500)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DeviceType {
        IOS,
        ANDROID,
        WEB
    }

    public DeviceTokenEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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
        private String deviceToken;
        private DeviceType deviceType;
        private String deviceName;
        private Boolean isActive = true;
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

        public Builder deviceToken(String deviceToken) {
            this.deviceToken = deviceToken;
            return this;
        }

        public Builder deviceType(DeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
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

        public DeviceTokenEntity build() {
            DeviceTokenEntity entity = new DeviceTokenEntity();
            entity.id = this.id;
            entity.userId = this.userId;
            entity.deviceToken = this.deviceToken;
            entity.deviceType = this.deviceType;
            entity.deviceName = this.deviceName;
            entity.isActive = this.isActive;
            entity.createdAt = this.createdAt;
            entity.updatedAt = this.updatedAt;
            return entity;
        }
    }
}
