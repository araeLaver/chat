package com.beam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 익명 회원가입 요청 DTO
 * 이메일/전화번호 없이 username과 password만으로 가입
 */
public class AnonymousRegisterRequest {

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 20, message = "사용자명은 3~20자여야 합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
    private String password;

    @Size(max = 50, message = "표시 이름은 50자를 초과할 수 없습니다")
    private String displayName;

    @Size(max = 10, message = "선호 언어 코드는 10자를 초과할 수 없습니다")
    private String preferredLanguage = "ko";

    public AnonymousRegisterRequest() {}

    public AnonymousRegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}
