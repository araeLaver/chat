package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_preferences_user", columnList = "user_id")
})
public class NotificationPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "message_preview", nullable = false)
    private Boolean messagePreview = true;

    @Column(name = "sound_enabled", nullable = false)
    private Boolean soundEnabled = true;

    @Column(name = "vibration_enabled", nullable = false)
    private Boolean vibrationEnabled = true;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NotificationPreferencesEntity() {
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.pushEnabled == null) {
            this.pushEnabled = true;
        }
        if (this.messagePreview == null) {
            this.messagePreview = true;
        }
        if (this.soundEnabled == null) {
            this.soundEnabled = true;
        }
        if (this.vibrationEnabled == null) {
            this.vibrationEnabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Check if current time is within quiet hours
    public boolean isQuietHoursActive() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !now.isBefore(quietHoursStart) && now.isBefore(quietHoursEnd);
        } else {
            // Quiet hours span midnight
            return !now.isBefore(quietHoursStart) || now.isBefore(quietHoursEnd);
        }
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

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getMessagePreview() {
        return messagePreview;
    }

    public void setMessagePreview(Boolean messagePreview) {
        this.messagePreview = messagePreview;
    }

    public Boolean getSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(Boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public Boolean getVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(Boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
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
        private Boolean pushEnabled = true;
        private Boolean messagePreview = true;
        private Boolean soundEnabled = true;
        private Boolean vibrationEnabled = true;
        private LocalTime quietHoursStart;
        private LocalTime quietHoursEnd;
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

        public Builder pushEnabled(Boolean pushEnabled) {
            this.pushEnabled = pushEnabled;
            return this;
        }

        public Builder messagePreview(Boolean messagePreview) {
            this.messagePreview = messagePreview;
            return this;
        }

        public Builder soundEnabled(Boolean soundEnabled) {
            this.soundEnabled = soundEnabled;
            return this;
        }

        public Builder vibrationEnabled(Boolean vibrationEnabled) {
            this.vibrationEnabled = vibrationEnabled;
            return this;
        }

        public Builder quietHoursStart(LocalTime quietHoursStart) {
            this.quietHoursStart = quietHoursStart;
            return this;
        }

        public Builder quietHoursEnd(LocalTime quietHoursEnd) {
            this.quietHoursEnd = quietHoursEnd;
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

        public NotificationPreferencesEntity build() {
            NotificationPreferencesEntity entity = new NotificationPreferencesEntity();
            entity.id = this.id;
            entity.userId = this.userId;
            entity.pushEnabled = this.pushEnabled;
            entity.messagePreview = this.messagePreview;
            entity.soundEnabled = this.soundEnabled;
            entity.vibrationEnabled = this.vibrationEnabled;
            entity.quietHoursStart = this.quietHoursStart;
            entity.quietHoursEnd = this.quietHoursEnd;
            entity.createdAt = this.createdAt;
            entity.updatedAt = this.updatedAt;
            return entity;
        }
    }
}
