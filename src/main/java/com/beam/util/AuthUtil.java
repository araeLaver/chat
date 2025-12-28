package com.beam.util;

import com.beam.JwtUtil;

/**
 * 인증 관련 유틸리티
 */
public final class AuthUtil {

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthUtil() {
        // 유틸리티 클래스
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     */
    public static String extractToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        if (authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return authorizationHeader;
    }

    /**
     * Authorization 헤더에서 사용자 ID 추출
     */
    public static Long extractUserId(String authorizationHeader, JwtUtil jwtUtil) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * Authorization 헤더에서 사용자명 추출
     */
    public static String extractUsername(String authorizationHeader, JwtUtil jwtUtil) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            return null;
        }
        return jwtUtil.getUsernameFromToken(token);
    }
}
