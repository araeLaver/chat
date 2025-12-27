package com.beam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 휴대폰 인증 코드 발송 요청 DTO
 */
public class PhoneSendCodeRequest {

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 한국 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;

    public PhoneSendCodeRequest() {}

    public PhoneSendCodeRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
