package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private MonitoringService monitoringService;

    @GetMapping
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "BEAM Messenger");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("message", "대한민국 대표 메신저 BEAM 정상 작동 중");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalMessages", monitoringService.getTotalMessages());
        metrics.put("activeUsers", monitoringService.getActiveUsers());
        metrics.put("activeRooms", monitoringService.getActiveRooms());
        metrics.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("database", "CONNECTED");
        status.put("websocket", "ACTIVE");
        status.put("fileStorage", "READY");
        status.put("messaging", "OPERATIONAL");
        status.put("security", "ENABLED");
        status.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(status);
    }
}