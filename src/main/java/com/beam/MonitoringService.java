package com.beam;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
 * </ul>
 *
 * <p>Metrics endpoint: /actuator/prometheus
 *
 * @since 1.1.0
 */
@Service
public class MonitoringService {

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

    private final MeterRegistry meterRegistry;

    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong activeRooms = new AtomicLong(0);
    private final AtomicLong friendRequests = new AtomicLong(0);

    // Counters for events
    private Counter messageCounter;
    private Counter loginCounter;

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

        // Register counters for events
        messageCounter = Counter.builder("beam_messages_sent_total")
                .description("Total messages sent (counter)")
                .register(meterRegistry);

        loginCounter = Counter.builder("beam_logins_total")
                .description("Total user logins (counter)")
                .register(meterRegistry);
    }

    /**
     * Updates metrics every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void updateMetrics() {
        try {
            if (directMessageRepository != null && groupMessageRepository != null) {
                long dmCount = directMessageRepository.count();
                long groupCount = groupMessageRepository.count();
                totalMessages.set(dmCount + groupCount);
            }

            if (userRepository != null) {
                long onlineUsers = userRepository.findAll().stream()
                    .filter(user -> user.getIsOnline() != null && user.getIsOnline())
                    .count();
                activeUsers.set(onlineUsers);
            }

            if (roomRepository != null) {
                long rooms = roomRepository.count();
                activeRooms.set(rooms);
            }

            if (friendRepository != null) {
                long pending = friendRepository.findAll().stream()
                    .filter(f -> f.getStatus() == FriendEntity.FriendStatus.PENDING)
                    .count();
                friendRequests.set(pending);
            }

            System.out.println("=== BEAM 메신저 모니터링 ===");
            System.out.println("시간: " + LocalDateTime.now());
            System.out.println("총 메시지: " + totalMessages.get());
            System.out.println("온라인 사용자: " + activeUsers.get());
            System.out.println("활성 채팅방: " + activeRooms.get());
            System.out.println("대기 중인 친구 요청: " + friendRequests.get());
            System.out.println("==========================");

        } catch (Exception e) {
            System.err.println("모니터링 업데이트 실패: " + e.getMessage());
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
}