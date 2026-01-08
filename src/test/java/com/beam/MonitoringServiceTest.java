package com.beam;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringService Tests")
class MonitoringServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private GroupMessageRepository groupMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private FriendRepository friendRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        monitoringService = new MonitoringService(registry);
        ReflectionTestUtils.setField(monitoringService, "directMessageRepository", directMessageRepository);
        ReflectionTestUtils.setField(monitoringService, "groupMessageRepository", groupMessageRepository);
        ReflectionTestUtils.setField(monitoringService, "userRepository", userRepository);
        ReflectionTestUtils.setField(monitoringService, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(monitoringService, "friendRepository", friendRepository);
    }

    @Nested
    @DisplayName("updateMetrics Tests")
    class UpdateMetricsTests {

        @Test
        @DisplayName("Should update all metrics successfully")
        void shouldUpdateAllMetricsSuccessfully() {
            when(directMessageRepository.count()).thenReturn(100L);
            when(groupMessageRepository.count()).thenReturn(200L);
            when(userRepository.countOnlineUsers()).thenReturn(50L);
            when(roomRepository.count()).thenReturn(10L);
            when(friendRepository.countAllPendingRequests()).thenReturn(5L);

            monitoringService.updateMetrics();

            assertEquals(300L, monitoringService.getTotalMessages());
            assertEquals(50L, monitoringService.getActiveUsers());
            assertEquals(10L, monitoringService.getActiveRooms());
        }

        @Test
        @DisplayName("Should handle null repositories gracefully")
        void shouldHandleNullRepositoriesGracefully() {
            ReflectionTestUtils.setField(monitoringService, "directMessageRepository", null);
            ReflectionTestUtils.setField(monitoringService, "groupMessageRepository", null);
            ReflectionTestUtils.setField(monitoringService, "userRepository", null);
            ReflectionTestUtils.setField(monitoringService, "roomRepository", null);
            ReflectionTestUtils.setField(monitoringService, "friendRepository", null);

            assertDoesNotThrow(() -> monitoringService.updateMetrics());
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void shouldHandleExceptionGracefully() {
            when(directMessageRepository.count()).thenThrow(new RuntimeException("Database error"));

            assertDoesNotThrow(() -> monitoringService.updateMetrics());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return initial values as zero")
        void shouldReturnInitialValuesAsZero() {
            assertEquals(0L, monitoringService.getTotalMessages());
            assertEquals(0L, monitoringService.getActiveUsers());
            assertEquals(0L, monitoringService.getActiveRooms());
        }
    }

    @Nested
    @DisplayName("Counter Tests")
    class CounterTests {

        @Test
        @DisplayName("Should record message sent")
        void shouldRecordMessageSent() {
            assertDoesNotThrow(() -> monitoringService.recordMessageSent());
        }

        @Test
        @DisplayName("Should record login")
        void shouldRecordLogin() {
            assertDoesNotThrow(() -> monitoringService.recordLogin());
        }
    }
}
