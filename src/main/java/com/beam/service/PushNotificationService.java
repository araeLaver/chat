package com.beam.service;

import com.beam.DeviceTokenEntity;
import com.beam.DeviceTokenRepository;
import com.beam.FirebaseConfig;
import com.beam.NotificationPreferencesEntity;
import com.beam.NotificationPreferencesRepository;
import com.beam.UserEntity;
import com.beam.UserRepository;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Autowired
    private FirebaseConfig firebaseConfig;

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Send a push notification to a specific user
     */
    @Async
    public CompletableFuture<Boolean> sendToUser(Long userId, String title, String body) {
        return sendToUser(userId, title, body, new HashMap<>());
    }

    /**
     * Send a push notification to a specific user with custom data
     */
    @Async
    public CompletableFuture<Boolean> sendToUser(Long userId, String title, String body, Map<String, String> data) {
        if (!firebaseConfig.isEnabled()) {
            logger.debug("Firebase is disabled. Skipping push notification.");
            return CompletableFuture.completedFuture(false);
        }

        // Check user preferences
        NotificationPreferencesEntity preferences = notificationPreferencesRepository
            .findByUserId(userId)
            .orElse(null);

        if (preferences != null) {
            if (!preferences.getPushEnabled()) {
                logger.debug("Push notifications disabled for user {}", userId);
                return CompletableFuture.completedFuture(false);
            }
            if (preferences.isQuietHoursActive()) {
                logger.debug("Quiet hours active for user {}", userId);
                return CompletableFuture.completedFuture(false);
            }
            // If message preview is disabled, hide the content
            if (!preferences.getMessagePreview()) {
                body = "새 메시지가 도착했습니다";
            }
        }

        List<DeviceTokenEntity> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            logger.debug("No active device tokens for user {}", userId);
            return CompletableFuture.completedFuture(false);
        }

        List<String> tokenStrings = tokens.stream()
            .map(DeviceTokenEntity::getDeviceToken)
            .toList();

        return sendToTokens(tokenStrings, title, body, data, preferences);
    }

    /**
     * Send a push notification to multiple users
     */
    @Async
    public CompletableFuture<Integer> sendToUsers(List<Long> userIds, String title, String body, Map<String, String> data) {
        if (!firebaseConfig.isEnabled()) {
            return CompletableFuture.completedFuture(0);
        }

        List<DeviceTokenEntity> tokens = deviceTokenRepository.findActiveTokensByUserIds(userIds);
        if (tokens.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        // Filter out users with push disabled or in quiet hours
        List<String> validTokens = new ArrayList<>();
        for (DeviceTokenEntity token : tokens) {
            NotificationPreferencesEntity prefs = notificationPreferencesRepository
                .findByUserId(token.getUserId())
                .orElse(null);

            if (prefs == null || (prefs.getPushEnabled() && !prefs.isQuietHoursActive())) {
                validTokens.add(token.getDeviceToken());
            }
        }

        if (validTokens.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        try {
            MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .addAllTokens(validTokens)
                .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            int successCount = response.getSuccessCount();

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                handleFailedTokens(validTokens, response.getResponses());
            }

            return CompletableFuture.completedFuture(successCount);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send multicast push notification: {}", e.getMessage());
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Send a message notification (for chat messages)
     */
    @Async
    public CompletableFuture<Boolean> sendMessageNotification(Long userId, String senderName, String messageContent,
                                                               String roomId, String roomName) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "message");
        data.put("roomId", roomId);
        data.put("roomName", roomName);
        data.put("senderId", senderName);

        String title = roomName != null ? roomName : senderName;
        String body = senderName + ": " + truncateMessage(messageContent, 100);

        return sendToUser(userId, title, body, data);
    }

    /**
     * Send a mention notification
     */
    @Async
    public CompletableFuture<Boolean> sendMentionNotification(Long userId, String mentionerName,
                                                               String messageContent, String roomId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "mention");
        data.put("roomId", roomId);

        String title = "멘션됨";
        String body = mentionerName + "님이 회원님을 멘션했습니다: " + truncateMessage(messageContent, 80);

        return sendToUser(userId, title, body, data);
    }

    /**
     * Send a reaction notification
     */
    @Async
    public CompletableFuture<Boolean> sendReactionNotification(Long userId, String reactorName,
                                                                String emoji, String roomId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "reaction");
        data.put("roomId", roomId);

        String title = "새로운 반응";
        String body = reactorName + "님이 " + emoji + " 반응을 남겼습니다";

        return sendToUser(userId, title, body, data);
    }

    /**
     * Send a friend request notification
     */
    @Async
    public CompletableFuture<Boolean> sendFriendRequestNotification(Long userId, String requesterName) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "friend_request");

        String title = "친구 요청";
        String body = requesterName + "님이 친구 요청을 보냈습니다";

        return sendToUser(userId, title, body, data);
    }

    private CompletableFuture<Boolean> sendToTokens(List<String> tokens, String title, String body,
                                                     Map<String, String> data,
                                                     NotificationPreferencesEntity preferences) {
        try {
            // Build Android config with sound settings
            AndroidConfig.Builder androidBuilder = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH);

            if (preferences != null && preferences.getSoundEnabled()) {
                androidBuilder.setNotification(AndroidNotification.builder()
                    .setSound("default")
                    .build());
            }

            // Build APNs config for iOS
            ApnsConfig.Builder apnsBuilder = ApnsConfig.builder()
                .setAps(Aps.builder()
                    .setAlert(ApsAlert.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .build());

            if (preferences != null && preferences.getSoundEnabled()) {
                apnsBuilder.setAps(Aps.builder()
                    .setAlert(ApsAlert.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .setSound("default")
                    .build());
            }

            MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .setAndroidConfig(androidBuilder.build())
                .setApnsConfig(apnsBuilder.build())
                .putAllData(data)
                .addAllTokens(tokens)
                .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                handleFailedTokens(tokens, response.getResponses());
            }

            return CompletableFuture.completedFuture(response.getSuccessCount() > 0);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send push notification: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    private void handleFailedTokens(List<String> tokens, List<SendResponse> responses) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (!response.isSuccessful()) {
                FirebaseMessagingException exception = response.getException();
                if (exception != null) {
                    MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                    // Deactivate invalid or unregistered tokens
                    if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                        errorCode == MessagingErrorCode.UNREGISTERED) {
                        String failedToken = tokens.get(i);
                        logger.info("Deactivating invalid token: {}", failedToken.substring(0, 20) + "...");
                        deviceTokenRepository.deactivateByToken(failedToken);
                    }
                }
            }
        }
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) return "";
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength - 3) + "...";
    }
}
