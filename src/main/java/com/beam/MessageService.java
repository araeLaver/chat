package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageReadReceiptRepository readReceiptRepository;
    
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
    
    public List<MessageEntity> getAllRoomMessages(String roomId) {
        return messageRepository.findByRoomIdOrderByTimestampAsc(roomId);
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
     * 채팅방의 모든 메시지를 읽음 처리
     */
    public void markRoomMessagesAsRead(String roomId, Long userId, String username) {
        List<MessageEntity> messages = messageRepository.findByRoomIdOrderByTimestampAsc(roomId);

        for (MessageEntity message : messages) {
            // 내가 보낸 메시지는 제외
            if (!message.getSender().equals(username)) {
                // 아직 읽지 않은 메시지만 읽음 처리
                if (!readReceiptRepository.existsByMessageIdAndUserId(message.getId(), userId)) {
                    markMessageAsRead(message.getId(), userId);
                }
            }
        }
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