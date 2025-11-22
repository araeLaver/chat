package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final String allowedOrigins;

    @Autowired
    public WebSocketConfig(
            ChatWebSocketHandler chatWebSocketHandler,
            @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register handler for both native WebSocket and SockJS
        registry.addHandler(chatWebSocketHandler, "/ws")
                .setAllowedOrigins(allowedOrigins.split(","));

        // Keep /chat endpoint with SockJS for backward compatibility
        registry.addHandler(chatWebSocketHandler, "/chat")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();
    }
}