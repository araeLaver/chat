package com.beam.util;

import com.beam.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthUtil 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthUtilTest {

    @Mock
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("토큰 추출 테스트")
    class ExtractTokenTest {

        @Test
        @DisplayName("Bearer 접두사 있는 경우 - 토큰만 추출")
        void extractToken_withBearerPrefix() {
            String header = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

            String token = AuthUtil.extractToken(header);

            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", token);
        }

        @Test
        @DisplayName("Bearer 접두사 없는 경우 - 원본 반환")
        void extractToken_withoutBearerPrefix() {
            String header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

            String token = AuthUtil.extractToken(header);

            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", token);
        }

        @Test
        @DisplayName("null 입력 - null 반환")
        void extractToken_nullInput() {
            String token = AuthUtil.extractToken(null);

            assertNull(token);
        }

        @Test
        @DisplayName("빈 문자열 - 빈 문자열 반환")
        void extractToken_emptyString() {
            String token = AuthUtil.extractToken("");

            assertEquals("", token);
        }

        @Test
        @DisplayName("Bearer만 있는 경우 - 빈 문자열 반환")
        void extractToken_onlyBearer() {
            String token = AuthUtil.extractToken("Bearer ");

            assertEquals("", token);
        }

        @Test
        @DisplayName("대소문자 구분 - bearer (소문자) 처리 안됨")
        void extractToken_lowercaseBearer() {
            String header = "bearer token123";

            String token = AuthUtil.extractToken(header);

            // 소문자 "bearer"는 처리되지 않음
            assertEquals("bearer token123", token);
        }
    }

    @Nested
    @DisplayName("사용자 ID 추출 테스트")
    class ExtractUserIdTest {

        @Test
        @DisplayName("정상 헤더에서 사용자 ID 추출")
        void extractUserId_success() {
            String header = "Bearer valid.jwt.token";
            when(jwtUtil.getUserIdFromToken("valid.jwt.token")).thenReturn(123L);

            Long userId = AuthUtil.extractUserId(header, jwtUtil);

            assertEquals(123L, userId);
            verify(jwtUtil).getUserIdFromToken("valid.jwt.token");
        }

        @Test
        @DisplayName("null 헤더 - null 반환")
        void extractUserId_nullHeader() {
            Long userId = AuthUtil.extractUserId(null, jwtUtil);

            assertNull(userId);
            verify(jwtUtil, never()).getUserIdFromToken(anyString());
        }

        @Test
        @DisplayName("Bearer 없는 헤더에서도 추출 가능")
        void extractUserId_withoutBearer() {
            String header = "direct.jwt.token";
            when(jwtUtil.getUserIdFromToken("direct.jwt.token")).thenReturn(456L);

            Long userId = AuthUtil.extractUserId(header, jwtUtil);

            assertEquals(456L, userId);
        }
    }

    @Nested
    @DisplayName("사용자명 추출 테스트")
    class ExtractUsernameTest {

        @Test
        @DisplayName("정상 헤더에서 사용자명 추출")
        void extractUsername_success() {
            String header = "Bearer valid.jwt.token";
            when(jwtUtil.getUsernameFromToken("valid.jwt.token")).thenReturn("testuser");

            String username = AuthUtil.extractUsername(header, jwtUtil);

            assertEquals("testuser", username);
            verify(jwtUtil).getUsernameFromToken("valid.jwt.token");
        }

        @Test
        @DisplayName("null 헤더 - null 반환")
        void extractUsername_nullHeader() {
            String username = AuthUtil.extractUsername(null, jwtUtil);

            assertNull(username);
            verify(jwtUtil, never()).getUsernameFromToken(anyString());
        }

        @Test
        @DisplayName("특수문자 포함 사용자명 추출")
        void extractUsername_withSpecialChars() {
            String header = "Bearer token";
            when(jwtUtil.getUsernameFromToken("token")).thenReturn("user@example.com");

            String username = AuthUtil.extractUsername(header, jwtUtil);

            assertEquals("user@example.com", username);
        }
    }
}
