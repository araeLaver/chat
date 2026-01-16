package com.beam.dto;

/**
 * 자동 삭제 메시지 TTL 옵션
 */
public enum TTLOption {
    MINUTE_1(60L, "1분"),
    MINUTE_5(300L, "5분"),
    HOUR_1(3600L, "1시간"),
    HOUR_24(86400L, "24시간"),
    WEEK_1(604800L, "7일");

    private final long seconds;
    private final String displayName;

    TTLOption(long seconds, String displayName) {
        this.seconds = seconds;
        this.displayName = displayName;
    }

    public long getSeconds() {
        return seconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 초 단위 값으로 TTLOption 찾기
     */
    public static TTLOption fromSeconds(long seconds) {
        for (TTLOption option : values()) {
            if (option.seconds == seconds) {
                return option;
            }
        }
        return null;
    }

    /**
     * 유효한 TTL 값인지 확인
     */
    public static boolean isValidTTL(Long seconds) {
        if (seconds == null) {
            return true; // null은 영구 메시지
        }
        return fromSeconds(seconds) != null;
    }
}
