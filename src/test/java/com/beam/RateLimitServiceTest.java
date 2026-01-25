package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
        // Set default values via reflection
        ReflectionTestUtils.setField(rateLimitService, "apiCapacity", 100L);
        ReflectionTestUtils.setField(rateLimitService, "apiRefillTokens", 100L);
        ReflectionTestUtils.setField(rateLimitService, "apiRefillMinutes", 1L);
        ReflectionTestUtils.setField(rateLimitService, "wsCapacity", 50L);
        ReflectionTestUtils.setField(rateLimitService, "wsRefillTokens", 50L);
        ReflectionTestUtils.setField(rateLimitService, "wsRefillSeconds", 10L);
        // Set TTL and max buckets for memory management
        ReflectionTestUtils.setField(rateLimitService, "apiTtlMinutes", 30L);
        ReflectionTestUtils.setField(rateLimitService, "wsTtlMinutes", 10L);
        ReflectionTestUtils.setField(rateLimitService, "maxBuckets", 10000);
    }

    @Nested
    @DisplayName("API Rate Limiting Tests")
    class ApiRateLimitingTests {

        @Test
        @DisplayName("Should allow first API request")
        void shouldAllowFirstApiRequest() {
            boolean allowed = rateLimitService.isApiRequestAllowed("192.168.1.1");
            assertTrue(allowed);
        }

        @Test
        @DisplayName("Should allow multiple requests within limit")
        void shouldAllowMultipleRequestsWithinLimit() {
            String identifier = "192.168.1.2";
            for (int i = 0; i < 50; i++) {
                assertTrue(rateLimitService.isApiRequestAllowed(identifier));
            }
        }

        @Test
        @DisplayName("Should block requests after capacity exceeded")
        void shouldBlockRequestsAfterCapacityExceeded() {
            String identifier = "192.168.1.3";
            // Exhaust all tokens
            for (int i = 0; i < 100; i++) {
                rateLimitService.isApiRequestAllowed(identifier);
            }
            // Next request should be blocked
            assertFalse(rateLimitService.isApiRequestAllowed(identifier));
        }

        @Test
        @DisplayName("Should track different IPs separately")
        void shouldTrackDifferentIpsSeparately() {
            String ip1 = "192.168.1.10";
            String ip2 = "192.168.1.11";

            // Exhaust IP1
            for (int i = 0; i < 100; i++) {
                rateLimitService.isApiRequestAllowed(ip1);
            }

            // IP1 should be blocked, IP2 should be allowed
            assertFalse(rateLimitService.isApiRequestAllowed(ip1));
            assertTrue(rateLimitService.isApiRequestAllowed(ip2));
        }
    }

    @Nested
    @DisplayName("WebSocket Rate Limiting Tests")
    class WebSocketRateLimitingTests {

        @Test
        @DisplayName("Should allow first WebSocket message")
        void shouldAllowFirstWebSocketMessage() {
            boolean allowed = rateLimitService.isWebSocketMessageAllowed("session-1");
            assertTrue(allowed);
        }

        @Test
        @DisplayName("Should allow multiple messages within limit")
        void shouldAllowMultipleMessagesWithinLimit() {
            String sessionId = "session-2";
            for (int i = 0; i < 25; i++) {
                assertTrue(rateLimitService.isWebSocketMessageAllowed(sessionId));
            }
        }

        @Test
        @DisplayName("Should block messages after capacity exceeded")
        void shouldBlockMessagesAfterCapacityExceeded() {
            String sessionId = "session-3";
            // Exhaust all tokens (50)
            for (int i = 0; i < 50; i++) {
                rateLimitService.isWebSocketMessageAllowed(sessionId);
            }
            // Next message should be blocked
            assertFalse(rateLimitService.isWebSocketMessageAllowed(sessionId));
        }

        @Test
        @DisplayName("Should track different sessions separately")
        void shouldTrackDifferentSessionsSeparately() {
            String session1 = "session-10";
            String session2 = "session-11";

            // Exhaust session1
            for (int i = 0; i < 50; i++) {
                rateLimitService.isWebSocketMessageAllowed(session1);
            }

            // Session1 should be blocked, Session2 should be allowed
            assertFalse(rateLimitService.isWebSocketMessageAllowed(session1));
            assertTrue(rateLimitService.isWebSocketMessageAllowed(session2));
        }
    }

    @Nested
    @DisplayName("Remove Limiter Tests")
    class RemoveLimiterTests {

        @Test
        @DisplayName("Should remove API limiter")
        void shouldRemoveApiLimiter() {
            String identifier = "192.168.1.100";

            // Use some tokens
            for (int i = 0; i < 50; i++) {
                rateLimitService.isApiRequestAllowed(identifier);
            }

            // Remove limiter
            rateLimitService.removeApiLimiter(identifier);

            // Should get full capacity again
            assertEquals(100, rateLimitService.getApiRemainingTokens(identifier));
        }

        @Test
        @DisplayName("Should remove WebSocket limiter")
        void shouldRemoveWebSocketLimiter() {
            String sessionId = "session-100";

            // Use some tokens
            for (int i = 0; i < 25; i++) {
                rateLimitService.isWebSocketMessageAllowed(sessionId);
            }

            // Remove limiter
            rateLimitService.removeWebSocketLimiter(sessionId);

            // Should get full capacity again
            assertEquals(50, rateLimitService.getWebSocketRemainingTokens(sessionId));
        }

        @Test
        @DisplayName("Should handle removing non-existent limiter gracefully")
        void shouldHandleRemovingNonExistentLimiterGracefully() {
            assertDoesNotThrow(() -> rateLimitService.removeApiLimiter("non-existent"));
            assertDoesNotThrow(() -> rateLimitService.removeWebSocketLimiter("non-existent"));
        }
    }

    @Nested
    @DisplayName("Get Remaining Tokens Tests")
    class GetRemainingTokensTests {

        @Test
        @DisplayName("Should return full capacity for new API client")
        void shouldReturnFullCapacityForNewApiClient() {
            long remaining = rateLimitService.getApiRemainingTokens("new-client");
            assertEquals(100, remaining);
        }

        @Test
        @DisplayName("Should return full capacity for new WebSocket session")
        void shouldReturnFullCapacityForNewWebSocketSession() {
            long remaining = rateLimitService.getWebSocketRemainingTokens("new-session");
            assertEquals(50, remaining);
        }

        @Test
        @DisplayName("Should return correct remaining tokens after consumption")
        void shouldReturnCorrectRemainingTokensAfterConsumption() {
            String identifier = "client-tokens";
            rateLimitService.isApiRequestAllowed(identifier);
            rateLimitService.isApiRequestAllowed(identifier);

            long remaining = rateLimitService.getApiRemainingTokens(identifier);
            assertEquals(98, remaining);
        }

        @Test
        @DisplayName("Should return correct remaining WebSocket tokens after consumption")
        void shouldReturnCorrectRemainingWebSocketTokensAfterConsumption() {
            String sessionId = "session-tokens";
            rateLimitService.isWebSocketMessageAllowed(sessionId);
            rateLimitService.isWebSocketMessageAllowed(sessionId);

            long remaining = rateLimitService.getWebSocketRemainingTokens(sessionId);
            assertEquals(48, remaining);
        }
    }

    @Nested
    @DisplayName("Clear All Limiters Tests")
    class ClearAllLimitersTests {

        @Test
        @DisplayName("Should clear all API limiters")
        void shouldClearAllApiLimiters() {
            // Create some limiters
            for (int i = 0; i < 50; i++) {
                rateLimitService.isApiRequestAllowed("client-" + i);
            }

            // Clear all
            rateLimitService.clearAllLimiters();

            // All should have full capacity again
            assertEquals(100, rateLimitService.getApiRemainingTokens("client-0"));
        }

        @Test
        @DisplayName("Should clear all WebSocket limiters")
        void shouldClearAllWebSocketLimiters() {
            // Create some limiters
            for (int i = 0; i < 25; i++) {
                rateLimitService.isWebSocketMessageAllowed("session-" + i);
            }

            // Clear all
            rateLimitService.clearAllLimiters();

            // All should have full capacity again
            assertEquals(50, rateLimitService.getWebSocketRemainingTokens("session-0"));
        }
    }
}
