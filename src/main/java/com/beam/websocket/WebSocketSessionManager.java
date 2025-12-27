package com.beam.websocket;

import com.beam.User;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 세션 관리
 * 세션, 사용자, 세션-방 매핑을 관리
 */
@Component
public class WebSocketSessionManager {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        users.remove(session.getId());
        sessionToRoom.remove(session.getId());
    }

    public Set<WebSocketSession> getAllSessions() {
        return sessions;
    }

    public WebSocketSession findSessionById(String sessionId) {
        return sessions.stream()
            .filter(session -> session.getId().equals(sessionId))
            .findFirst()
            .orElse(null);
    }

    public void addUser(String sessionId, User user) {
        users.put(sessionId, user);
    }

    public User getUser(String sessionId) {
        return users.get(sessionId);
    }

    public User removeUser(String sessionId) {
        return users.remove(sessionId);
    }

    public void setSessionRoom(String sessionId, String roomId) {
        sessionToRoom.put(sessionId, roomId);
    }

    public String getSessionRoom(String sessionId) {
        return sessionToRoom.get(sessionId);
    }

    public void removeSessionRoom(String sessionId) {
        sessionToRoom.remove(sessionId);
    }
}
