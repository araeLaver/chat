package com.chat;

public enum FriendStatus {
    PENDING("요청 중"),
    ACCEPTED("친구"),
    BLOCKED("차단");

    private final String displayName;

    FriendStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
