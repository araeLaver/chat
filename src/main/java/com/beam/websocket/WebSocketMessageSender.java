package com.beam.websocket;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.User;
import com.beam.service.MessageTranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 메시지 전송 담당
 */
@Component
public class WebSocketMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageSender.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private ChatRoomManager roomManager;

    @Autowired
    private MessageTranslationService messageTranslationService;

    public void sendToSession(WebSocketSession session, ChatMessage message) throws Exception {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    public void sendToSession(WebSocketSession session, String json) throws Exception {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(json));
        }
    }

    public void broadcastToRoom(String roomId, ChatMessage message) throws Exception {
        ChatRoom room = roomManager.getRoom(roomId);
        if (room == null) return;

        String messageJson = objectMapper.writeValueAsString(message);
        for (User user : room.getUsers().values()) {
            WebSocketSession userSession = sessionManager.findSessionById(user.getSessionId());
            if (userSession != null && userSession.isOpen()) {
                userSession.sendMessage(new TextMessage(messageJson));
            }
        }
    }

    /**
     * 번역 기능이 포함된 브로드캐스트
     * 각 수신자의 언어 설정에 따라 개인화된 번역 메시지 전송
     */
    public void broadcastToRoomWithTranslation(String roomId, ChatMessage message) throws Exception {
        ChatRoom room = roomManager.getRoom(roomId);
        if (room == null) return;

        // 번역 서비스가 비활성화된 경우 일반 브로드캐스트
        if (!messageTranslationService.isTranslationEnabled()) {
            broadcastToRoom(roomId, message);
            return;
        }

        // 발신자 언어 감지
        messageTranslationService.detectAndSetLanguage(message);

        for (User user : room.getUsers().values()) {
            WebSocketSession userSession = sessionManager.findSessionById(user.getSessionId());
            if (userSession != null && userSession.isOpen()) {
                try {
                    // 각 수신자에 맞게 번역
                    ChatMessage translatedMessage = messageTranslationService.translateForRecipient(
                        message, user.getUserId()
                    );
                    String messageJson = objectMapper.writeValueAsString(translatedMessage);
                    userSession.sendMessage(new TextMessage(messageJson));
                } catch (Exception e) {
                    // 번역 실패 시 원본 메시지 전송
                    logger.warn("Translation failed for user {}: {}", user.getUserId(), e.getMessage());
                    String originalJson = objectMapper.writeValueAsString(message);
                    userSession.sendMessage(new TextMessage(originalJson));
                }
            }
        }
    }

    /**
     * 특정 사용자에게 번역된 메시지 전송 (DM용)
     */
    public void sendToUserWithTranslation(Long userId, ChatMessage message) throws Exception {
        if (!messageTranslationService.isTranslationEnabled()) {
            sendToUser(userId, message);
            return;
        }

        WebSocketSession session = sessionManager.findSessionByUserId(userId);
        if (session != null && session.isOpen()) {
            messageTranslationService.detectAndSetLanguage(message);
            ChatMessage translatedMessage = messageTranslationService.translateForRecipient(message, userId);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(translatedMessage)));
        }
    }

    /**
     * 특정 사용자에게 메시지 전송
     */
    public void sendToUser(Long userId, ChatMessage message) throws Exception {
        WebSocketSession session = sessionManager.findSessionByUserId(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    public void broadcastRoomListToAll() throws Exception {
        for (WebSocketSession session : sessionManager.getAllSessions()) {
            if (session.isOpen()) {
                sendRoomList(session);
            }
        }
    }

    public void sendRoomList(WebSocketSession session) throws Exception {
        List<Map<String, Object>> roomDetails = new ArrayList<>();

        for (ChatRoom room : roomManager.getAllRooms()) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", room.getRoomId());
            roomInfo.put("roomName", room.getRoomName());
            roomInfo.put("roomType", room.getRoomType().toString());
            roomInfo.put("userCount", room.getUserCount());
            roomInfo.put("creator", room.getCreator());
            roomInfo.put("description", room.getDescription());
            roomInfo.put("isDirectMessage", room.isDirectMessage());
            roomDetails.add(roomInfo);
        }

        ChatMessage roomListMessage = new ChatMessage("시스템",
            objectMapper.writeValueAsString(roomDetails),
            LocalDateTime.now().format(TIME_FORMATTER),
            "roomlist");

        sendToSession(session, roomListMessage);
    }

    public void sendRoomUserList(String roomId) throws Exception {
        ChatRoom room = roomManager.getRoom(roomId);
        if (room == null) return;

        ChatMessage userListMessage = new ChatMessage("시스템",
            objectMapper.writeValueAsString(room.getUsers().values()),
            LocalDateTime.now().format(TIME_FORMATTER),
            "userlist");
        userListMessage.setRoomId(roomId);

        broadcastToRoom(roomId, userListMessage);
    }

    public void sendSystemMessage(String roomId, String content, String type) throws Exception {
        ChatMessage message = new ChatMessage("시스템", content,
            LocalDateTime.now().format(TIME_FORMATTER), type);
        message.setRoomId(roomId);
        broadcastToRoom(roomId, message);
    }

    public void sendErrorMessage(WebSocketSession session, String errorMessage) throws Exception {
        ChatMessage error = new ChatMessage("시스템", errorMessage,
            LocalDateTime.now().format(TIME_FORMATTER), "error");
        sendToSession(session, error);
    }

    public void sendSuccessMessage(WebSocketSession session, String successMessage) throws Exception {
        ChatMessage success = new ChatMessage("시스템", successMessage,
            LocalDateTime.now().format(TIME_FORMATTER), "success");
        sendToSession(session, success);
    }

    public void sendSuccessMessage(WebSocketSession session, String successMessage,
                                    String type, String roomId) throws Exception {
        ChatMessage success = new ChatMessage("시스템", successMessage,
            LocalDateTime.now().format(TIME_FORMATTER), type);
        success.setRoomId(roomId);
        sendToSession(session, success);
    }

    public String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
