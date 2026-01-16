package com.beam;

public class User {
    private String id;
    private Long userId;  // DB의 UserEntity ID (번역 기능용)
    private String username;
    private String sessionId;
    private long joinTime;

    public User() {}

    public User(String id, String username, String sessionId) {
        this.id = id;
        this.username = username;
        this.sessionId = sessionId;
        this.joinTime = System.currentTimeMillis();
    }

    public User(String id, String username, String sessionId, Long userId) {
        this.id = id;
        this.username = username;
        this.sessionId = sessionId;
        this.userId = userId;
        this.joinTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}