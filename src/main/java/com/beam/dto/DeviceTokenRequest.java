package com.beam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    @Size(max = 500, message = "Device token must be at most 500 characters")
    private String deviceToken;

    @NotBlank(message = "Device type is required")
    @Pattern(regexp = "^(IOS|ANDROID|WEB)$", message = "Device type must be IOS, ANDROID, or WEB")
    private String deviceType;

    @Size(max = 100, message = "Device name must be at most 100 characters")
    private String deviceName;

    public DeviceTokenRequest() {
    }

    public DeviceTokenRequest(String deviceToken, String deviceType, String deviceName) {
        this.deviceToken = deviceToken;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
