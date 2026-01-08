package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DirectMessageService {

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageEncryptionService encryptionService;

    @Transactional
    public DirectMessageEntity sendMessage(Long senderId, Long receiverId, String content) {
        UserEntity sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        UserEntity receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new RuntimeException("Receiver not found"));

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

        DirectMessageEntity message = DirectMessageEntity.builder()
            .conversationId(conversationId)
            .senderId(senderId)
            .receiverId(receiverId)
            .content(messageContent)
            .messageType(DirectMessageEntity.MessageType.TEXT)
            .timestamp(LocalDateTime.now())
            .isRead(false)
            .isEncrypted(isEncrypted)
            .securityType(MessageSecurityType.NORMAL)
            .build();

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
    public List<ConversationEntity> getUserConversations(Long userId) {
        return conversationRepository.findUserConversations(userId);
    }

    @Transactional(readOnly = true)
    public Integer getUnreadCount(String conversationId, Long userId) {
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