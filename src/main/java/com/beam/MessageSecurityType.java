package com.beam;

public enum MessageSecurityType {
    NORMAL("일반"),
    SECRET("비밀"),
    VOLATILE("휘발성");

    private final String displayName;

    MessageSecurityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresEncryption() {
        return this == SECRET;
    }
}