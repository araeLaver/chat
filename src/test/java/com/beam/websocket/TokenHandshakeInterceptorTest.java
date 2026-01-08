package com.beam.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TokenHandshakeInterceptor Unit Tests")
class TokenHandshakeInterceptorTest {

    private TokenHandshakeInterceptor interceptor;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler wsHandler;
    private Map<String, Object> attributes;
    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;

    @BeforeEach
    void setUp() {
        interceptor = new TokenHandshakeInterceptor();
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        wsHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
        requestHeaders = new HttpHeaders();
        responseHeaders = new HttpHeaders();

        when(request.getHeaders()).thenReturn(requestHeaders);
        when(response.getHeaders()).thenReturn(responseHeaders);
    }

    @Nested
    @DisplayName("beforeHandshake Tests")
    class BeforeHandshakeTests {

        @Test
        @DisplayName("Should extract token from Sec-WebSocket-Protocol header")
        void shouldExtractTokenFromSecWebSocketProtocolHeader() {
            requestHeaders.put("Sec-WebSocket-Protocol",
                Arrays.asList("access_token, eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", attributes.get("token"));
        }

        @Test
        @DisplayName("Should set Sec-WebSocket-Protocol response header")
        void shouldSetSecWebSocketProtocolResponseHeader() {
            String protocol = "access_token, test_token";
            requestHeaders.put("Sec-WebSocket-Protocol", Arrays.asList(protocol));

            interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertEquals(protocol, responseHeaders.getFirst("Sec-WebSocket-Protocol"));
        }

        @Test
        @DisplayName("Should return true when no protocol header")
        void shouldReturnTrueWhenNoProtocolHeader() {
            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertNull(attributes.get("token"));
        }

        @Test
        @DisplayName("Should return true when protocol header is empty")
        void shouldReturnTrueWhenProtocolHeaderIsEmpty() {
            requestHeaders.put("Sec-WebSocket-Protocol", Arrays.asList());

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertNull(attributes.get("token"));
        }

        @Test
        @DisplayName("Should not extract token when protocol does not start with access_token")
        void shouldNotExtractTokenWhenProtocolDoesNotStartWithAccessToken() {
            requestHeaders.put("Sec-WebSocket-Protocol", Arrays.asList("other_protocol"));

            boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertTrue(result);
            assertNull(attributes.get("token"));
        }

        @Test
        @DisplayName("Should trim whitespace from token")
        void shouldTrimWhitespaceFromToken() {
            requestHeaders.put("Sec-WebSocket-Protocol",
                Arrays.asList("access_token,   token_with_spaces   "));

            interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertEquals("token_with_spaces", attributes.get("token"));
        }

        @Test
        @DisplayName("Should handle multiple protocol values")
        void shouldHandleMultipleProtocolValues() {
            requestHeaders.put("Sec-WebSocket-Protocol",
                Arrays.asList("access_token, first_token", "second_protocol"));

            interceptor.beforeHandshake(request, response, wsHandler, attributes);

            assertEquals("first_token", attributes.get("token"));
        }
    }

    @Nested
    @DisplayName("afterHandshake Tests")
    class AfterHandshakeTests {

        @Test
        @DisplayName("Should complete without exception")
        void shouldCompleteWithoutException() {
            assertDoesNotThrow(() ->
                interceptor.afterHandshake(request, response, wsHandler, null));
        }

        @Test
        @DisplayName("Should handle exception parameter gracefully")
        void shouldHandleExceptionParameterGracefully() {
            Exception exception = new RuntimeException("Test exception");
            assertDoesNotThrow(() ->
                interceptor.afterHandshake(request, response, wsHandler, exception));
        }
    }
}
