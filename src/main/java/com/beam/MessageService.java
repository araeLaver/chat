package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReadReceiptRepository readReceiptRepository;

    @Autowired
    private MessageEncryptionService encryptionService;

    @Autowired
    private MentionService mentionService;

    @Value("${app.page.default-size:100}")
    private int defaultPageSize;

    @Value("${app.page.max-size:500}")
    private int maxPageSize;

    public MessageEntity saveMessage(ChatMessage chatMessage) {
        String content = chatMessage.getContent();
        boolean isEncrypted = false;

        // 암호화 활성화 시 메시지 암호화
        if (encryptionService.isEncryptionEnabled() && content != null) {
            content = encryptionService.encryptRoomMessage(content, chatMessage.getRoomId());
            isEncrypted = true;
        }

        MessageEntity entity = new MessageEntity(
            chatMessage.getSender(),
            content,
            chatMessage.getRoomId(),
            chatMessage.getType() != null ? chatMessage.getType() : "message"
        );

        entity.setSecurityType(chatMessage.getSecurityType());
        entity.setIsEncrypted(isEncrypted);

        // TTL (자동 삭제) 처리
        if (chatMessage.getTtlSeconds() != null && chatMessage.getTtlSeconds() > 0) {
            entity.setTtlSeconds(chatMessage.getTtlSeconds());
            entity.setExpiresAt(LocalDateTime.now().plusSeconds(chatMessage.getTtlSeconds()));
        }

        // 번역 메타데이터 설정
        if (chatMessage.getSourceLanguage() != null) {
            entity.setSourceLanguage(chatMessage.getSourceLanguage());
        }

        // Handle reply/quote
        if (chatMessage.getReplyToId() != null) {
            messageRepository.findById(chatMessage.getReplyToId()).ifPresent(originalMessage -> {
                entity.setReplyToId(originalMessage.getId());
                entity.setReplyToSender(originalMessage.getSender());
                entity.setReplyToContent(truncateContent(
                    decryptContentIfNeeded(originalMessage, chatMessage.getRoomId()), 200));
            });
        }

        MessageEntity savedMessage = messageRepository.save(entity);

        // Process mentions in the message content
        if (chatMessage.getContent() != null && chatMessage.getUserId() != null) {
            mentionService.processMentions(
                chatMessage.getContent(),
                savedMessage.getId(),
                MentionEntity.MessageType.ROOM,
                chatMessage.getUserId(),
                chatMessage.getRoomId(),
                null
            );
        }

        return savedMessage;
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength - 3) + "...";
    }

    private String decryptContentIfNeeded(MessageEntity message, String roomId) {
        if (Boolean.TRUE.equals(message.getIsEncrypted())) {
            return encryptionService.decryptRoomMessage(message.getContent(), roomId, true);
        }
        return message.getContent();
    }

    public List<MessageEntity> getRecentMessages(String roomId) {
        List<MessageEntity> messages = messageRepository.findTop50ByRoomIdOrderByTimestampDesc(roomId);
        return decryptMessages(messages, roomId);
    }

    private List<MessageEntity> decryptMessages(List<MessageEntity> messages, String roomId) {
        for (MessageEntity msg : messages) {
            if (Boolean.TRUE.equals(msg.getIsEncrypted())) {
                String decrypted = encryptionService.decryptRoomMessage(
                    msg.getContent(), roomId, true);
                msg.setContent(decrypted);
            }
        }
        return messages;
    }

    /**
     * 채팅방의 모든 메시지 조회 (페이징 없음 - 하위 호환성 유지)
     * @deprecated 대용량 채팅방에서는 getAllRoomMessages(roomId, page, size) 사용 권장
     */
    @Deprecated
    public List<MessageEntity> getAllRoomMessages(String roomId) {
        List<MessageEntity> messages = messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
        return decryptMessages(messages, roomId);
    }

    /**
     * 채팅방 메시지 조회 (페이징 지원)
     */
    public Page<MessageEntity> getAllRoomMessages(String roomId, int page, int size) {
        int pageSize = Math.min(size, maxPageSize);
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<MessageEntity> messagePage = messageRepository.findByRoomIdOrderByTimestampAsc(roomId, pageable);
        // Page 내용 복호화
        decryptMessages(messagePage.getContent(), roomId);
        return messagePage;
    }

    /**
     * 메시지를 읽음 처리
     */
    public MessageReadReceipt markMessageAsRead(Long messageId, Long userId) {
        // 이미 읽음 표시가 있으면 그대로 반환
        var existing = readReceiptRepository.findByMessageIdAndUserId(messageId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 새로운 읽음 표시 생성
        MessageReadReceipt receipt = new MessageReadReceipt(messageId, userId);
        return readReceiptRepository.save(receipt);
    }

    /**
     * 채팅방의 모든 메시지를 읽음 처리 (배치 처리로 N+1 쿼리 방지)
     */
    @Transactional
    public void markRoomMessagesAsRead(String roomId, Long userId, String username) {
        // 1. 읽지 않은 메시지 ID 목록을 한 번의 쿼리로 조회
        List<Long> unreadMessageIds = messageRepository.findUnreadMessageIds(roomId, userId, username);

        if (unreadMessageIds.isEmpty()) {
            return;
        }

        // 2. 배치로 읽음 표시 생성
        List<MessageReadReceipt> receipts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long messageId : unreadMessageIds) {
            MessageReadReceipt receipt = new MessageReadReceipt(messageId, userId);
            receipt.setReadAt(now);
            receipts.add(receipt);
        }

        // 3. 한 번에 저장 (배치 insert)
        readReceiptRepository.saveAll(receipts);
    }

    /**
     * 채팅방의 안읽은 메시지 수 조회
     */
    public long getUnreadMessageCount(String roomId, Long userId, String username) {
        return readReceiptRepository.countUnreadMessagesInRoom(roomId, userId, username);
    }

    /**
     * 메시지를 읽은 사용자 수 조회
     */
    public long getReadCount(Long messageId) {
        return readReceiptRepository.countByMessageId(messageId);
    }

    /**
     * 메시지 읽음 여부 확인
     */
    public boolean isMessageRead(Long messageId, Long userId) {
        return readReceiptRepository.existsByMessageIdAndUserId(messageId, userId);
    }
}