package com.beam.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket Handshake Interceptor for token extraction
 * Handles Sec-WebSocket-Protocol header for authentication
 */
public class TokenHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // Extract token from Sec-WebSocket-Protocol header
        List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            String protocol = protocols.get(0);
            if (protocol.startsWith("access_token,")) {
                String token = protocol.substring("access_token,".length()).trim();
                attributes.put("token", token);

                // Accept the subprotocol by echoing it back
                response.getHeaders().set("Sec-WebSocket-Protocol", protocol);
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed after handshake
    }
}
