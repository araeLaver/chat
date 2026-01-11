package com.beam;

import com.beam.util.IpAddressExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API Rate Limiting 필터
 * - IP 기반 요청 제한
 * - 슬라이딩 윈도우 알고리즘
 * - 엔드포인트별 차등 제한
 * - Rate Limit 헤더 지원
 * - DDoS 및 스팸 방지
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Rate Limit HTTP 헤더 상수
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    // IP별 요청 카운터 (IP -> RequestInfo)
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    // 엔드포인트별 요청 카운터 (IP:endpoint -> RequestInfo)
    private final Map<String, RequestInfo> endpointRequestCounts = new ConcurrentHashMap<>();

    // 기본 설정값
    @Value("${rate-limit.default.requests-per-minute:60}")
    private int defaultMaxRequestsPerMinute;

    @Value("${rate-limit.default.requests-per-second:10}")
    private int defaultMaxRequestsPerSecond;

    // 인증 엔드포인트 설정 (더 엄격)
    @Value("${rate-limit.auth.requests-per-minute:20}")
    private int authMaxRequestsPerMinute;

    @Value("${rate-limit.auth.requests-per-second:5}")
    private int authMaxRequestsPerSecond;

    // 파일 업로드 엔드포인트 설정
    @Value("${rate-limit.upload.requests-per-minute:30}")
    private int uploadMaxRequestsPerMinute;

    @Autowired
    private IpAddressExtractor ipAddressExtractor;

    private static final long WINDOW_SIZE_MS = 60000;        // 1분 윈도우
    private static final long CLEANUP_INTERVAL_MS = 300000;  // 5분마다 정리

    private long lastCleanupTime = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 정적 리소스는 Rate Limit 제외
        String uri = request.getRequestURI();
        if (isStaticResource(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ipAddressExtractor.extractClientIp(request);
        long currentTime = System.currentTimeMillis();

        // 주기적으로 오래된 항목 정리
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanupOldEntries(currentTime);
            lastCleanupTime = currentTime;
        }

        // 엔드포인트 타입에 따른 제한 설정
        EndpointLimit limits = getEndpointLimits(uri);

        // 요청 정보 가져오기 또는 생성
        RequestInfo requestInfo = requestCounts.computeIfAbsent(clientIp,
            k -> new RequestInfo(limits.maxPerMinute, limits.maxPerSecond));

        // 설정 업데이트 (동적 설정 변경 대응)
        requestInfo.updateLimits(limits.maxPerMinute, limits.maxPerSecond);

        // Rate Limit 체크
        RateLimitResult result = requestInfo.checkAndAllowRequest(currentTime);

        // Rate Limit 헤더 추가 (항상)
        addRateLimitHeaders(response, result, limits.maxPerMinute);

        if (!result.allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.retryAfterSeconds));
            response.getWriter().write(
                "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Please try again in "
                + result.retryAfterSeconds + " seconds.\", \"retryAfter\": " + result.retryAfterSeconds + "}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit HTTP 헤더 추가
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result, int limit) {
        response.setHeader(HEADER_LIMIT, String.valueOf(limit));
        response.setHeader(HEADER_REMAINING, String.valueOf(Math.max(0, result.remaining)));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetTimestamp));
    }

    /**
     * 엔드포인트별 Rate Limit 설정 반환
     */
    private EndpointLimit getEndpointLimits(String uri) {
        // 인증 관련 엔드포인트 (더 엄격한 제한)
        if (uri.contains("/auth/") || uri.contains("/login") || uri.contains("/register")) {
            return new EndpointLimit(authMaxRequestsPerMinute, authMaxRequestsPerSecond);
        }
        // 파일 업로드 엔드포인트
        if (uri.contains("/files/upload")) {
            return new EndpointLimit(uploadMaxRequestsPerMinute, 3);
        }
        // 기본 설정
        return new EndpointLimit(defaultMaxRequestsPerMinute, defaultMaxRequestsPerSecond);
    }

    /**
     * 엔드포인트별 제한 설정 클래스
     */
    private static class EndpointLimit {
        final int maxPerMinute;
        final int maxPerSecond;

        EndpointLimit(int maxPerMinute, int maxPerSecond) {
            this.maxPerMinute = maxPerMinute;
            this.maxPerSecond = maxPerSecond;
        }
    }

    /**
     * Rate Limit 체크 결과
     */
    private static class RateLimitResult {
        final boolean allowed;
        final int remaining;
        final long resetTimestamp;
        final int retryAfterSeconds;

        RateLimitResult(boolean allowed, int remaining, long resetTimestamp, int retryAfterSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetTimestamp = resetTimestamp;
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }

    /**
     * 정적 리소스 체크
     */
    private boolean isStaticResource(String uri) {
        return uri.endsWith(".css") ||
               uri.endsWith(".js") ||
               uri.endsWith(".png") ||
               uri.endsWith(".jpg") ||
               uri.endsWith(".jpeg") ||
               uri.endsWith(".gif") ||
               uri.endsWith(".svg") ||
               uri.endsWith(".ico") ||
               uri.endsWith(".woff") ||
               uri.endsWith(".woff2") ||
               uri.endsWith(".ttf") ||
               uri.contains("/static/");
    }

    /**
     * 오래된 항목 정리
     */
    private void cleanupOldEntries(long currentTime) {
        requestCounts.entrySet().removeIf(entry ->
            currentTime - entry.getValue().getWindowStart() > WINDOW_SIZE_MS * 2
        );
    }

    /**
     * 요청 정보 클래스 (슬라이딩 윈도우)
     */
    private static class RequestInfo {
        private long windowStart;
        private AtomicInteger requestCount;
        private long lastRequestTime;
        private AtomicInteger requestsThisSecond;
        private int maxPerMinute;
        private int maxPerSecond;

        public RequestInfo(int maxPerMinute, int maxPerSecond) {
            this.windowStart = System.currentTimeMillis();
            this.requestCount = new AtomicInteger(0);
            this.lastRequestTime = 0;
            this.requestsThisSecond = new AtomicInteger(0);
            this.maxPerMinute = maxPerMinute;
            this.maxPerSecond = maxPerSecond;
        }

        public void updateLimits(int maxPerMinute, int maxPerSecond) {
            this.maxPerMinute = maxPerMinute;
            this.maxPerSecond = maxPerSecond;
        }

        public synchronized RateLimitResult checkAndAllowRequest(long currentTime) {
            // 윈도우가 만료되면 초기화
            if (currentTime - windowStart > WINDOW_SIZE_MS) {
                windowStart = currentTime;
                requestCount.set(0);
                requestsThisSecond.set(0);
                lastRequestTime = currentTime;
            }

            long resetTimestamp = (windowStart + WINDOW_SIZE_MS) / 1000;
            int remaining = maxPerMinute - requestCount.get();

            // 초당 제한 체크
            if (currentTime - lastRequestTime < 1000) {
                if (requestsThisSecond.get() >= maxPerSecond) {
                    int retryAfter = (int) Math.ceil((1000 - (currentTime - lastRequestTime)) / 1000.0);
                    return new RateLimitResult(false, remaining, resetTimestamp, Math.max(1, retryAfter));
                }
            } else {
                // 새로운 초 시작
                requestsThisSecond.set(0);
                lastRequestTime = currentTime;
            }

            // 분당 제한 체크
            if (requestCount.get() >= maxPerMinute) {
                int retryAfter = (int) Math.ceil((WINDOW_SIZE_MS - (currentTime - windowStart)) / 1000.0);
                return new RateLimitResult(false, 0, resetTimestamp, Math.max(1, retryAfter));
            }

            // 요청 허용
            requestCount.incrementAndGet();
            requestsThisSecond.incrementAndGet();
            remaining = maxPerMinute - requestCount.get();

            return new RateLimitResult(true, remaining, resetTimestamp, 0);
        }

        public long getWindowStart() {
            return windowStart;
        }
    }
}
