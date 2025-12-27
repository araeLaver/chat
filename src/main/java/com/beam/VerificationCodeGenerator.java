package com.beam;

import java.security.SecureRandom;

/**
 * 보안 인증 코드 생성 유틸리티
 *
 * <p>SecureRandom을 사용하여 암호학적으로 안전한 인증 코드를 생성합니다.
 * java.util.Random은 예측 가능하므로 보안 관련 코드에 사용하면 안 됩니다.
 *
 * @since 1.1.0
 */
public final class VerificationCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final int MAX_CODE_VALUE = 999999;

    private VerificationCodeGenerator() {
        // 유틸리티 클래스는 인스턴스화 방지
    }

    /**
     * 6자리 숫자 인증 코드 생성
     *
     * @return 6자리 인증 코드 (예: "012345", "987654")
     */
    public static String generate() {
        return generate(DEFAULT_CODE_LENGTH);
    }

    /**
     * 지정된 길이의 숫자 인증 코드 생성
     *
     * @param length 코드 길이 (1-9)
     * @return 지정된 길이의 인증 코드
     */
    public static String generate(int length) {
        if (length < 1 || length > 9) {
            throw new IllegalArgumentException("Code length must be between 1 and 9");
        }

        int maxValue = (int) Math.pow(10, length) - 1;
        int code = SECURE_RANDOM.nextInt(maxValue + 1);
        return String.format("%0" + length + "d", code);
    }

    /**
     * 영숫자 혼합 인증 코드 생성 (더 높은 보안성)
     *
     * @param length 코드 길이
     * @return 영숫자 혼합 인증 코드
     */
    public static String generateAlphanumeric(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Code length must be at least 1");
        }

        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동 방지: 0, O, 1, I 제외
        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }

        return code.toString();
    }
}
