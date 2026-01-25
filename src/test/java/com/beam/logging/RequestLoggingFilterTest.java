package com.beam.logging;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RequestLoggingFilter Tests")
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @Mock
    private IpAddressExtractor ipAddressExtractor;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        ReflectionTestUtils.setField(filter, "ipAddressExtractor", ipAddressExtractor);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(ipAddressExtractor.extractClientIp(any())).thenReturn("192.168.1.100");
    }

    @Nested
    @DisplayName("doFilterInternal() Tests")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Should call filter chain for normal request")
        void shouldCallFilterChainForNormalRequest() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/users");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should extract client IP for request")
        void shouldExtractClientIpForRequest() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/messages");

            filter.doFilterInternal(request, response, filterChain);

            verify(ipAddressExtractor).extractClientIp(request);
        }

        @Test
        @DisplayName("Should handle GET request")
        void shouldHandleGetRequest() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/rooms");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle POST request")
        void shouldHandlePostRequest() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/rooms");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle request with error response")
        void shouldHandleRequestWithErrorResponse() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/notfound");
            response.setStatus(404);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle request with 500 error")
        void shouldHandleRequestWith500Error() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/error");
            response.setStatus(500);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("shouldNotFilter() Tests")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("Should not filter /actuator/health")
        void shouldNotFilterActuatorHealth() {
            request.setRequestURI("/actuator/health");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should not filter /health")
        void shouldNotFilterHealth() {
            request.setRequestURI("/health");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should filter normal API endpoint")
        void shouldFilterNormalApiEndpoint() {
            request.setRequestURI("/api/users");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should filter /actuator/metrics")
        void shouldFilterActuatorMetrics() {
            request.setRequestURI("/actuator/metrics");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Static Resource Detection Tests")
    class StaticResourceDetectionTests {

        @Test
        @DisplayName("Should log static JS resource at debug level")
        void shouldLogStaticJsResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/app.js");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static CSS resource at debug level")
        void shouldLogStaticCssResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/styles.css");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static PNG resource at debug level")
        void shouldLogStaticPngResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/logo.png");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static JPG resource at debug level")
        void shouldLogStaticJpgResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/image.jpg");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static ICO resource at debug level")
        void shouldLogStaticIcoResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/favicon.ico");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static WOFF resource at debug level")
        void shouldLogStaticWoffResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/font.woff");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static WOFF2 resource at debug level")
        void shouldLogStaticWoff2ResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/font.woff2");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log static TTF resource at debug level")
        void shouldLogStaticTtfResourceAtDebugLevel() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/font.ttf");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log /static/ path as static resource")
        void shouldLogStaticPathAsStaticResource() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/static/bundle.js");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should log /assets/ path as static resource")
        void shouldLogAssetsPathAsStaticResource() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/assets/image.png");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Response Status Logging Tests")
    class ResponseStatusLoggingTests {

        @Test
        @DisplayName("Should handle 200 OK response")
        void shouldHandle200OkResponse() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/data");
            response.setStatus(200);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle 201 Created response")
        void shouldHandle201CreatedResponse() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/items");
            response.setStatus(201);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle 400 Bad Request response")
        void shouldHandle400BadRequestResponse() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/api/data");
            response.setStatus(400);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle 401 Unauthorized response")
        void shouldHandle401UnauthorizedResponse() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/secure");
            response.setStatus(401);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle 403 Forbidden response")
        void shouldHandle403ForbiddenResponse() throws ServletException, IOException {
            request.setMethod("DELETE");
            request.setRequestURI("/api/admin");
            response.setStatus(403);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle 404 Not Found response")
        void shouldHandle404NotFoundResponse() throws ServletException, IOException {
            request.setMethod("GET");
            request.setRequestURI("/api/unknown");
            response.setStatus(404);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
