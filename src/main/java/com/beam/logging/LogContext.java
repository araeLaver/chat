package com.beam.logging;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC 기반 구조화된 로깅 컨텍스트
 * - 요청 추적을 위한 상관관계 ID
 * - 사용자/세션 정보
 * - 자동 정리를 위한 try-with-resources 지원
 */
public class LogContext implements AutoCloseable {

    // MDC 키 상수
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";
    public static final String SESSION_ID = "sessionId";
    public static final String ROOM_ID = "roomId";
    public static final String ACTION = "action";
    public static final String CLIENT_IP = "clientIp";

    private LogContext() {
        // Private constructor - use static methods
    }

    /**
     * 요청 ID 생성 및 설정
     */
    public static LogContext forRequest() {
        LogContext context = new LogContext();
        MDC.put(REQUEST_ID, generateRequestId());
        return context;
    }

    /**
     * 사용자 컨텍스트 설정
     */
    public static LogContext forUser(Long userId, String username) {
        LogContext context = new LogContext();
        MDC.put(REQUEST_ID, generateRequestId());
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
        if (username != null) {
            MDC.put(USERNAME, username);
        }
        return context;
    }

    /**
     * WebSocket 세션 컨텍스트 설정
     */
    public static LogContext forWebSocket(String sessionId, Long userId, String username) {
        LogContext context = new LogContext();
        MDC.put(REQUEST_ID, generateRequestId());
        if (sessionId != null) {
            MDC.put(SESSION_ID, sessionId);
        }
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
        if (username != null) {
            MDC.put(USERNAME, username);
        }
        return context;
    }

    /**
     * 채팅방 컨텍스트 추가
     */
    public LogContext withRoom(Long roomId) {
        if (roomId != null) {
            MDC.put(ROOM_ID, roomId.toString());
        }
        return this;
    }

    /**
     * 액션 컨텍스트 추가
     */
    public LogContext withAction(String action) {
        if (action != null) {
            MDC.put(ACTION, action);
        }
        return this;
    }

    /**
     * 클라이언트 IP 추가
     */
    public LogContext withClientIp(String ip) {
        if (ip != null) {
            MDC.put(CLIENT_IP, ip);
        }
        return this;
    }

    /**
     * 커스텀 컨텍스트 추가
     */
    public LogContext with(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
        return this;
    }

    /**
     * 현재 요청 ID 조회
     */
    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /**
     * 현재 사용자 ID 조회
     */
    public static Long getCurrentUserId() {
        String userId = MDC.get(USER_ID);
        return userId != null ? Long.parseLong(userId) : null;
    }

    /**
     * 요청 ID 생성
     */
    private static String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * MDC 컨텍스트 정리
     */
    public static void clear() {
        MDC.clear();
    }

    @Override
    public void close() {
        clear();
    }
}
