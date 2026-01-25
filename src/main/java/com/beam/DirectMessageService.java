package com.beam;

import com.beam.dto.ConversationListItemProjection;
import com.beam.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 1:1 다이렉트 메시지 서비스
 * - 메시지 전송/조회
 * - 대화 목록 캐싱
 * - 읽음 상태 관리
 */
@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private static final Logger logger = LoggerFactory.getLogger(DirectMessageService.class);

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageEncryptionService encryptionService;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "conversations", key = "#senderId"),
        @CacheEvict(value = "conversations", key = "#receiverId"),
        @CacheEvict(value = "unreadCounts", key = "#senderId + '_' + #receiverId"),
        @CacheEvict(value = "unreadCounts", key = "#receiverId + '_' + #senderId")
    })
    public DirectMessageEntity sendMessage(Long senderId, Long receiverId, String content) {
        return sendMessage(senderId, receiverId, content, null, null);
    }

    /**
     * TTL과 언어 메타데이터를 지원하는 메시지 전송
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "conversations", key = "#senderId"),
        @CacheEvict(value = "conversations", key = "#receiverId"),
        @CacheEvict(value = "unreadCounts", key = "#senderId + '_' + #receiverId"),
        @CacheEvict(value = "unreadCounts", key = "#receiverId + '_' + #senderId")
    })
    public DirectMessageEntity sendMessage(Long senderId, Long receiverId, String content, Long ttlSeconds, String sourceLanguage) {
        userRepository.findById(senderId)
            .orElseThrow(() -> UserException.notFound(senderId));
        userRepository.findById(receiverId)
            .orElseThrow(() -> UserException.notFound(receiverId));

        String conversationId = DirectMessageEntity.generateConversationId(senderId, receiverId);

        ConversationEntity conversation = conversationRepository
            .findByConversationId(conversationId)
            .orElseGet(() -> createConversation(senderId, receiverId, conversationId));

        // 암호화 적용
        String messageContent = content;
        boolean isEncrypted = false;
        if (encryptionService.isEncryptionEnabled() && content != null) {
            messageContent = encryptionService.encryptDirectMessage(content, conversationId);
            isEncrypted = true;
        }

        LocalDateTime now = LocalDateTime.now();
        DirectMessageEntity message = DirectMessageEntity.builder()
            .conversationId(conversationId)
            .senderId(senderId)
            .receiverId(receiverId)
            .content(messageContent)
            .messageType(DirectMessageEntity.MessageType.TEXT)
            .timestamp(now)
            .isRead(false)
            .isEncrypted(isEncrypted)
            .securityType(MessageSecurityType.NORMAL)
            .build();

        // TTL (자동 삭제) 처리
        if (ttlSeconds != null && ttlSeconds > 0) {
            message.setTtlSeconds(ttlSeconds);
            message.setExpiresAt(now.plusSeconds(ttlSeconds));
        }

        // 번역 메타데이터 설정
        if (sourceLanguage != null) {
            message.setSourceLanguage(sourceLanguage);
        }

        message = directMessageRepository.save(message);

        // 대화 미리보기는 원본 저장 (UI 표시용)
        String preview = content != null && content.length() > 50
            ? content.substring(0, 50) + "..."
            : content;
        conversation.setLastMessage(preview);
        conversation.setLastMessageTime(message.getTimestamp());
        conversation.setLastMessageSenderId(senderId);
        conversation.incrementUnreadCount(receiverId);
        conversationRepository.save(conversation);

        return message;
    }

    @Transactional
    public List<DirectMessageEntity> getConversationMessages(String conversationId, Long userId) {
        List<DirectMessageEntity> messages = directMessageRepository
            .findByConversationIdOrderByTimestampAsc(conversationId);

        // 메시지 복호화
        decryptMessages(messages, conversationId);

        markMessagesAsRead(conversationId, userId);

        return messages;
    }

    private void decryptMessages(List<DirectMessageEntity> messages, String conversationId) {
        for (DirectMessageEntity msg : messages) {
            if (Boolean.TRUE.equals(msg.getIsEncrypted())) {
                String decrypted = encryptionService.decryptDirectMessage(
                    msg.getContent(), conversationId, true);
                msg.setContent(decrypted);
            }
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "conversations", key = "#userId"),
        @CacheEvict(value = "unreadCounts", key = "#conversationId + '_' + #userId")
    })
    public void markMessagesAsRead(String conversationId, Long userId) {
        List<DirectMessageEntity> unreadMessages = directMessageRepository
            .findUnreadMessages(conversationId, userId);

        LocalDateTime now = LocalDateTime.now();
        for (DirectMessageEntity message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(now);
        }

        if (!unreadMessages.isEmpty()) {
            directMessageRepository.saveAll(unreadMessages);

            Optional<ConversationEntity> conversationOpt = conversationRepository
                .findByConversationId(conversationId);
            conversationOpt.ifPresent(conv -> {
                conv.resetUnreadCount(userId);
                conversationRepository.save(conv);
            });
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "conversations", key = "#userId")
    public List<ConversationEntity> getUserConversations(Long userId) {
        logger.debug("Loading conversations for user: {} (cache miss)", userId);
        return conversationRepository.findUserConversations(userId);
    }

    /**
     * 대화 목록을 사용자 정보와 함께 조회 (N+1 문제 해결)
     */
    @Transactional(readOnly = true)
    public List<ConversationListItemProjection> getUserConversationsWithUsers(Long userId) {
        logger.debug("Loading conversations with users for user: {}", userId);
        return conversationRepository.findUserConversationsWithUsers(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "unreadCounts", key = "#conversationId + '_' + #userId")
    public Integer getUnreadCount(String conversationId, Long userId) {
        logger.debug("Loading unread count for conversation: {} user: {} (cache miss)", conversationId, userId);
        return directMessageRepository.countUnreadMessages(conversationId, userId);
    }

    @Transactional
    public ConversationEntity getOrCreateConversation(Long user1Id, Long user2Id) {
        return conversationRepository.findByUsers(user1Id, user2Id)
            .orElseGet(() -> {
                String conversationId = DirectMessageEntity.generateConversationId(user1Id, user2Id);
                return createConversation(user1Id, user2Id, conversationId);
            });
    }

    private ConversationEntity createConversation(Long user1Id, Long user2Id, String conversationId) {
        Long smaller = Math.min(user1Id, user2Id);
        Long larger = Math.max(user1Id, user2Id);

        ConversationEntity conversation = ConversationEntity.builder()
            .conversationId(conversationId)
            .user1Id(smaller)
            .user2Id(larger)
            .createdAt(LocalDateTime.now())
            .build();

        return conversationRepository.save(conversation);
    }
}