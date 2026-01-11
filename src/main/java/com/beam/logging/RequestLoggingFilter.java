package com.beam.logging;

import com.beam.util.IpAddressExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 요청 로깅 필터
 * - 모든 요청에 대해 MDC 컨텍스트 설정
 * - 요청/응답 로깅
 * - 처리 시간 측정
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Autowired
    private IpAddressExtractor ipAddressExtractor;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String clientIp = ipAddressExtractor.extractClientIp(request);

        try (LogContext context = LogContext.forRequest()
                .withClientIp(clientIp)
                .withAction(request.getMethod() + " " + request.getRequestURI())) {

            // 요청 로깅 (상세 정보는 DEBUG 레벨)
            if (logger.isDebugEnabled()) {
                logger.debug("Request: {} {} from {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    clientIp);
            }

            filterChain.doFilter(request, response);

            // 응답 로깅
            long duration = System.currentTimeMillis() - startTime;
            logResponse(request, response, duration);
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, long duration) {
        int status = response.getStatus();
        String uri = request.getRequestURI();

        // 정적 리소스는 DEBUG 레벨로
        if (isStaticResource(uri)) {
            logger.debug("Response: {} {} - {} ({}ms)",
                request.getMethod(), uri, status, duration);
            return;
        }

        // 에러 응답은 WARN 레벨로
        if (status >= 400) {
            logger.warn("Response: {} {} - {} ({}ms)",
                request.getMethod(), uri, status, duration);
        } else if (logger.isDebugEnabled()) {
            logger.debug("Response: {} {} - {} ({}ms)",
                request.getMethod(), uri, status, duration);
        }

        // 느린 요청 경고 (1초 이상)
        if (duration > 1000) {
            logger.warn("Slow request detected: {} {} took {}ms",
                request.getMethod(), uri, duration);
        }
    }

    private boolean isStaticResource(String uri) {
        return uri.endsWith(".js") || uri.endsWith(".css") ||
               uri.endsWith(".png") || uri.endsWith(".jpg") ||
               uri.endsWith(".ico") || uri.endsWith(".woff") ||
               uri.endsWith(".woff2") || uri.endsWith(".ttf") ||
               uri.startsWith("/static/") || uri.startsWith("/assets/");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 헬스체크 등 제외
        return path.equals("/actuator/health") || path.equals("/health");
    }
}
