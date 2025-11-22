package com.beam;

public class User {
    private String id;
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
}