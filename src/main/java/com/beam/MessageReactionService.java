package com.beam;

import com.beam.MessageReactionEntity.MessageType;
import com.beam.exception.MessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageReactionService {

    private final MessageReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final DirectMessageRepository directMessageRepository;

    @Transactional
    public MessageReactionEntity addReaction(Long messageId, MessageType messageType,
                                              Long userId, String emoji) {
        // Validate message exists
        validateMessageExists(messageId, messageType);

        // Check for duplicate reaction
        if (reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
                messageId, messageType, userId, emoji)) {
            throw MessageException.reactionExists(messageId, userId, emoji);
        }

        MessageReactionEntity reaction = MessageReactionEntity.builder()
            .messageId(messageId)
            .messageType(messageType)
            .userId(userId)
            .emoji(emoji)
            .build();

        return reactionRepository.save(reaction);
    }

    @Transactional
    public void removeReaction(Long messageId, MessageType messageType,
                               Long userId, String emoji) {
        reactionRepository.deleteByMessageIdAndMessageTypeAndUserIdAndEmoji(
            messageId, messageType, userId, emoji);
    }

    @Transactional
    public MessageReactionEntity toggleReaction(Long messageId, MessageType messageType,
                                                 Long userId, String emoji) {
        Optional<MessageReactionEntity> existing = reactionRepository
            .findByMessageIdAndMessageTypeAndUserIdAndEmoji(messageId, messageType, userId, emoji);

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            return null;
        } else {
            return addReaction(messageId, messageType, userId, emoji);
        }
    }

    @Transactional(readOnly = true)
    public List<ReactionSummary> getReactionSummary(Long messageId, MessageType messageType) {
        List<Object[]> counts = reactionRepository.countReactionsByEmoji(messageId, messageType);
        List<Object[]> usersData = reactionRepository.findReactionsWithUsers(messageId, messageType);

        // Group users by emoji
        Map<String, List<Long>> usersByEmoji = usersData.stream()
            .collect(Collectors.groupingBy(
                row -> (String) row[0],
                Collectors.mapping(row -> (Long) row[1], Collectors.toList())
            ));

        return counts.stream()
            .map(row -> {
                String emoji = (String) row[0];
                int count = ((Long) row[1]).intValue();
                List<Long> users = usersByEmoji.getOrDefault(emoji, Collections.emptyList());
                return new ReactionSummary(emoji, count, users);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageReactionEntity> getReactions(Long messageId, MessageType messageType) {
        return reactionRepository.findByMessageIdAndMessageType(messageId, messageType);
    }

    @Transactional(readOnly = true)
    public boolean hasUserReacted(Long messageId, MessageType messageType, Long userId, String emoji) {
        return reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
            messageId, messageType, userId, emoji);
    }

    private void validateMessageExists(Long messageId, MessageType messageType) {
        boolean exists = switch (messageType) {
            case ROOM -> messageRepository.existsById(messageId);
            case DIRECT -> directMessageRepository.existsById(messageId);
            case GROUP -> messageRepository.existsById(messageId); // Use same repo for now
        };

        if (!exists) {
            throw MessageException.notFound(messageId);
        }
    }

    public static class ReactionSummary {
        private final String emoji;
        private final int count;
        private final List<Long> userIds;

        public ReactionSummary(String emoji, int count, List<Long> userIds) {
            this.emoji = emoji;
            this.count = count;
            this.userIds = userIds;
        }

        public String getEmoji() {
            return emoji;
        }

        public int getCount() {
            return count;
        }

        public List<Long> getUserIds() {
            return userIds;
        }
    }
}
