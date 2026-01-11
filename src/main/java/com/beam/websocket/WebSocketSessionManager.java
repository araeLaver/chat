package com.beam.websocket;

import com.beam.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 세션 관리
 * 세션, 사용자, 세션-방 매핑을 관리
 * 세션 활동 추적 및 유휴 타임아웃 지원
 * Heartbeat/Ping-Pong 메커니즘 지원
 */
@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, Instant> sessionLastActivity = new ConcurrentHashMap<>();

    // Heartbeat 관련 필드
    private final Map<String, Instant> lastPongReceived = new ConcurrentHashMap<>();
    private final Map<String, Integer> missedPongCount = new ConcurrentHashMap<>();

    @Value("${websocket.session.idle-timeout-minutes:30}")
    private int idleTimeoutMinutes;

    @Value("${websocket.heartbeat.interval-seconds:30}")
    private int heartbeatIntervalSeconds;

    @Value("${websocket.heartbeat.max-missed-pongs:3}")
    private int maxMissedPongs;

    public void addSession(WebSocketSession session) {
        sessions.add(session);
        updateActivity(session.getId());
        lastPongReceived.put(session.getId(), Instant.now());
        missedPongCount.put(session.getId(), 0);
        logger.debug("Session added: {} (total: {})", session.getId(), sessions.size());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        users.remove(session.getId());
        sessionToRoom.remove(session.getId());
        sessionLastActivity.remove(session.getId());
        lastPongReceived.remove(session.getId());
        missedPongCount.remove(session.getId());
        logger.debug("Session removed: {} (total: {})", session.getId(), sessions.size());
    }

    /**
     * 세션 활동 시간 갱신
     */
    public void updateActivity(String sessionId) {
        sessionLastActivity.put(sessionId, Instant.now());
    }

    /**
     * 유휴 세션 정리 (30분마다 실행)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 체크
    public void cleanupIdleSessions() {
        Instant cutoff = Instant.now().minusSeconds(idleTimeoutMinutes * 60L);
        List<WebSocketSession> idleSessions = new ArrayList<>();

        for (WebSocketSession session : sessions) {
            Instant lastActivity = sessionLastActivity.get(session.getId());
            if (lastActivity != null && lastActivity.isBefore(cutoff)) {
                idleSessions.add(session);
            }
        }

        for (WebSocketSession session : idleSessions) {
            try {
                logger.info("Closing idle session: {} (inactive for {} minutes)",
                    session.getId(), idleTimeoutMinutes);
                session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Session idle timeout"));
                removeSession(session);
            } catch (IOException e) {
                logger.warn("Error closing idle session: {}", session.getId(), e);
            }
        }

        if (!idleSessions.isEmpty()) {
            logger.info("Cleaned up {} idle sessions", idleSessions.size());
        }
    }

    /**
     * Heartbeat ping 전송 (30초마다 실행)
     * 모든 활성 세션에 ping 메시지를 전송하고 응답하지 않는 세션을 정리
     */
    @Scheduled(fixedRateString = "${websocket.heartbeat.interval-seconds:30}000")
    public void sendHeartbeat() {
        if (sessions.isEmpty()) {
            return;
        }

        logger.debug("Sending heartbeat ping to {} sessions", sessions.size());
        List<WebSocketSession> deadSessions = new ArrayList<>();

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                deadSessions.add(session);
                continue;
            }

            String sessionId = session.getId();
            int missed = missedPongCount.getOrDefault(sessionId, 0);

            // 최대 놓친 pong 초과 시 연결 종료
            if (missed >= maxMissedPongs) {
                logger.warn("Session {} missed {} pongs, closing connection", sessionId, missed);
                deadSessions.add(session);
                continue;
            }

            try {
                // ping 메시지 전송 (두 가지 방식: WebSocket ping frame + 애플리케이션 레벨 ping)
                // 1. WebSocket ping frame (브라우저가 자동으로 pong 응답)
                session.sendMessage(new PingMessage(ByteBuffer.wrap(("ping-" + System.currentTimeMillis()).getBytes())));

                // 2. 애플리케이션 레벨 ping (클라이언트 코드에서 처리 가능)
                Map<String, Object> pingMessage = new HashMap<>();
                pingMessage.put("type", "ping");
                pingMessage.put("timestamp", Instant.now().toEpochMilli());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pingMessage)));

                // 놓친 pong 카운트 증가
                missedPongCount.put(sessionId, missed + 1);

            } catch (IOException e) {
                logger.warn("Failed to send heartbeat to session {}: {}", sessionId, e.getMessage());
                deadSessions.add(session);
            }
        }

        // 죽은 세션 정리
        for (WebSocketSession session : deadSessions) {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Heartbeat timeout"));
                }
                removeSession(session);
            } catch (IOException e) {
                logger.warn("Error closing dead session: {}", session.getId(), e);
            }
        }

        if (!deadSessions.isEmpty()) {
            logger.info("Removed {} dead sessions via heartbeat", deadSessions.size());
        }
    }

    /**
     * Pong 응답 처리 (클라이언트로부터 pong 수신 시 호출)
     */
    public void handlePong(String sessionId) {
        lastPongReceived.put(sessionId, Instant.now());
        missedPongCount.put(sessionId, 0);
        logger.debug("Pong received from session: {}", sessionId);
    }

    /**
     * Heartbeat 상태 조회
     */
    public HeartbeatStatus getHeartbeatStatus(String sessionId) {
        Instant lastPong = lastPongReceived.get(sessionId);
        Integer missed = missedPongCount.get(sessionId);
        return new HeartbeatStatus(
            lastPong != null ? lastPong.toEpochMilli() : 0,
            missed != null ? missed : 0,
            maxMissedPongs
        );
    }

    public record HeartbeatStatus(long lastPongTimestamp, int missedPongs, int maxAllowedMissedPongs) {}

    /**
     * 세션 통계 조회
     */
    public SessionStats getStats() {
        return new SessionStats(
            sessions.size(),
            users.size(),
            sessionToRoom.size()
        );
    }

    public record SessionStats(int totalSessions, int authenticatedUsers, int activeRooms) {}

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
