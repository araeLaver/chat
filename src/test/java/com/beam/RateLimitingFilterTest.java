package com.beam;

import com.beam.util.IpAddressExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private IpAddressExtractor ipAddressExtractor;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String CLIENT_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
        ReflectionTestUtils.setField(rateLimitingFilter, "ipAddressExtractor", ipAddressExtractor);
        ReflectionTestUtils.setField(rateLimitingFilter, "defaultMaxRequestsPerMinute", 60);
        ReflectionTestUtils.setField(rateLimitingFilter, "defaultMaxRequestsPerSecond", 10);
        ReflectionTestUtils.setField(rateLimitingFilter, "authMaxRequestsPerMinute", 20);
        ReflectionTestUtils.setField(rateLimitingFilter, "authMaxRequestsPerSecond", 5);
        ReflectionTestUtils.setField(rateLimitingFilter, "uploadMaxRequestsPerMinute", 30);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(ipAddressExtractor.extractClientIp(any())).thenReturn(CLIENT_IP);
    }

    @Nested
    @DisplayName("Static Resource Bypass Tests")
    class StaticResourceTests {

        @Test
        @DisplayName("Should bypass CSS files")
        void shouldBypassCssFiles() throws ServletException, IOException {
            request.setRequestURI("/static/style.css");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(ipAddressExtractor);
        }

        @Test
        @DisplayName("Should bypass JS files")
        void shouldBypassJsFiles() throws ServletException, IOException {
            request.setRequestURI("/assets/app.js");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should bypass image files")
        void shouldBypassImageFiles() throws ServletException, IOException {
            request.setRequestURI("/images/logo.png");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should bypass static path")
        void shouldBypassStaticPath() throws ServletException, IOException {
            request.setRequestURI("/static/some-resource");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should bypass font files")
        void shouldBypassFontFiles() throws ServletException, IOException {
            request.setRequestURI("/fonts/roboto.woff2");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Rate Limit Header Tests")
    class RateLimitHeaderTests {

        @Test
        @DisplayName("Should add rate limit headers to response")
        void shouldAddRateLimitHeaders() throws ServletException, IOException {
            request.setRequestURI("/api/messages");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("60");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        }

        @Test
        @DisplayName("Should decrease remaining count with each request")
        void shouldDecreaseRemainingCount() throws ServletException, IOException {
            request.setRequestURI("/api/messages");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            int firstRemaining = Integer.parseInt(response.getHeader("X-RateLimit-Remaining"));

            response = new MockHttpServletResponse();
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            int secondRemaining = Integer.parseInt(response.getHeader("X-RateLimit-Remaining"));

            assertThat(secondRemaining).isLessThan(firstRemaining);
        }
    }

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @DisplayName("Should allow request within limit")
        void shouldAllowRequestWithinLimit() throws ServletException, IOException {
            request.setRequestURI("/api/messages");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should block request when minute limit exceeded")
        void shouldBlockRequestWhenMinuteLimitExceeded() throws ServletException, IOException {
            ReflectionTestUtils.setField(rateLimitingFilter, "defaultMaxRequestsPerMinute", 3);
            ReflectionTestUtils.setField(rateLimitingFilter, "defaultMaxRequestsPerSecond", 100);
            request.setRequestURI("/api/messages");

            // Make requests up to limit
            for (int i = 0; i < 3; i++) {
                response = new MockHttpServletResponse();
                rateLimitingFilter.doFilterInternal(request, response, filterChain);
            }

            // Next request should be blocked
            response = new MockHttpServletResponse();
            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getHeader("Retry-After")).isNotNull();
        }

        @Test
        @DisplayName("Should return error JSON when rate limited")
        void shouldReturnErrorJsonWhenRateLimited() throws ServletException, IOException {
            ReflectionTestUtils.setField(rateLimitingFilter, "defaultMaxRequestsPerMinute", 1);
            ReflectionTestUtils.setField(rateLimitingFilter, "defaultMaxRequestsPerSecond", 100);
            request.setRequestURI("/api/messages");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            response = new MockHttpServletResponse();
            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentAsString()).contains("Too many requests");
            assertThat(response.getContentAsString()).contains("retryAfter");
        }
    }

    @Nested
    @DisplayName("Endpoint-Specific Limits Tests")
    class EndpointSpecificLimitsTests {

        @Test
        @DisplayName("Should apply stricter limits for auth endpoints")
        void shouldApplyStricterLimitsForAuthEndpoints() throws ServletException, IOException {
            request.setRequestURI("/api/auth/login");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            // Auth endpoint should have limit of 20
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
        }

        @Test
        @DisplayName("Should apply stricter limits for register endpoint")
        void shouldApplyStricterLimitsForRegisterEndpoint() throws ServletException, IOException {
            request.setRequestURI("/api/register");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
        }

        @Test
        @DisplayName("Should apply upload limits for file upload endpoint")
        void shouldApplyUploadLimitsForFileUploadEndpoint() throws ServletException, IOException {
            request.setRequestURI("/api/files/upload");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("30");
        }

        @Test
        @DisplayName("Should apply default limits for regular API endpoints")
        void shouldApplyDefaultLimitsForRegularEndpoints() throws ServletException, IOException {
            request.setRequestURI("/api/chat/rooms");

            rateLimitingFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("60");
        }
    }

    @Nested
    @DisplayName("IP-Based Rate Limiting Tests")
    class IpBasedRateLimitingTests {

        @Test
        @DisplayName("Should track requests per IP")
        void shouldTrackRequestsPerIp() throws ServletException, IOException {
            request.setRequestURI("/api/messages");

            // First IP
            when(ipAddressExtractor.extractClientIp(any())).thenReturn("192.168.1.1");
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            String firstIpRemaining = response.getHeader("X-RateLimit-Remaining");

            // Second IP - should have fresh limit
            response = new MockHttpServletResponse();
            when(ipAddressExtractor.extractClientIp(any())).thenReturn("192.168.1.2");
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
            String secondIpRemaining = response.getHeader("X-RateLimit-Remaining");

            assertThat(firstIpRemaining).isEqualTo(secondIpRemaining);
        }
    }
}
