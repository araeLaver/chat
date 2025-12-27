package com.beam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 휴대폰 회원가입 완료 요청 DTO
 */
public class PhoneRegisterRequest {

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 한국 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;

    @NotBlank(message = "표시 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "표시 이름은 2~50자 사이입니다")
    private String displayName;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 30, message = "사용자명은 3~30자 사이입니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 밑줄만 사용 가능합니다")
    private String username;

    public PhoneRegisterRequest() {}

    public PhoneRegisterRequest(String phoneNumber, String displayName, String username) {
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
