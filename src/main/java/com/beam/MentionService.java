package com.beam;

import com.beam.MentionEntity.MessageType;
import com.beam.NotificationEntity.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @Autowired
    private MentionRepository mentionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public List<MentionEntity> processMentions(String content, Long messageId,
                                                MessageType messageType, Long senderId,
                                                String roomId, String conversationId) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<MentionEntity> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String username = matcher.group(1);
            Optional<UserEntity> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                UserEntity mentionedUser = userOpt.get();

                // Don't create self-mentions
                if (mentionedUser.getId().equals(senderId)) {
                    continue;
                }

                // Check if already mentioned in this message
                if (mentionRepository.existsByMessageIdAndMessageTypeAndMentionedUserId(
                        messageId, messageType, mentionedUser.getId())) {
                    continue;
                }

                MentionEntity mention = MentionEntity.builder()
                    .messageId(messageId)
                    .messageType(messageType)
                    .mentionedUserId(mentionedUser.getId())
                    .mentionerUserId(senderId)
                    .roomId(roomId)
                    .conversationId(conversationId)
                    .build();

                mentions.add(mentionRepository.save(mention));

                // Create notification
                notificationService.createMentionNotification(
                    mentionedUser.getId(), senderId, messageId, content, roomId);
            }
        }

        return mentions;
    }

    @Transactional(readOnly = true)
    public List<MentionEntity> getUnreadMentions(Long userId) {
        return mentionRepository.findByMentionedUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<MentionEntity> getAllMentions(Long userId) {
        return mentionRepository.findByMentionedUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Integer getUnreadMentionCount(Long userId) {
        return mentionRepository.countUnreadMentions(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        mentionRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void markAsRead(Long mentionId, Long userId) {
        mentionRepository.markAsRead(mentionId, userId, LocalDateTime.now());
    }

    @Transactional
    public void markRoomMentionsAsRead(Long userId, String roomId) {
        List<MentionEntity> unreadMentions = mentionRepository.findUnreadMentionsInRoom(userId, roomId);
        LocalDateTime now = LocalDateTime.now();

        for (MentionEntity mention : unreadMentions) {
            mention.setIsRead(true);
            mention.setReadAt(now);
        }

        mentionRepository.saveAll(unreadMentions);
    }

    public List<Long> extractMentionedUserIds(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String username = matcher.group(1);
            userRepository.findByUsername(username)
                .ifPresent(user -> userIds.add(user.getId()));
        }

        return userIds;
    }
}
