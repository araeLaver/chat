package com.beam;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal Tests")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Should continue filter chain when no Authorization header")
        void shouldContinueFilterChainWhenNoAuthorizationHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should continue filter chain when Authorization header is not Bearer")
        void shouldContinueFilterChainWhenAuthorizationHeaderIsNotBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should set authentication when valid token and active user")
        void shouldSetAuthenticationWhenValidTokenAndActiveUser() throws Exception {
            String token = "valid.jwt.token";
            UserEntity user = new UserEntity();
            user.setId(1L);
            user.setUsername("testuser");
            user.setIsActive(true);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.validateToken(token)).thenReturn(true);
            when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(1L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        }

        @Test
        @DisplayName("Should not set authentication when user is inactive")
        void shouldNotSetAuthenticationWhenUserIsInactive() throws Exception {
            String token = "valid.jwt.token";
            UserEntity user = new UserEntity();
            user.setId(1L);
            user.setUsername("testuser");
            user.setIsActive(false);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.validateToken(token)).thenReturn(true);
            when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should not set authentication when user not found")
        void shouldNotSetAuthenticationWhenUserNotFound() throws Exception {
            String token = "valid.jwt.token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.validateToken(token)).thenReturn(true);
            when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
            when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should not set authentication when token is invalid")
        void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
            String token = "invalid.jwt.token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.validateToken(token)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should continue filter chain when exception occurs")
        void shouldContinueFilterChainWhenExceptionOccurs() throws Exception {
            String token = "valid.jwt.token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtUtil.validateToken(token)).thenThrow(new RuntimeException("Token error"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
