package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_phone_number", columnList = "phoneNumber"),
    @Index(name = "idx_username", columnList = "username")
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false, length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String displayName;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "status_message", length = 200)
    private String statusMessage;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_phone_verified", nullable = false)
    private Boolean isPhoneVerified = false;

    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsPhoneVerified() { return isPhoneVerified; }
    public void setIsPhoneVerified(Boolean isPhoneVerified) { this.isPhoneVerified = isPhoneVerified; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public LocalDateTime getVerificationCodeExpiresAt() { return verificationCodeExpiresAt; }
    public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) { this.verificationCodeExpiresAt = verificationCodeExpiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Builder pattern
    public static UserEntityBuilder builder() { return new UserEntityBuilder(); }

    public static class UserEntityBuilder {
        private Long id;
        private String username;
        private String password;
        private String phoneNumber;
        private String email;
        private String displayName;
        private String profileImage;
        private String statusMessage;
        private Boolean isOnline = false;
        private LocalDateTime lastSeen;
        private Boolean isActive = true;
        private Boolean isPhoneVerified = false;
        private String verificationCode;
        private LocalDateTime verificationCodeExpiresAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserEntityBuilder id(Long id) { this.id = id; return this; }
        public UserEntityBuilder username(String username) { this.username = username; return this; }
        public UserEntityBuilder password(String password) { this.password = password; return this; }
        public UserEntityBuilder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public UserEntityBuilder email(String email) { this.email = email; return this; }
        public UserEntityBuilder displayName(String displayName) { this.displayName = displayName; return this; }
        public UserEntityBuilder profileImage(String profileImage) { this.profileImage = profileImage; return this; }
        public UserEntityBuilder statusMessage(String statusMessage) { this.statusMessage = statusMessage; return this; }
        public UserEntityBuilder isOnline(Boolean isOnline) { this.isOnline = isOnline; return this; }
        public UserEntityBuilder lastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; return this; }
        public UserEntityBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public UserEntityBuilder isPhoneVerified(Boolean isPhoneVerified) { this.isPhoneVerified = isPhoneVerified; return this; }
        public UserEntityBuilder verificationCode(String verificationCode) { this.verificationCode = verificationCode; return this; }
        public UserEntityBuilder verificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) { this.verificationCodeExpiresAt = verificationCodeExpiresAt; return this; }
        public UserEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserEntity build() {
            UserEntity entity = new UserEntity();
            entity.id = this.id;
            entity.username = this.username;
            entity.password = this.password;
            entity.phoneNumber = this.phoneNumber;
            entity.email = this.email;
            entity.displayName = this.displayName;
            entity.profileImage = this.profileImage;
            entity.statusMessage = this.statusMessage;
            entity.isOnline = this.isOnline;
            entity.lastSeen = this.lastSeen;
            entity.isActive = this.isActive;
            entity.isPhoneVerified = this.isPhoneVerified;
            entity.verificationCode = this.verificationCode;
            entity.verificationCodeExpiresAt = this.verificationCodeExpiresAt;
            entity.createdAt = this.createdAt;
            entity.updatedAt = this.updatedAt;
            return entity;
        }
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isOnline == null) this.isOnline = false;
        if (this.isActive == null) this.isActive = true;
        if (this.isPhoneVerified == null) this.isPhoneVerified = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}