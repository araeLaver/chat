package com.beam;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
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
 *   <li>Automatic cleanup of expired entries to prevent memory leaks</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    // Bucket과 마지막 접근 시간을 함께 저장
    private final Map<String, BucketEntry> apiBuckets = new ConcurrentHashMap<>();
    private final Map<String, BucketEntry> webSocketBuckets = new ConcurrentHashMap<>();

    // TTL 설정 (기본: API 30분, WebSocket 10분)
    @Value("${rate.limit.api.ttl-minutes:30}")
    private long apiTtlMinutes;

    @Value("${rate.limit.websocket.ttl-minutes:10}")
    private long wsTtlMinutes;

    // 최대 버킷 개수 제한 (메모리 보호)
    @Value("${rate.limit.max-buckets:10000}")
    private int maxBuckets;

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

    @PostConstruct
    public void init() {
        logger.info("RateLimitService initialized - API TTL: {}min, WebSocket TTL: {}min, Max buckets: {}",
                apiTtlMinutes, wsTtlMinutes, maxBuckets);
    }

    /**
     * Check if an API request is allowed for the given identifier (usually IP address)
     *
     * @param identifier Unique identifier (IP address, user ID, etc.)
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isApiRequestAllowed(String identifier) {
        // 최대 버킷 수 초과 시 새 버킷 생성 거부 (DoS 방어)
        if (!apiBuckets.containsKey(identifier) && apiBuckets.size() >= maxBuckets) {
            logger.warn("API bucket limit reached ({}), rejecting new identifier: {}", maxBuckets, identifier);
            return false;
        }

        BucketEntry entry = apiBuckets.computeIfAbsent(identifier, k -> new BucketEntry(createApiBucket()));
        entry.updateLastAccess();
        return entry.getBucket().tryConsume(1);
    }

    /**
     * Check if a WebSocket message is allowed for the given session
     *
     * @param sessionId WebSocket session identifier
     * @return true if message is allowed, false if rate limit exceeded
     */
    public boolean isWebSocketMessageAllowed(String sessionId) {
        // 최대 버킷 수 초과 시 새 버킷 생성 거부 (DoS 방어)
        if (!webSocketBuckets.containsKey(sessionId) && webSocketBuckets.size() >= maxBuckets) {
            logger.warn("WebSocket bucket limit reached ({}), rejecting new session: {}", maxBuckets, sessionId);
            return false;
        }

        BucketEntry entry = webSocketBuckets.computeIfAbsent(sessionId, k -> new BucketEntry(createWebSocketBucket()));
        entry.updateLastAccess();
        return entry.getBucket().tryConsume(1);
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
        BucketEntry entry = apiBuckets.get(identifier);
        return entry != null ? entry.getBucket().getAvailableTokens() : apiCapacity;
    }

    /**
     * Get remaining tokens for WebSocket messages
     *
     * @param sessionId WebSocket session identifier
     * @return Number of available tokens
     */
    public long getWebSocketRemainingTokens(String sessionId) {
        BucketEntry entry = webSocketBuckets.get(sessionId);
        return entry != null ? entry.getBucket().getAvailableTokens() : wsCapacity;
    }

    /**
     * Scheduled cleanup of expired bucket entries (runs every 5 minutes)
     * Prevents memory leak from accumulated rate limit buckets
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupExpiredBuckets() {
        int apiRemoved = cleanupMap(apiBuckets, Duration.ofMinutes(apiTtlMinutes));
        int wsRemoved = cleanupMap(webSocketBuckets, Duration.ofMinutes(wsTtlMinutes));

        if (apiRemoved > 0 || wsRemoved > 0) {
            logger.info("Rate limit cleanup: removed {} API buckets, {} WebSocket buckets. Current: API={}, WS={}",
                    apiRemoved, wsRemoved, apiBuckets.size(), webSocketBuckets.size());
        }
    }

    private int cleanupMap(Map<String, BucketEntry> map, Duration ttl) {
        int removed = 0;
        Instant cutoff = Instant.now().minus(ttl);

        Iterator<Map.Entry<String, BucketEntry>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BucketEntry> entry = iterator.next();
            if (entry.getValue().getLastAccess().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
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
        logger.info("All rate limiters cleared");
    }

    /**
     * Get current bucket counts for monitoring
     */
    public Map<String, Integer> getBucketCounts() {
        return Map.of(
                "api", apiBuckets.size(),
                "webSocket", webSocketBuckets.size()
        );
    }

    /**
     * Inner class to hold bucket with last access time for TTL management
     */
    private static class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastAccess;

        public BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        public Bucket getBucket() {
            return bucket;
        }

        public Instant getLastAccess() {
            return lastAccess;
        }

        public void updateLastAccess() {
            this.lastAccess = Instant.now();
        }
    }
}
