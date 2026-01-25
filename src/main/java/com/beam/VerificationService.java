package com.beam;

import com.beam.exception.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 인증 코드 관리 서비스
 * - 인증 코드 설정/검증/초기화 로직 통합
 * - 중복 코드 제거
 */
@Service
public class VerificationService {

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 5;

    /**
     * 사용자에게 인증 코드 설정
     */
    public void setVerificationCode(UserEntity user, String code) {
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES));
    }

    /**
     * 인증 코드 검증
     * @throws AuthenticationException 코드가 일치하지 않거나 만료된 경우
     */
    public void validateVerificationCode(UserEntity user, String code) {
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw AuthenticationException.verificationFailed();
        }

        if (user.getVerificationCodeExpiresAt() == null ||
            user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw AuthenticationException.verificationExpired();
        }
    }

    /**
     * 인증 코드 검증 (boolean 반환 버전)
     */
    public boolean isValidCode(UserEntity user, String code) {
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            return false;
        }

        if (user.getVerificationCodeExpiresAt() == null ||
            user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * 인증 코드 초기화 (인증 완료 후)
     */
    public void clearVerificationCode(UserEntity user) {
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
    }

    /**
     * 인증 코드 생성 및 설정
     */
    public String generateAndSetCode(UserEntity user) {
        String code = VerificationCodeGenerator.generate();
        setVerificationCode(user, code);
        return code;
    }
}
