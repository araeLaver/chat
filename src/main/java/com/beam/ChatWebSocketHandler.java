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
        // 세션 유효성 먼저 체크 (Race Condition 방지)
        if (session == null || !session.isOpen()) {
            logger.debug("Ignoring message for closed or null session");
            return;
        }

        String sessionId = session.getId();

        try {
            // 세션 활동 시간 갱신
            sessionManager.updateActivity(sessionId);

            // Rate Limiting
            if (!rateLimitService.isWebSocketMessageAllowed(sessionId)) {
                sendRateLimitErrorSafely(session);
                return;
            }

            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));

            routeMessage(session, chatMessage);

            logger.debug("Message processed: {} - {}", chatMessage.getSender(), chatMessage.getContent());

        } catch (java.io.IOException e) {
            // JSON 파싱 오류 - 클라이언트에 에러 전송
            logger.warn("Invalid message format from session {}: {}", sessionId, e.getMessage());
            sendErrorMessageSafely(session, "Invalid message format");
        } catch (IllegalStateException e) {
            // 세션 상태 오류 (이미 닫힌 경우 등)
            logger.debug("Session {} is no longer valid: {}", sessionId, e.getMessage());
        } catch (Exception e) {
            logger.error("Message processing error for session {}: {}", sessionId, e.getMessage(), e);
            sendErrorMessageSafely(session, "Message processing failed");
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
            case "pong":
                // Heartbeat pong 응답 처리
                sessionManager.handlePong(session.getId());
                break;
            case "ping":
                // 클라이언트에서 보낸 ping에 pong 응답
                handleClientPing(session);
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

    /**
     * 안전하게 Rate Limit 에러 전송 (세션 상태 체크 포함)
     */
    private void sendRateLimitErrorSafely(WebSocketSession session) {
        sendErrorMessageSafely(session, "Rate limit exceeded. Please slow down.");
        logger.warn("Rate limit exceeded for session: {}", session.getId());
    }

    /**
     * 안전하게 에러 메시지 전송 (Race Condition 방지)
     */
    private void sendErrorMessageSafely(WebSocketSession session, String errorContent) {
        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            ChatMessage errorMessage = new ChatMessage();
            errorMessage.setType("error");
            errorMessage.setContent(errorContent);
            errorMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));

            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to send error message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * 클라이언트 ping 메시지에 대한 pong 응답
     */
    private void handleClientPing(WebSocketSession session) throws Exception {
        ChatMessage pongMessage = new ChatMessage();
        pongMessage.setType("pong");
        pongMessage.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongMessage)));
        logger.debug("Sent pong response to session: {}", session.getId());
    }
}
