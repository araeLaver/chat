package com.beam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 이메일 회원가입 완료 요청 DTO
 */
public class EmailRegisterRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "표시 이름은 필수입니다")
    @Size(min = 2, max = 50, message = "표시 이름은 2~50자 사이입니다")
    private String displayName;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 30, message = "사용자명은 3~30자 사이입니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 밑줄만 사용 가능합니다")
    private String username;

    public EmailRegisterRequest() {}

    public EmailRegisterRequest(String email, String displayName, String username) {
        this.email = email;
        this.displayName = displayName;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
