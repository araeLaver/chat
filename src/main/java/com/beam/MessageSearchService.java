package com.beam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 메시지 검색 서비스
 * - DM/그룹 메시지 검색
 * - 비동기 병렬 검색 지원
 */
@Service
public class MessageSearchService {

    private static final Logger logger = LoggerFactory.getLogger(MessageSearchService.class);

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * DM 메시지 비동기 검색
     */
    @Async("taskExecutor")
    public CompletableFuture<List<Map<String, Object>>> searchDirectMessagesAsync(Long userId, String keyword) {
        logger.debug("Starting async DM search for user: {}", userId);
        List<Map<String, Object>> results = searchDirectMessages(userId, keyword);
        logger.debug("Async DM search completed: {} results found", results.size());
        return CompletableFuture.completedFuture(results);
    }

    /**
     * 그룹 메시지 비동기 검색
     */
    @Async("taskExecutor")
    public CompletableFuture<List<Map<String, Object>>> searchRoomMessagesAsync(Long userId, String keyword) {
        logger.debug("Starting async room search for user: {}", userId);
        List<Map<String, Object>> results = searchRoomMessages(userId, keyword);
        logger.debug("Async room search completed: {} results found", results.size());
        return CompletableFuture.completedFuture(results);
    }

    public List<Map<String, Object>> searchDirectMessages(Long userId, String keyword) {
        List<ConversationEntity> userConversations = conversationRepository.findUserConversations(userId);

        List<Map<String, Object>> results = new ArrayList<>();

        for (ConversationEntity conversation : userConversations) {
            List<DirectMessageEntity> messages = directMessageRepository
                .findByConversationIdOrderByTimestampAsc(conversation.getConversationId());

            List<DirectMessageEntity> matchedMessages = messages.stream()
                .filter(msg -> msg.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

            for (DirectMessageEntity message : matchedMessages) {
                Long otherUserId = message.getSenderId().equals(userId)
                    ? message.getReceiverId()
                    : message.getSenderId();

                Optional<UserEntity> otherUser = userRepository.findById(otherUserId);

                Map<String, Object> result = new HashMap<>();
                result.put("type", "DM");
                result.put("messageId", message.getId());
                result.put("conversationId", message.getConversationId());
                result.put("content", message.getContent());
                result.put("senderId", message.getSenderId());
                result.put("timestamp", message.getTimestamp().toString());
                result.put("otherUserId", otherUserId);
                result.put("otherUserName", otherUser.map(UserEntity::getDisplayName).orElse("Unknown"));
                result.put("isMine", message.getSenderId().equals(userId));

                results.add(result);
            }
        }

        return results;
    }

    public List<Map<String, Object>> searchRoomMessages(Long userId, String keyword) {
        List<RoomMemberEntity> memberships = roomMemberRepository.findByUserIdAndIsActiveTrue(userId);

        List<Map<String, Object>> results = new ArrayList<>();

        for (RoomMemberEntity membership : memberships) {
            List<GroupMessageEntity> messages = groupMessageRepository
                .findByRoomIdAndIsDeletedFalseOrderByTimestampAsc(membership.getRoomId());

            List<GroupMessageEntity> matchedMessages = messages.stream()
                .filter(msg -> msg.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

            for (GroupMessageEntity message : matchedMessages) {
                Optional<UserEntity> sender = userRepository.findById(message.getSenderId());

                Map<String, Object> result = new HashMap<>();
                result.put("type", "ROOM");
                result.put("messageId", message.getId());
                result.put("roomId", message.getRoomId());
                result.put("content", message.getContent());
                result.put("senderId", message.getSenderId());
                result.put("senderName", sender.map(UserEntity::getDisplayName).orElse("Unknown"));
                result.put("timestamp", message.getTimestamp().toString());
                result.put("isMine", message.getSenderId().equals(userId));

                results.add(result);
            }
        }

        return results;
    }

    public List<Map<String, Object>> searchAllMessages(Long userId, String keyword) {
        List<Map<String, Object>> dmResults = searchDirectMessages(userId, keyword);
        List<Map<String, Object>> roomResults = searchRoomMessages(userId, keyword);

        List<Map<String, Object>> allResults = new ArrayList<>();
        allResults.addAll(dmResults);
        allResults.addAll(roomResults);

        allResults.sort((a, b) -> {
            String timeA = a.get("timestamp").toString();
            String timeB = b.get("timestamp").toString();
            return timeB.compareTo(timeA);
        });

        return allResults;
    }

    /**
     * 전체 메시지 비동기 병렬 검색
     * DM과 그룹 메시지를 동시에 검색하여 성능 향상
     */
    public CompletableFuture<List<Map<String, Object>>> searchAllMessagesAsync(Long userId, String keyword) {
        logger.debug("Starting parallel async search for user: {}", userId);
        long startTime = System.currentTimeMillis();

        CompletableFuture<List<Map<String, Object>>> dmFuture = searchDirectMessagesAsync(userId, keyword);
        CompletableFuture<List<Map<String, Object>>> roomFuture = searchRoomMessagesAsync(userId, keyword);

        return CompletableFuture.allOf(dmFuture, roomFuture)
            .thenApply(v -> {
                List<Map<String, Object>> allResults = new ArrayList<>();

                try {
                    allResults.addAll(dmFuture.join());
                    allResults.addAll(roomFuture.join());
                } catch (Exception e) {
                    logger.error("Error during async search: {}", e.getMessage());
                }

                // 시간순 정렬 (최신순)
                allResults.sort((a, b) -> {
                    String timeA = a.get("timestamp").toString();
                    String timeB = b.get("timestamp").toString();
                    return timeB.compareTo(timeA);
                });

                long duration = System.currentTimeMillis() - startTime;
                logger.debug("Parallel async search completed in {}ms: {} total results", duration, allResults.size());

                return allResults;
            });
    }
}