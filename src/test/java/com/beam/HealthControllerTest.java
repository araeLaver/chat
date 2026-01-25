package com.beam;

import com.beam.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("HealthController Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringService monitoringService;

    @MockBean
    private WebSocketSessionManager sessionManager;

    @Nested
    @DisplayName("GET /api/health Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("BEAM Messenger"))
                    .andExpect(jsonPath("$.version").value("1.0.0"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.message").value("대한민국 대표 메신저 BEAM 정상 작동 중"));
        }
    }

    @Nested
    @DisplayName("GET /api/health/metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should return metrics")
        void shouldReturnMetrics() throws Exception {
            when(monitoringService.getTotalMessages()).thenReturn(100L);
            when(monitoringService.getActiveUsers()).thenReturn(25L);
            when(monitoringService.getActiveRooms()).thenReturn(10L);

            mockMvc.perform(get("/api/v1/health/metrics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalMessages").value(100))
                    .andExpect(jsonPath("$.activeUsers").value(25))
                    .andExpect(jsonPath("$.activeRooms").value(10))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return zero metrics when no activity")
        void shouldReturnZeroMetricsWhenNoActivity() throws Exception {
            when(monitoringService.getTotalMessages()).thenReturn(0L);
            when(monitoringService.getActiveUsers()).thenReturn(0L);
            when(monitoringService.getActiveRooms()).thenReturn(0L);

            mockMvc.perform(get("/api/v1/health/metrics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalMessages").value(0))
                    .andExpect(jsonPath("$.activeUsers").value(0))
                    .andExpect(jsonPath("$.activeRooms").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/health/status Tests")
    class ServiceStatusTests {

        @Test
        @DisplayName("Should return service status")
        void shouldReturnServiceStatus() throws Exception {
            when(sessionManager.getStats())
                .thenReturn(new WebSocketSessionManager.SessionStats(5, 3, 2));

            mockMvc.perform(get("/api/v1/health/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.database.status").exists())
                    .andExpect(jsonPath("$.websocket.status").value("HEALTHY"))
                    .andExpect(jsonPath("$.websocket.activeSessions").value(5))
                    .andExpect(jsonPath("$.fileStorage.status").exists())
                    .andExpect(jsonPath("$.overall").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/health/ready Tests")
    class ReadinessTests {

        @Test
        @DisplayName("Should return ready status")
        void shouldReturnReadyStatus() throws Exception {
            mockMvc.perform(get("/api/v1/health/ready"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("READY"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/health/live Tests")
    class LivenessTests {

        @Test
        @DisplayName("Should return alive status")
        void shouldReturnAliveStatus() throws Exception {
            mockMvc.perform(get("/api/v1/health/live"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ALIVE"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
