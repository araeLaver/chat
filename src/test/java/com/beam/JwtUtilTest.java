package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 단위 테스트
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-must-be-at-least-32-bytes";
    private static final Long TEST_EXPIRATION = 3600000L; // 1시간

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    class GenerateTokenTest {

        @Test
        @DisplayName("정상 토큰 생성")
        void generateToken_success() {
            String token = jwtUtil.generateToken("testuser", 1L);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(token.split("\\.").length == 3); // JWT 형식 검증
        }

        @Test
        @DisplayName("다른 사용자는 다른 토큰 생성")
        void differentUsers_differentTokens() {
            String token1 = jwtUtil.generateToken("user1", 1L);
            String token2 = jwtUtil.generateToken("user2", 2L);

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("토큰 검증 테스트")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰 - 성공")
        void validToken_returnsTrue() {
            String token = jwtUtil.generateToken("testuser", 1L);

            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("잘못된 토큰 형식 - 실패")
        void invalidTokenFormat_returnsFalse() {
            assertFalse(jwtUtil.validateToken("invalid.token.format"));
        }

        @Test
        @DisplayName("null 토큰 - 실패")
        void nullToken_returnsFalse() {
            assertFalse(jwtUtil.validateToken(null));
        }

        @Test
        @DisplayName("빈 토큰 - 실패")
        void emptyToken_returnsFalse() {
            assertFalse(jwtUtil.validateToken(""));
        }

        @Test
        @DisplayName("변조된 토큰 - 실패")
        void tamperedToken_returnsFalse() {
            String token = jwtUtil.generateToken("testuser", 1L);
            String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

            assertFalse(jwtUtil.validateToken(tamperedToken));
        }

        @Test
        @DisplayName("다른 secret으로 생성된 토큰 - 실패")
        void tokenFromDifferentSecret_returnsFalse() {
            // 다른 secret 키를 사용하는 JwtUtil
            JwtUtil otherJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(otherJwtUtil, "secret",
                "other-secret-key-for-jwt-token-generation-must-be-at-least-32-bytes");
            ReflectionTestUtils.setField(otherJwtUtil, "expiration", TEST_EXPIRATION);

            String tokenFromOther = otherJwtUtil.generateToken("testuser", 1L);

            assertFalse(jwtUtil.validateToken(tokenFromOther));
        }
    }

    @Nested
    @DisplayName("토큰에서 정보 추출 테스트")
    class ExtractFromTokenTest {

        @Test
        @DisplayName("사용자명 추출 - 성공")
        void getUsernameFromToken_success() {
            String username = "testuser";
            String token = jwtUtil.generateToken(username, 1L);

            String extractedUsername = jwtUtil.getUsernameFromToken(token);

            assertEquals(username, extractedUsername);
        }

        @Test
        @DisplayName("사용자 ID 추출 - 성공")
        void getUserIdFromToken_success() {
            Long userId = 12345L;
            String token = jwtUtil.generateToken("testuser", userId);

            Long extractedUserId = jwtUtil.getUserIdFromToken(token);

            assertEquals(userId, extractedUserId);
        }

        @Test
        @DisplayName("특수문자 포함 사용자명 추출")
        void getUsernameWithSpecialChars_success() {
            String username = "user@example.com";
            String token = jwtUtil.generateToken(username, 1L);

            String extractedUsername = jwtUtil.getUsernameFromToken(token);

            assertEquals(username, extractedUsername);
        }

        @Test
        @DisplayName("한글 사용자명 추출")
        void getKoreanUsername_success() {
            String username = "홍길동";
            String token = jwtUtil.generateToken(username, 1L);

            String extractedUsername = jwtUtil.getUsernameFromToken(token);

            assertEquals(username, extractedUsername);
        }
    }

    @Nested
    @DisplayName("토큰 만료 테스트")
    class TokenExpirationTest {

        @Test
        @DisplayName("만료된 토큰 - 검증 실패")
        void expiredToken_returnsFalse() throws InterruptedException {
            // 매우 짧은 만료 시간 설정
            JwtUtil shortExpiryJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortExpiryJwtUtil, "secret", TEST_SECRET);
            ReflectionTestUtils.setField(shortExpiryJwtUtil, "expiration", 1L); // 1ms

            String token = shortExpiryJwtUtil.generateToken("testuser", 1L);

            // 토큰 만료 대기
            Thread.sleep(100);

            assertFalse(shortExpiryJwtUtil.validateToken(token));
        }
    }
}
