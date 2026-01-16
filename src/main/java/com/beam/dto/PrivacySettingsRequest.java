package com.beam.dto;

/**
 * 프라이버시 설정 요청 DTO
 */
public class PrivacySettingsRequest {

    private Boolean ghostMode;
    private Boolean disableIpLogging;
    private Boolean autoTranslate;
    private String preferredLanguage;

    public PrivacySettingsRequest() {}

    public Boolean getGhostMode() {
        return ghostMode;
    }

    public void setGhostMode(Boolean ghostMode) {
        this.ghostMode = ghostMode;
    }

    public Boolean getDisableIpLogging() {
        return disableIpLogging;
    }

    public void setDisableIpLogging(Boolean disableIpLogging) {
        this.disableIpLogging = disableIpLogging;
    }

    public Boolean getAutoTranslate() {
        return autoTranslate;
    }

    public void setAutoTranslate(Boolean autoTranslate) {
        this.autoTranslate = autoTranslate;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}
