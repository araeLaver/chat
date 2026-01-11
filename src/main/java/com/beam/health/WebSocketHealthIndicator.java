package com.beam.health;

import com.beam.websocket.WebSocketSessionManager;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${health.websocket.max-sessions-warning:5000}")
    private int maxSessionsWarning;

    @Value("${health.websocket.max-sessions-critical:10000}")
    private int maxSessionsCritical;

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

        if (totalSessions >= maxSessionsCritical) {
            builder = Health.down()
                .withDetail("reason", "Session limit critical - approaching maximum capacity");
        } else if (totalSessions >= maxSessionsWarning) {
            builder = Health.up()
                .withDetail("warning", "High session count - monitor closely");
        } else {
            builder = Health.up();
        }

        return builder
            .withDetail("totalSessions", totalSessions)
            .withDetail("authenticatedUsers", authenticatedUsers)
            .withDetail("activeRooms", activeRooms)
            .withDetail("maxSessionsWarning", maxSessionsWarning)
            .withDetail("maxSessionsCritical", maxSessionsCritical)
            .build();
    }
}
