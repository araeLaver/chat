package com.beam.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 데이터베이스 연결 상태 헬스 체크
 * - 연결 가능 여부
 * - 응답 시간 측정
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Value("${health.database.slow-query-threshold-ms:100}")
    private long slowQueryThresholdMs;

    @Value("${health.database.critical-query-threshold-ms:500}")
    private long criticalQueryThresholdMs;

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            long responseTime = System.currentTimeMillis() - startTime;

            Health.Builder builder;

            if (responseTime > criticalQueryThresholdMs) {
                builder = Health.down()
                    .withDetail("error", "Database response time critically slow");
            } else if (responseTime > slowQueryThresholdMs) {
                builder = Health.up()
                    .withDetail("warning", "Database response time slow");
            } else {
                builder = Health.up();
            }

            return builder
                .withDetail("responseTimeMs", responseTime)
                .withDetail("database", connection.getMetaData().getDatabaseProductName())
                .withDetail("databaseVersion", connection.getMetaData().getDatabaseProductVersion())
                .withDetail("driverName", connection.getMetaData().getDriverName())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Database connection failed")
                .withDetail("message", e.getMessage())
                .build();
        }
    }
}
