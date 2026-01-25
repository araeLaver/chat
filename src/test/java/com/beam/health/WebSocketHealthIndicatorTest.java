package com.beam.health;

import com.beam.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketHealthIndicator Tests")
class WebSocketHealthIndicatorTest {

    private WebSocketHealthIndicator healthIndicator;

    @Mock
    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        healthIndicator = new WebSocketHealthIndicator(sessionManager);
        ReflectionTestUtils.setField(healthIndicator, "maxSessionsWarning", 5000);
        ReflectionTestUtils.setField(healthIndicator, "maxSessionsCritical", 10000);
    }

    @Nested
    @DisplayName("health() Tests")
    class HealthTests {

        @Test
        @DisplayName("Should return UP when sessions are normal")
        void shouldReturnUpWhenSessionsAreNormal() {
            when(sessionManager.getStats()).thenReturn(
                    new WebSocketSessionManager.SessionStats(100, 50, 10));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails().get("totalSessions")).isEqualTo(100);
            assertThat(health.getDetails().get("authenticatedUsers")).isEqualTo(50);
            assertThat(health.getDetails().get("activeRooms")).isEqualTo(10);
        }

        @Test
        @DisplayName("Should return UP with warning when sessions are high")
        void shouldReturnUpWithWarningWhenSessionsAreHigh() {
            when(sessionManager.getStats()).thenReturn(
                    new WebSocketSessionManager.SessionStats(6000, 3000, 100));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails().get("warning")).isEqualTo("High session count - monitor closely");
            assertThat(health.getDetails().get("totalSessions")).isEqualTo(6000);
        }

        @Test
        @DisplayName("Should return DOWN when sessions are critical")
        void shouldReturnDownWhenSessionsAreCritical() {
            when(sessionManager.getStats()).thenReturn(
                    new WebSocketSessionManager.SessionStats(10000, 5000, 200));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails().get("reason"))
                    .isEqualTo("Session limit critical - approaching maximum capacity");
            assertThat(health.getDetails().get("totalSessions")).isEqualTo(10000);
        }

        @Test
        @DisplayName("Should include threshold configuration in health details")
        void shouldIncludeThresholdConfigurationInHealthDetails() {
            when(sessionManager.getStats()).thenReturn(
                    new WebSocketSessionManager.SessionStats(100, 50, 10));

            Health health = healthIndicator.health();

            assertThat(health.getDetails().get("maxSessionsWarning")).isEqualTo(5000);
            assertThat(health.getDetails().get("maxSessionsCritical")).isEqualTo(10000);
        }

        @Test
        @DisplayName("Should return UP with zero sessions")
        void shouldReturnUpWithZeroSessions() {
            when(sessionManager.getStats()).thenReturn(
                    new WebSocketSessionManager.SessionStats(0, 0, 0));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails().get("totalSessions")).isEqualTo(0);
        }
    }
}
