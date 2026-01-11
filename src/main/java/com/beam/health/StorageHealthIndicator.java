package com.beam.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.DecimalFormat;

/**
 * 파일 스토리지 헬스 체크
 * - 업로드 디렉토리 접근 가능 여부
 * - 디스크 용량 확인
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private static final double DISK_WARNING_THRESHOLD = 0.10; // 10% 남음
    private static final double DISK_CRITICAL_THRESHOLD = 0.05; // 5% 남음

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public Health health() {
        File storageDir = new File(uploadDir);

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                return Health.down()
                    .withDetail("error", "Cannot create upload directory")
                    .withDetail("path", uploadDir)
                    .build();
            }
        }

        if (!storageDir.canWrite()) {
            return Health.down()
                .withDetail("error", "Upload directory is not writable")
                .withDetail("path", storageDir.getAbsolutePath())
                .build();
        }

        long totalSpace = storageDir.getTotalSpace();
        long usableSpace = storageDir.getUsableSpace();
        double freeRatio = totalSpace > 0 ? (double) usableSpace / totalSpace : 0;

        Health.Builder builder;

        if (freeRatio < DISK_CRITICAL_THRESHOLD) {
            builder = Health.down()
                .withDetail("error", "Disk space critically low");
        } else if (freeRatio < DISK_WARNING_THRESHOLD) {
            builder = Health.up()
                .withDetail("warning", "Disk space running low");
        } else {
            builder = Health.up();
        }

        return builder
            .withDetail("path", storageDir.getAbsolutePath())
            .withDetail("totalSpace", formatBytes(totalSpace))
            .withDetail("usableSpace", formatBytes(usableSpace))
            .withDetail("freePercent", String.format("%.1f%%", freeRatio * 100))
            .withDetail("writable", true)
            .build();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
