package com.beam.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StorageHealthIndicator Tests")
class StorageHealthIndicatorTest {

    private StorageHealthIndicator healthIndicator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        healthIndicator = new StorageHealthIndicator();
    }

    @Nested
    @DisplayName("health() Tests")
    class HealthTests {

        @Test
        @DisplayName("Should return UP when directory exists and is writable")
        void shouldReturnUpWhenDirectoryExistsAndWritable() {
            ReflectionTestUtils.setField(healthIndicator, "uploadDir", tempDir.toString());

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("totalSpace");
            assertThat(health.getDetails()).containsKey("usableSpace");
            assertThat(health.getDetails()).containsKey("freePercent");
            assertThat(health.getDetails().get("writable")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should create directory if not exists")
        void shouldCreateDirectoryIfNotExists() {
            String newDir = tempDir.resolve("new-upload-dir").toString();
            ReflectionTestUtils.setField(healthIndicator, "uploadDir", newDir);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(new File(newDir).exists()).isTrue();
        }

        @Test
        @DisplayName("Should include path in health details")
        void shouldIncludePathInHealthDetails() {
            ReflectionTestUtils.setField(healthIndicator, "uploadDir", tempDir.toString());

            Health health = healthIndicator.health();

            assertThat(health.getDetails().get("path")).isNotNull();
        }

        @Test
        @DisplayName("Should format disk space correctly")
        void shouldFormatDiskSpaceCorrectly() {
            ReflectionTestUtils.setField(healthIndicator, "uploadDir", tempDir.toString());

            Health health = healthIndicator.health();

            String totalSpace = (String) health.getDetails().get("totalSpace");
            String usableSpace = (String) health.getDetails().get("usableSpace");

            // Should contain unit (B, KB, MB, GB, TB)
            assertThat(totalSpace).matches(".*[BKMGT]B?$");
            assertThat(usableSpace).matches(".*[BKMGT]B?$");
        }

        @Test
        @DisplayName("Should include free percent in health details")
        void shouldIncludeFreePercent() {
            ReflectionTestUtils.setField(healthIndicator, "uploadDir", tempDir.toString());

            Health health = healthIndicator.health();

            String freePercent = (String) health.getDetails().get("freePercent");
            assertThat(freePercent).matches("\\d+\\.\\d+%");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle default upload directory")
        void shouldHandleDefaultUploadDirectory() {
            // Default is "uploads" - may or may not exist
            ReflectionTestUtils.setField(healthIndicator, "uploadDir", "uploads");

            Health health = healthIndicator.health();

            // Should either create it or report status
            assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
        }
    }
}
