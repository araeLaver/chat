package com.beam;

import com.beam.exception.MessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadReceiptService {

    private final ReadReceiptRepository readReceiptRepository;
    private final DirectMessageRepository directMessageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private SimpMessageSendingOperations messagingTemplate;

    @Transactional
    public void markDirectMessageAsRead(Long messageId, Long userId) {
        if (readReceiptRepository.existsByMessageIdAndUserId(messageId, userId)) {
            return;
        }

        DirectMessageEntity message = directMessageRepository.findById(messageId)
            .orElseThrow(() -> MessageException.notFound(messageId));

        if (!message.getReceiverId().equals(userId)) {
            throw MessageException.readUnauthorized(messageId, userId);
        }

        ReadReceiptEntity receipt = ReadReceiptEntity.builder()
            .messageId(messageId)
            .userId(userId)
            .readAt(LocalDateTime.now())
            .build();

        readReceiptRepository.save(receipt);

        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        directMessageRepository.save(message);

        if (messagingTemplate != null) {
            Map<String, Object> notification = Map.of(
                "type", "READ_RECEIPT_DM",
                "messageId", messageId,
                "userId", userId,
                "readAt", LocalDateTime.now().toString()
            );

            messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/read-receipts",
                notification
            );
        }
    }

    @Transactional
    public void markGroupMessageAsRead(Long messageId, Long userId) {
        if (readReceiptRepository.existsByGroupMessageIdAndUserId(messageId, userId)) {
            return;
        }

        GroupMessageEntity message = groupMessageRepository.findById(messageId)
            .orElseThrow(() -> MessageException.notFound(messageId));

        if (message.getSenderId().equals(userId)) {
            return;
        }

        ReadReceiptEntity receipt = ReadReceiptEntity.builder()
            .groupMessageId(messageId)
            .userId(userId)
            .readAt(LocalDateTime.now())
            .build();

        readReceiptRepository.save(receipt);

        message.incrementReadCount();
        groupMessageRepository.save(message);

        if (messagingTemplate != null) {
            Map<String, Object> notification = Map.of(
                "type", "READ_RECEIPT_GROUP",
                "messageId", messageId,
                "roomId", message.getRoomId(),
                "userId", userId,
                "readAt", LocalDateTime.now().toString(),
                "totalReadCount", message.getReadCount()
            );

            messagingTemplate.convertAndSend(
                "/topic/room." + message.getRoomId() + ".read-receipts",
                notification
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMessageReadReceipts(Long messageId, boolean isGroupMessage) {
        List<ReadReceiptEntity> receipts = isGroupMessage
            ? readReceiptRepository.findByGroupMessageId(messageId)
            : readReceiptRepository.findByMessageId(messageId);

        return receipts.stream().map(receipt -> {
            Optional<UserEntity> userOpt = userRepository.findById(receipt.getUserId());

            Map<String, Object> receiptMap = new HashMap<>();
            receiptMap.put("userId", receipt.getUserId());
            receiptMap.put("userName", userOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
            receiptMap.put("readAt", receipt.getReadAt().toString());

            return receiptMap;
        }).collect(Collectors.toList());
    }
}