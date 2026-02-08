package com.beam;

import com.beam.NotificationEntity.NotificationType;
import com.beam.service.PushNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

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
        NotificationEntity notification = createNotification(userId, NotificationType.MENTION, messageId, actorUserId, content, roomId);

        // Send push notification
        String actorName = getActorName(actorUserId);
        pushNotificationService.sendMentionNotification(userId, actorName, content, roomId);

        return notification;
    }

    @Transactional
    public NotificationEntity createReactionNotification(Long userId, Long actorUserId,
                                                          Long messageId, String emoji, String roomId) {
        String content = "님이 회원님의 메시지에 " + emoji + " 반응을 남겼습니다.";
        NotificationEntity notification = createNotification(userId, NotificationType.REACTION, messageId, actorUserId, content, roomId);

        // Send push notification
        String actorName = getActorName(actorUserId);
        pushNotificationService.sendReactionNotification(userId, actorName, emoji, roomId);

        return notification;
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

    private String getActorName(Long actorUserId) {
        if (actorUserId == null) return "알 수 없음";
        return userRepository.findById(actorUserId)
            .map(user -> user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
            .orElse("알 수 없음");
    }

    /**
     * Send push notification for a new message
     */
    public void sendMessagePushNotification(Long userId, String senderName, String messageContent,
                                             String roomId, String roomName) {
        pushNotificationService.sendMessageNotification(userId, senderName, messageContent, roomId, roomName);
    }

    /**
     * Send push notification for friend request
     */
    public void sendFriendRequestPushNotification(Long userId, String requesterName) {
        pushNotificationService.sendFriendRequestNotification(userId, requesterName);
    }
}
