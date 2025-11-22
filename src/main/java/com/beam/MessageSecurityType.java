package com.beam;

public enum MessageSecurityType {
    NORMAL("일반");

    private final String displayName;

    MessageSecurityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}