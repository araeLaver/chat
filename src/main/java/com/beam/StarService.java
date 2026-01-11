package com.beam;

import com.beam.StarredMessageEntity.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StarService {

    @Autowired
    private StarredMessageRepository starredMessageRepository;

    @Value("${app.star.max-per-user:1000}")
    private int maxStarsPerUser;

    @Transactional
    public StarredMessageEntity starMessage(Long userId, Long messageId, MessageType messageType,
                                             String roomId, String conversationId, String note) {
        // Check if already starred
        if (starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(userId, messageId, messageType)) {
            throw new IllegalStateException("Message is already starred");
        }

        // Check max stars limit
        Integer currentCount = starredMessageRepository.countByUserId(userId);
        if (currentCount >= maxStarsPerUser) {
            throw new IllegalStateException("Maximum number of starred messages reached: " + maxStarsPerUser);
        }

        StarredMessageEntity starredMessage = StarredMessageEntity.builder()
            .userId(userId)
            .messageId(messageId)
            .messageType(messageType)
            .roomId(roomId)
            .conversationId(conversationId)
            .note(note)
            .build();

        return starredMessageRepository.save(starredMessage);
    }

    @Transactional
    public void unstarMessage(Long userId, Long messageId, MessageType messageType) {
        starredMessageRepository.deleteByUserIdAndMessageIdAndMessageType(userId, messageId, messageType);
    }

    @Transactional
    public StarredMessageEntity toggleStar(Long userId, Long messageId, MessageType messageType,
                                            String roomId, String conversationId, String note) {
        Optional<StarredMessageEntity> existing = starredMessageRepository
            .findByUserIdAndMessageIdAndMessageType(userId, messageId, messageType);

        if (existing.isPresent()) {
            starredMessageRepository.delete(existing.get());
            return null;
        } else {
            return starMessage(userId, messageId, messageType, roomId, conversationId, note);
        }
    }

    @Transactional(readOnly = true)
    public List<StarredMessageEntity> getStarredMessages(Long userId) {
        return starredMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Page<StarredMessageEntity> getStarredMessages(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return starredMessageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<StarredMessageEntity> getStarredMessagesInRoom(Long userId, String roomId) {
        return starredMessageRepository.findByUserIdAndRoomIdOrderByCreatedAtDesc(userId, roomId);
    }

    @Transactional(readOnly = true)
    public List<StarredMessageEntity> getStarredMessagesInConversation(Long userId, String conversationId) {
        return starredMessageRepository.findByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId);
    }

    @Transactional(readOnly = true)
    public boolean isStarred(Long userId, Long messageId, MessageType messageType) {
        return starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(userId, messageId, messageType);
    }

    @Transactional(readOnly = true)
    public Integer getStarredCount(Long userId) {
        return starredMessageRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Integer getMessageStarCount(Long messageId, MessageType messageType) {
        return starredMessageRepository.countStars(messageId, messageType);
    }

    @Transactional
    public void updateStarNote(Long starId, Long userId, String note) {
        StarredMessageEntity starred = starredMessageRepository.findById(starId)
            .orElseThrow(() -> new IllegalArgumentException("Starred message not found"));

        if (!starred.getUserId().equals(userId)) {
            throw new IllegalStateException("Not authorized to update this starred message");
        }

        starred.setNote(note);
        starredMessageRepository.save(starred);
    }
}
