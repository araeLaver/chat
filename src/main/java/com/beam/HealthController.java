package com.beam;

import com.beam.websocket.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "서비스 상태 확인 API")
public class HealthController {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private DataSource dataSource;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping
    @Operation(summary = "기본 헬스 체크", description = "서비스 기본 상태 확인")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "BEAM Messenger");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("message", "대한민국 대표 메신저 BEAM 정상 작동 중");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics")
    @Operation(summary = "비즈니스 메트릭", description = "메시지, 사용자, 채팅방 통계")
    public ResponseEntity<?> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalMessages", monitoringService.getTotalMessages());
        metrics.put("activeUsers", monitoringService.getActiveUsers());
        metrics.put("activeRooms", monitoringService.getActiveRooms());
        metrics.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/status")
    @Operation(summary = "상세 서비스 상태", description = "데이터베이스, WebSocket, 스토리지 등 실시간 상태 확인")
    public ResponseEntity<?> getServiceStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Database 상태 확인
        status.put("database", checkDatabaseStatus());

        // WebSocket 상태 확인
        status.put("websocket", checkWebSocketStatus());

        // 파일 스토리지 상태 확인
        status.put("fileStorage", checkStorageStatus());

        // 전체 상태 판단
        boolean allHealthy = status.values().stream()
            .allMatch(v -> v instanceof Map && "HEALTHY".equals(((Map<?, ?>) v).get("status")));

        status.put("overall", allHealthy ? "HEALTHY" : "DEGRADED");
        status.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(status);
    }

    @GetMapping("/ready")
    @Operation(summary = "서비스 준비 상태", description = "Kubernetes readiness probe용")
    public ResponseEntity<?> readinessCheck() {
        try {
            // DB 연결 확인
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) {
                    return ResponseEntity.status(503).body(Map.of(
                        "status", "NOT_READY",
                        "reason", "Database not available"
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                "status", "READY",
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "NOT_READY",
                "reason", e.getMessage()
            ));
        }
    }

    @GetMapping("/live")
    @Operation(summary = "서비스 생존 상태", description = "Kubernetes liveness probe용")
    public ResponseEntity<?> livenessCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "ALIVE",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    private Map<String, Object> checkDatabaseStatus() {
        Map<String, Object> dbStatus = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            long responseTime = System.currentTimeMillis() - startTime;
            dbStatus.put("status", "HEALTHY");
            dbStatus.put("responseTimeMs", responseTime);
            dbStatus.put("database", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            dbStatus.put("status", "UNHEALTHY");
            dbStatus.put("error", e.getMessage());
        }

        return dbStatus;
    }

    private Map<String, Object> checkWebSocketStatus() {
        Map<String, Object> wsStatus = new LinkedHashMap<>();
        WebSocketSessionManager.SessionStats stats = sessionManager.getStats();

        wsStatus.put("status", "HEALTHY");
        wsStatus.put("activeSessions", stats.totalSessions());
        wsStatus.put("authenticatedUsers", stats.authenticatedUsers());
        wsStatus.put("activeRooms", stats.activeRooms());

        return wsStatus;
    }

    private Map<String, Object> checkStorageStatus() {
        Map<String, Object> storageStatus = new LinkedHashMap<>();
        File storage = new File(uploadDir);

        if (!storage.exists()) {
            storage.mkdirs();
        }

        if (storage.canWrite()) {
            long totalSpace = storage.getTotalSpace();
            long freeSpace = storage.getFreeSpace();
            double freePercent = totalSpace > 0 ? (freeSpace * 100.0 / totalSpace) : 0;

            storageStatus.put("status", freePercent > 5 ? "HEALTHY" : "WARNING");
            storageStatus.put("freePercent", String.format("%.1f%%", freePercent));
            storageStatus.put("writable", true);
        } else {
            storageStatus.put("status", "UNHEALTHY");
            storageStatus.put("writable", false);
        }

        return storageStatus;
    }
}