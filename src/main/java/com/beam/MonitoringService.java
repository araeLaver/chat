package com.beam;

import com.beam.websocket.WebSocketSessionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitoring Service
 *
 * <p>Provides business metrics for Prometheus monitoring:
 * <ul>
 *   <li>beam_total_messages - Total messages sent</li>
 *   <li>beam_active_users - Currently online users</li>
 *   <li>beam_active_rooms - Active chat rooms</li>
 *   <li>beam_friend_requests - Friend requests sent</li>
 *   <li>beam_websocket_sessions - Active WebSocket connections</li>
 *   <li>beam_files_uploaded_total - Total files uploaded</li>
 *   <li>beam_file_upload_size_bytes - Total file upload size</li>
 *   <li>beam_rate_limit_hits_total - Rate limit violations</li>
 *   <li>beam_auth_errors_total - Authentication errors</li>
 *   <li>beam_api_response_time - API response time histogram</li>
 * </ul>
 *
 * <p>Metrics endpoint: /actuator/prometheus
 *
 * @since 1.1.0
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired(required = false)
    private DirectMessageRepository directMessageRepository;

    @Autowired(required = false)
    private GroupMessageRepository groupMessageRepository;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private RoomRepository roomRepository;

    @Autowired(required = false)
    private FriendRepository friendRepository;

    @Autowired(required = false)
    private FileMetadataRepository fileMetadataRepository;

    @Autowired(required = false)
    private WebSocketSessionManager sessionManager;

    private final MeterRegistry meterRegistry;

    // Gauges for current state metrics
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong activeRooms = new AtomicLong(0);
    private final AtomicLong friendRequests = new AtomicLong(0);
    private final AtomicLong websocketSessions = new AtomicLong(0);
    private final AtomicLong totalFileSize = new AtomicLong(0);

    // Counters for events
    private Counter messageCounter;
    private Counter loginCounter;
    private Counter fileUploadCounter;
    private Counter rateLimitHitCounter;
    private Counter authErrorCounter;
    private Counter websocketErrorCounter;

    // Timers for response times
    private Timer apiResponseTimer;
    private Timer websocketMessageTimer;

    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register gauges for Prometheus
        Gauge.builder("beam_total_messages", totalMessages, AtomicLong::get)
                .description("Total number of messages sent")
                .register(meterRegistry);

        Gauge.builder("beam_active_users", activeUsers, AtomicLong::get)
                .description("Number of currently online users")
                .register(meterRegistry);

        Gauge.builder("beam_active_rooms", activeRooms, AtomicLong::get)
                .description("Number of active chat rooms")
                .register(meterRegistry);

        Gauge.builder("beam_friend_requests", friendRequests, AtomicLong::get)
                .description("Number of pending friend requests")
                .register(meterRegistry);

        Gauge.builder("beam_websocket_sessions", websocketSessions, AtomicLong::get)
                .description("Number of active WebSocket sessions")
                .register(meterRegistry);

        Gauge.builder("beam_total_file_size_bytes", totalFileSize, AtomicLong::get)
                .description("Total file storage size in bytes")
                .register(meterRegistry);

        // Register counters for events
        messageCounter = Counter.builder("beam_messages_sent_total")
                .description("Total messages sent (counter)")
                .register(meterRegistry);

        loginCounter = Counter.builder("beam_logins_total")
                .description("Total user logins (counter)")
                .register(meterRegistry);

        fileUploadCounter = Counter.builder("beam_files_uploaded_total")
                .description("Total files uploaded")
                .register(meterRegistry);

        rateLimitHitCounter = Counter.builder("beam_rate_limit_hits_total")
                .description("Total rate limit violations")
                .register(meterRegistry);

        authErrorCounter = Counter.builder("beam_auth_errors_total")
                .description("Total authentication errors")
                .register(meterRegistry);

        websocketErrorCounter = Counter.builder("beam_websocket_errors_total")
                .description("Total WebSocket errors")
                .register(meterRegistry);

        // Register timers for response times
        apiResponseTimer = Timer.builder("beam_api_response_time")
                .description("API response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        websocketMessageTimer = Timer.builder("beam_websocket_message_time")
                .description("WebSocket message processing time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * Updates metrics periodically (default: 5 minutes)
     */
    @Scheduled(fixedRateString = "${monitoring.update-interval-ms:300000}")
    public void updateMetrics() {
        try {
            if (directMessageRepository != null && groupMessageRepository != null) {
                long dmCount = directMessageRepository.count();
                long groupCount = groupMessageRepository.count();
                totalMessages.set(dmCount + groupCount);
            }

            if (userRepository != null) {
                // N+1 쿼리 방지: 전용 count 쿼리 사용
                long onlineUsers = userRepository.countOnlineUsers();
                activeUsers.set(onlineUsers);
            }

            if (roomRepository != null) {
                long rooms = roomRepository.count();
                activeRooms.set(rooms);
            }

            if (friendRepository != null) {
                // N+1 쿼리 방지: 전용 count 쿼리 사용
                long pending = friendRepository.countAllPendingRequests();
                friendRequests.set(pending);
            }

            // WebSocket 세션 수 업데이트
            if (sessionManager != null) {
                var stats = sessionManager.getStats();
                websocketSessions.set(stats.totalSessions());
            }

            // 파일 저장소 크기 업데이트
            if (fileMetadataRepository != null) {
                Long fileSizeSum = fileMetadataRepository.getTotalFileSize();
                totalFileSize.set(fileSizeSum != null ? fileSizeSum : 0);
            }

            logger.info("BEAM Monitoring - Messages: {}, Online Users: {}, Active Rooms: {}, " +
                        "WebSocket Sessions: {}, File Storage: {} bytes",
                    totalMessages.get(), activeUsers.get(), activeRooms.get(),
                    websocketSessions.get(), totalFileSize.get());

        } catch (Exception e) {
            logger.error("Monitoring update failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Increment message counter (called when a message is sent)
     */
    public void recordMessageSent() {
        messageCounter.increment();
    }

    /**
     * Increment login counter (called when a user logs in)
     */
    public void recordLogin() {
        loginCounter.increment();
    }

    public long getTotalMessages() {
        return totalMessages.get();
    }

    public long getActiveUsers() {
        return activeUsers.get();
    }

    public long getActiveRooms() {
        return activeRooms.get();
    }

    public long getWebsocketSessions() {
        return websocketSessions.get();
    }

    public long getTotalFileSize() {
        return totalFileSize.get();
    }

    /**
     * Record file upload event
     */
    public void recordFileUpload() {
        fileUploadCounter.increment();
    }

    /**
     * Record rate limit hit event
     */
    public void recordRateLimitHit() {
        rateLimitHitCounter.increment();
    }

    /**
     * Record authentication error event
     */
    public void recordAuthError() {
        authErrorCounter.increment();
    }

    /**
     * Record WebSocket error event
     */
    public void recordWebsocketError() {
        websocketErrorCounter.increment();
    }

    /**
     * Record API response time
     * @param durationMs response time in milliseconds
     */
    public void recordApiResponseTime(long durationMs) {
        apiResponseTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record API response time with Duration
     * @param duration response time duration
     */
    public void recordApiResponseTime(Duration duration) {
        apiResponseTimer.record(duration);
    }

    /**
     * Record WebSocket message processing time
     * @param durationMs processing time in milliseconds
     */
    public void recordWebsocketMessageTime(long durationMs) {
        websocketMessageTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Get Timer for timing API calls (use with try-with-resources)
     * @return Timer.Sample for recording
     */
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop API timer and record the duration
     * @param sample the timer sample to stop
     */
    public void stopApiTimer(Timer.Sample sample) {
        sample.stop(apiResponseTimer);
    }

    /**
     * Get comprehensive metrics summary
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
            totalMessages.get(),
            activeUsers.get(),
            activeRooms.get(),
            friendRequests.get(),
            websocketSessions.get(),
            totalFileSize.get()
        );
    }

    public record MetricsSummary(
        long totalMessages,
        long activeUsers,
        long activeRooms,
        long pendingFriendRequests,
        long websocketSessions,
        long totalFileSizeBytes
    ) {}
}