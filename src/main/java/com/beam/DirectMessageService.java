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

        DirectMessageEntity message = DirectMessageEntity.builder()
            .conversationId(conversationId)
            .senderId(senderId)
            .receiverId(receiverId)
            .content(content)
            .messageType(DirectMessageEntity.MessageType.TEXT)
            .timestamp(LocalDateTime.now())
            .isRead(false)
            .build();

        message = directMessageRepository.save(message);

        conversation.setLastMessage(content);
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

        markMessagesAsRead(conversationId, userId);

        return messages;
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