package com.beam.health;

import com.beam.websocket.WebSocketSessionManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * WebSocket 연결 상태 헬스 체크
 * - 활성 세션 수 모니터링
 * - 인증된 사용자 수 확인
 */
@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    private static final int MAX_SESSIONS_WARNING = 5000;
    private static final int MAX_SESSIONS_CRITICAL = 10000;

    private final WebSocketSessionManager sessionManager;

    public WebSocketHealthIndicator(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Health health() {
        WebSocketSessionManager.SessionStats stats = sessionManager.getStats();
        int totalSessions = stats.totalSessions();
        int authenticatedUsers = stats.authenticatedUsers();
        int activeRooms = stats.activeRooms();

        Health.Builder builder;

        if (totalSessions >= MAX_SESSIONS_CRITICAL) {
            builder = Health.down()
                .withDetail("reason", "Session limit critical - approaching maximum capacity");
        } else if (totalSessions >= MAX_SESSIONS_WARNING) {
            builder = Health.up()
                .withDetail("warning", "High session count - monitor closely");
        } else {
            builder = Health.up();
        }

        return builder
            .withDetail("totalSessions", totalSessions)
            .withDetail("authenticatedUsers", authenticatedUsers)
            .withDetail("activeRooms", activeRooms)
            .withDetail("maxSessionsWarning", MAX_SESSIONS_WARNING)
            .withDetail("maxSessionsCritical", MAX_SESSIONS_CRITICAL)
            .build();
    }
}
