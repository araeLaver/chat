package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
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

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 500;

    public MessageEntity saveMessage(ChatMessage chatMessage) {
        MessageEntity entity = new MessageEntity(
            chatMessage.getSender(),
            chatMessage.getContent(),
            chatMessage.getRoomId(),
            chatMessage.getType() != null ? chatMessage.getType() : "message"
        );

        entity.setSecurityType(chatMessage.getSecurityType());

        return messageRepository.save(entity);
    }

    public List<MessageEntity> getRecentMessages(String roomId) {
        return messageRepository.findTop50ByRoomIdOrderByTimestampDesc(roomId);
    }

    /**
     * 채팅방의 모든 메시지 조회 (페이징 없음 - 하위 호환성 유지)
     * @deprecated 대용량 채팅방에서는 getAllRoomMessages(roomId, page, size) 사용 권장
     */
    @Deprecated
    public List<MessageEntity> getAllRoomMessages(String roomId) {
        return messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    /**
     * 채팅방 메시지 조회 (페이징 지원)
     */
    public Page<MessageEntity> getAllRoomMessages(String roomId, int page, int size) {
        int pageSize = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, pageSize);
        return messageRepository.findByRoomIdOrderByTimestampAsc(roomId, pageable);
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