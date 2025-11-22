package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageSearchService {

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
}