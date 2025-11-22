package com.beam;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service using Token Bucket Algorithm
 *
 * <p>Provides rate limiting for API endpoints and WebSocket messages
 * to prevent spam and DoS attacks.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Per-IP rate limiting for API requests</li>
 *   <li>Per-session rate limiting for WebSocket messages</li>
 *   <li>Configurable capacity and refill rates</li>
 *   <li>Token bucket algorithm for smooth traffic flow</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Service
public class RateLimitService {

    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> webSocketBuckets = new ConcurrentHashMap<>();

    @Value("${rate.limit.api.capacity:100}")
    private long apiCapacity;

    @Value("${rate.limit.api.refill-tokens:100}")
    private long apiRefillTokens;

    @Value("${rate.limit.api.refill-duration-minutes:1}")
    private long apiRefillMinutes;

    @Value("${rate.limit.websocket.capacity:50}")
    private long wsCapacity;

    @Value("${rate.limit.websocket.refill-tokens:50}")
    private long wsRefillTokens;

    @Value("${rate.limit.websocket.refill-duration-seconds:10}")
    private long wsRefillSeconds;

    /**
     * Check if an API request is allowed for the given identifier (usually IP address)
     *
     * @param identifier Unique identifier (IP address, user ID, etc.)
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isApiRequestAllowed(String identifier) {
        Bucket bucket = apiBuckets.computeIfAbsent(identifier, k -> createApiBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if a WebSocket message is allowed for the given session
     *
     * @param sessionId WebSocket session identifier
     * @return true if message is allowed, false if rate limit exceeded
     */
    public boolean isWebSocketMessageAllowed(String sessionId) {
        Bucket bucket = webSocketBuckets.computeIfAbsent(sessionId, k -> createWebSocketBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Remove rate limiter for API client (e.g., on logout or session expiry)
     *
     * @param identifier Client identifier
     */
    public void removeApiLimiter(String identifier) {
        apiBuckets.remove(identifier);
    }

    /**
     * Remove rate limiter for WebSocket session (e.g., on disconnect)
     *
     * @param sessionId WebSocket session identifier
     */
    public void removeWebSocketLimiter(String sessionId) {
        webSocketBuckets.remove(sessionId);
    }

    /**
     * Get remaining tokens for API requests
     *
     * @param identifier Client identifier
     * @return Number of available tokens
     */
    public long getApiRemainingTokens(String identifier) {
        Bucket bucket = apiBuckets.get(identifier);
        return bucket != null ? bucket.getAvailableTokens() : apiCapacity;
    }

    /**
     * Get remaining tokens for WebSocket messages
     *
     * @param sessionId WebSocket session identifier
     * @return Number of available tokens
     */
    public long getWebSocketRemainingTokens(String sessionId) {
        Bucket bucket = webSocketBuckets.get(sessionId);
        return bucket != null ? bucket.getAvailableTokens() : wsCapacity;
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(
                apiCapacity,
                Refill.intervally(apiRefillTokens, Duration.ofMinutes(apiRefillMinutes))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createWebSocketBucket() {
        Bandwidth limit = Bandwidth.classic(
                wsCapacity,
                Refill.intervally(wsRefillTokens, Duration.ofSeconds(wsRefillSeconds))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Clear all rate limiters (useful for testing or admin operations)
     */
    public void clearAllLimiters() {
        apiBuckets.clear();
        webSocketBuckets.clear();
    }
}
