package com.beam;

import com.beam.NotificationEntity.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.notification.content-max-length:100}")
    private int contentMaxLength;

    @Transactional
    public NotificationEntity createNotification(Long userId, NotificationType type,
                                                  Long referenceId, Long actorUserId,
                                                  String content, String roomId) {
        NotificationEntity notification = NotificationEntity.builder()
            .userId(userId)
            .type(type)
            .referenceId(referenceId)
            .actorUserId(actorUserId)
            .content(truncateContent(content))
            .roomId(roomId)
            .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public NotificationEntity createMentionNotification(Long userId, Long actorUserId,
                                                         Long messageId, String content, String roomId) {
        return createNotification(userId, NotificationType.MENTION, messageId, actorUserId, content, roomId);
    }

    @Transactional
    public NotificationEntity createReactionNotification(Long userId, Long actorUserId,
                                                          Long messageId, String emoji, String roomId) {
        String content = "님이 회원님의 메시지에 " + emoji + " 반응을 남겼습니다.";
        return createNotification(userId, NotificationType.REACTION, messageId, actorUserId, content, roomId);
    }

    @Transactional
    public NotificationEntity createReplyNotification(Long userId, Long actorUserId,
                                                       Long messageId, String content, String roomId) {
        return createNotification(userId, NotificationType.REPLY, messageId, actorUserId, content, roomId);
    }

    @Transactional
    public NotificationEntity createPinNotification(Long userId, Long actorUserId,
                                                     Long messageId, String roomId) {
        String content = "님이 회원님의 메시지를 고정했습니다.";
        return createNotification(userId, NotificationType.PIN, messageId, actorUserId, content, roomId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Integer getUnreadCount(Long userId) {
        return notificationRepository.countUnreadNotifications(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getNotificationsByType(Long userId, NotificationType type) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }

    @Transactional
    public void deleteOldNotifications(Long userId, int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByUserIdAndCreatedAtBefore(userId, cutoff);
    }

    private String truncateContent(String content) {
        if (content == null) return null;
        if (content.length() <= contentMaxLength) return content;
        return content.substring(0, contentMaxLength - 3) + "...";
    }
}
