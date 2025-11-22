package com.beam;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * - DDoS 및 스팸 방지
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // IP별 요청 카운터 (IP -> RequestInfo)
    private final Map<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    // 설정값
    private static final int MAX_REQUESTS_PER_MINUTE = 60;  // 분당 최대 요청 수
    private static final int MAX_REQUESTS_PER_SECOND = 10;   // 초당 최대 요청 수
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

        String clientIp = getClientIP(request);
        long currentTime = System.currentTimeMillis();

        // 주기적으로 오래된 항목 정리
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanupOldEntries(currentTime);
            lastCleanupTime = currentTime;
        }

        // 요청 정보 가져오기 또는 생성
        RequestInfo requestInfo = requestCounts.computeIfAbsent(clientIp,
            k -> new RequestInfo());

        // Rate Limit 체크
        if (!requestInfo.allowRequest(currentTime)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
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

        public RequestInfo() {
            this.windowStart = System.currentTimeMillis();
            this.requestCount = new AtomicInteger(0);
            this.lastRequestTime = 0;
            this.requestsThisSecond = new AtomicInteger(0);
        }

        public synchronized boolean allowRequest(long currentTime) {
            // 윈도우가 만료되면 초기화
            if (currentTime - windowStart > WINDOW_SIZE_MS) {
                windowStart = currentTime;
                requestCount.set(0);
                requestsThisSecond.set(0);
                lastRequestTime = currentTime;
            }

            // 초당 제한 체크
            if (currentTime - lastRequestTime < 1000) {
                if (requestsThisSecond.get() >= MAX_REQUESTS_PER_SECOND) {
                    return false;
                }
            } else {
                // 새로운 초 시작
                requestsThisSecond.set(0);
                lastRequestTime = currentTime;
            }

            // 분당 제한 체크
            if (requestCount.get() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }

            // 요청 허용
            requestCount.incrementAndGet();
            requestsThisSecond.incrementAndGet();
            return true;
        }

        public long getWindowStart() {
            return windowStart;
        }
    }
}
