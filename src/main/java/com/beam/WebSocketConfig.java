package com.beam;

import com.beam.websocket.TokenHandshakeInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

/**
 * WebSocket Configuration for BEAM Messenger
 *
 * <p>Configures WebSocket endpoint for real-time chat communication.
 * Uses basic WebSocket protocol (not STOMP) for lightweight, direct messaging.
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li><b>/chat</b> - Main WebSocket endpoint for chat messages</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <ul>
 *   <li>CORS: Restricted to domains specified in application.properties</li>
 *   <li>Authentication: JWT token validation in ChatWebSocketHandler</li>
 *   <li>Guest mode: Supported with 'guest' token</li>
 * </ul>
 *
 * <h3>Client Connection:</h3>
 * <pre>
 * // With JWT authentication
 * const ws = new WebSocket('ws://localhost:8080/chat?token=' + jwtToken);
 *
 * // Guest mode
 * const ws = new WebSocket('ws://localhost:8080/chat?token=guest');
 * </pre>
 *
 * @see ChatWebSocketHandler
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final String allowedOriginsConfig;
    private String[] allowedOriginPatterns;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Autowired
    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.allowedOriginsConfig = allowedOrigins;
    }

    @PostConstruct
    public void init() {
        // Parse allowed origins from config
        this.allowedOriginPatterns = Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // 개발 환경에서만 와일드카드 허용
        if (isDevEnvironment() && allowedOriginPatterns.length == 0) {
            logger.warn("⚠️ WebSocket CORS: No origins configured, using wildcard for development");
            allowedOriginPatterns = new String[]{"*"};
        }

        logger.info("✅ WebSocket CORS configured with origins: {}", Arrays.toString(allowedOriginPatterns));

        // 프로덕션에서 와일드카드 사용 시 경고
        if (!isDevEnvironment() && Arrays.asList(allowedOriginPatterns).contains("*")) {
            logger.warn("⚠️ SECURITY WARNING: WebSocket CORS allows all origins in production. " +
                "Configure specific origins via CORS_ALLOWED_ORIGINS environment variable.");
        }
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register handler for native WebSocket with token interceptor
        registry.addHandler(chatWebSocketHandler, "/ws")
                .addInterceptors(new TokenHandshakeInterceptor())
                .setAllowedOriginPatterns(allowedOriginPatterns);

        // Keep /chat endpoint with SockJS for backward compatibility
        registry.addHandler(chatWebSocketHandler, "/chat")
                .addInterceptors(new TokenHandshakeInterceptor())
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .withSockJS();

        logger.info("WebSocket handlers registered: /ws (native), /chat (SockJS)");
    }

    private boolean isDevEnvironment() {
        return "dev".equalsIgnoreCase(activeProfile) ||
               "local".equalsIgnoreCase(activeProfile) ||
               "development".equalsIgnoreCase(activeProfile);
    }
}