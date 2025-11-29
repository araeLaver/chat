package com.beam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 인증 요청 DTO
 * - 회원가입/로그인 시 사용
 * - 입력 검증 포함
 */
public class AuthRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 20, message = "사용자명은 3-20자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 언더스코어만 가능합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 100, message = "비밀번호는 최소 6자 이상이어야 합니다")
    private String password;

    @Pattern(
        regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
        message = "올바른 한국 휴대폰 번호 형식이 아닙니다"
    )
    private String phoneNumber;

    @Size(max = 50, message = "표시 이름은 최대 50자까지 가능합니다")
    private String displayName;

    public AuthRequest() {
    }

    public AuthRequest(String username, String password, String phoneNumber, String displayName) {
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthRequest that = (AuthRequest) o;
        return java.util.Objects.equals(username, that.username) &&
               java.util.Objects.equals(password, that.password) &&
               java.util.Objects.equals(phoneNumber, that.phoneNumber) &&
               java.util.Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(username, password, phoneNumber, displayName);
    }

    @Override
    public String toString() {
        return "AuthRequest{" +
               "username='" + username + '\'' +
               ", password='[PROTECTED]'" +
               ", phoneNumber='" + phoneNumber + '\'' +
               ", displayName='" + displayName + '\'' +
               '}';
    }
}
