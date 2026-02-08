package com.beam.dto;

import jakarta.validation.constraints.Pattern;

public class NotificationPreferencesRequest {

    private Boolean pushEnabled;

    private Boolean messagePreview;

    private Boolean soundEnabled;

    private Boolean vibrationEnabled;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Quiet hours start must be in HH:mm format")
    private String quietHoursStart;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Quiet hours end must be in HH:mm format")
    private String quietHoursEnd;

    public NotificationPreferencesRequest() {
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

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
}
