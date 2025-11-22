package com.beam;

public enum RoomType {
    DIRECT("1:1 채팅"),
    GROUP("그룹 채팅");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}