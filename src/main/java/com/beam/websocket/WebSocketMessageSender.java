package com.beam.websocket;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.User;
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
