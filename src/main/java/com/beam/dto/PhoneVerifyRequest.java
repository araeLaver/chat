package com.beam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 휴대폰 인증 확인 요청 DTO
 */
public class PhoneVerifyRequest {

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 한국 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;

    @NotBlank(message = "인증번호는 필수입니다")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
    @Pattern(regexp = "^[0-9]+$", message = "인증번호는 숫자만 입력 가능합니다")
    private String verificationCode;

    public PhoneVerifyRequest() {}

    public PhoneVerifyRequest(String phoneNumber, String verificationCode) {
        this.phoneNumber = phoneNumber;
        this.verificationCode = verificationCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
