package com.beam;

import com.beam.websocket.ChatRoomManager;
import com.beam.websocket.WebSocketMessageSender;
import com.beam.websocket.WebSocketSessionManager;
import com.beam.websocket.handler.ChatMessageHandler;
import com.beam.websocket.handler.RoomMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * WebSocket 메시지 라우터
 * 메시지 타입에 따라 적절한 핸들러에 위임
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private ChatRoomManager roomManager;

    @Autowired
    private WebSocketMessageSender messageSender;

    @Autowired
    private RoomMessageHandler roomHandler;

    @Autowired
    private ChatMessageHandler chatHandler;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitService rateLimitService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractTokenFromSession(session);

        // 게스트 모드 지원: 토큰이 없거나 "guest"인 경우 허용
        if (token != null && !"guest".equals(token)) {
            if (!jwtUtil.validateToken(token)) {
                logger.warn("Invalid JWT token for session: {}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
                return;
            }
            String username = jwtUtil.getUsernameFromToken(token);
            session.getAttributes().put("username", username);
            session.getAttributes().put("userId", jwtUtil.getUserIdFromToken(token));
            logger.info("Authenticated user connected: {}", username);
        } else {
            logger.debug("Guest user connected: {}", session.getId());
        }

        sessionManager.addSession(session);
        messageSender.sendRoomList(session);
        logger.debug("New WebSocket connection: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // Rate Limiting
            if (!rateLimitService.isWebSocketMessageAllowed(session.getId())) {
                sendRateLimitError(session);
                return;
            }

            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));

            routeMessage(session, chatMessage);

            logger.debug("Message processed: {} - {}", chatMessage.getSender(), chatMessage.getContent());

        } catch (Exception e) {
            logger.error("Message processing error: {}", e.getMessage(), e);
        }
    }

    private void routeMessage(WebSocketSession session, ChatMessage message) throws Exception {
        String type = message.getType();

        switch (type != null ? type : "message") {
            case "joinRoom":
                roomHandler.handleJoinRoom(session, message);
                break;
            case "createRoom":
                roomHandler.handleCreateRoom(session, message);
                break;
            case "createDirectMessage":
                roomHandler.handleCreateDirectMessage(session, message);
                break;
            case "deleteRoom":
                roomHandler.handleDeleteRoom(session, message);
                break;
            case "message":
                chatHandler.handleTextMessage(session, message);
                break;
            case "file":
                chatHandler.handleFileMessage(session, message);
                break;
            case "getHistory":
                chatHandler.handleGetHistory(session, message);
                break;
            case "markAsRead":
                chatHandler.handleMarkAsRead(session, message);
                break;
            default:
                // 기본적으로 텍스트 메시지로 처리
                chatHandler.handleTextMessage(session, message);
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        roomHandler.leaveCurrentRoom(session);
        sessionManager.removeSession(session);
        rateLimitService.removeWebSocketLimiter(session.getId());
        logger.debug("WebSocket connection closed: {}", session.getId());
    }

    private String extractTokenFromSession(WebSocketSession session) {
        // 1. Authorization 헤더에서 추출 시도
        List<String> authHeaders = session.getHandshakeHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }

        // 2. Sec-WebSocket-Protocol 헤더에서 추출 시도
        List<String> protocols = session.getHandshakeHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            String protocol = protocols.get(0);
            if (protocol.startsWith("access_token,")) {
                return protocol.substring("access_token,".length()).trim();
            }
            return protocol;
        }

        // 3. 세션 속성에서 추출 시도
        Object tokenAttr = session.getAttributes().get("token");
        if (tokenAttr != null) {
            return tokenAttr.toString();
        }

        return null;
    }

    private void sendRateLimitError(WebSocketSession session) throws Exception {
        ChatMessage errorMessage = new ChatMessage();
        errorMessage.setType("error");
        errorMessage.setContent("Rate limit exceeded. Please slow down.");
        errorMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
        logger.warn("Rate limit exceeded for session: {}", session.getId());
    }
}
