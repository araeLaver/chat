package com.beam;

public class AuthResponse {
    private String token;
    private String username;
    private Long userId;
    private String displayName;
    private String phoneNumber;
    private String email;
    private Boolean emailVerified;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, Long userId, String displayName, String phoneNumber, String message) {
        this.token = token;
        this.username = username;
        this.userId = userId;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    // Getters
    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    // Setters
    public void setToken(String token) {
        this.token = token;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthResponse that = (AuthResponse) o;
        return java.util.Objects.equals(token, that.token) &&
               java.util.Objects.equals(username, that.username) &&
               java.util.Objects.equals(userId, that.userId) &&
               java.util.Objects.equals(displayName, that.displayName) &&
               java.util.Objects.equals(phoneNumber, that.phoneNumber) &&
               java.util.Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(token, username, userId, displayName, phoneNumber, message);
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
               "token='" + token + '\'' +
               ", username='" + username + '\'' +
               ", userId=" + userId +
               ", displayName='" + displayName + '\'' +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", message='" + message + '\'' +
               '}';
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String username;
        private Long userId;
        private String displayName;
        private String phoneNumber;
        private String email;
        private Boolean emailVerified;
        private String message;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder emailVerified(Boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public AuthResponse build() {
            AuthResponse response = new AuthResponse();
            response.token = this.token;
            response.username = this.username;
            response.userId = this.userId;
            response.displayName = this.displayName;
            response.phoneNumber = this.phoneNumber;
            response.email = this.email;
            response.emailVerified = this.emailVerified;
            response.message = this.message;
            return response;
        }
    }
}
