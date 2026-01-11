package com.beam;

import com.beam.util.IpAddressExtractor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP Request Rate Limiting Interceptor
 *
 * <p>Intercepts all API requests and applies rate limiting based on client IP address.
 * Returns 429 (Too Many Requests) when rate limit is exceeded.
 *
 * <h3>Rate Limit Headers:</h3>
 * <ul>
 *   <li>X-RateLimit-Limit: Maximum requests allowed</li>
 *   <li>X-RateLimit-Remaining: Remaining requests in current window</li>
 *   <li>X-RateLimit-Reset: Time window for rate limit reset</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private IpAddressExtractor ipAddressExtractor;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip rate limiting for health checks and actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/") || path.equals("/health")) {
            return true;
        }

        // Get client identifier (IP address)
        String clientId = ipAddressExtractor.extractClientIp(request);

        // Check rate limit
        if (!rateLimitService.isApiRequestAllowed(clientId)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );

            // Add rate limit headers
            response.setHeader("X-RateLimit-Retry-After", "60");
            return false;
        }

        // Add rate limit info headers
        long remaining = rateLimitService.getApiRemainingTokens(clientId);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        return true;
    }
}
