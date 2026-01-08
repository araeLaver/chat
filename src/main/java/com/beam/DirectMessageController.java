package com.beam;

import com.beam.util.AuthUtil;
import com.beam.util.ResponseHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dm")
public class DirectMessageController {

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long senderId = AuthUtil.extractUserId(token, jwtUtil);
            Long receiverId = Long.valueOf(request.get("receiverId").toString());
            String content = request.get("content").toString();

            DirectMessageEntity message = directMessageService.sendMessage(senderId, receiverId, content);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("messageId", message.getId())
                .put("conversationId", message.getConversationId())
                .put("content", message.getContent())
                .put("timestamp", message.getTimestamp().toString())
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<ConversationEntity> conversations = directMessageService.getUserConversations(userId);

            List<Map<String, Object>> result = conversations.stream().map(conv -> {
                Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
                Optional<UserEntity> otherUserOpt = userRepository.findById(otherUserId);

                Map<String, Object> convMap = new HashMap<>();
                convMap.put("conversationId", conv.getConversationId());
                convMap.put("otherUserId", otherUserId);
                convMap.put("otherUserName", otherUserOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                convMap.put("lastMessage", conv.getLastMessage());
                convMap.put("lastMessageTime", conv.getLastMessageTime() != null ? conv.getLastMessageTime().toString() : null);
                convMap.put("unreadCount", conv.getUnreadCount(userId));
                convMap.put("isOnline", otherUserOpt.map(UserEntity::getIsOnline).orElse(false));

                return convMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable String conversationId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<DirectMessageEntity> messages = directMessageService.getConversationMessages(conversationId, userId);

            List<Map<String, Object>> result = messages.stream().map(msg -> {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("senderId", msg.getSenderId());
                msgMap.put("receiverId", msg.getReceiverId());
                msgMap.put("content", msg.getContent());
                msgMap.put("timestamp", msg.getTimestamp().toString());
                msgMap.put("isRead", msg.getIsRead());
                msgMap.put("messageType", msg.getMessageType().toString());
                msgMap.put("isMine", msg.getSenderId().equals(userId));

                return msgMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @PostMapping("/conversation/start")
    public ResponseEntity<?> startConversation(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            Long otherUserId = Long.valueOf(request.get("userId").toString());

            ConversationEntity conversation = directMessageService.getOrCreateConversation(userId, otherUserId);

            Optional<UserEntity> otherUser = userRepository.findById(otherUserId);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("conversationId", conversation.getConversationId())
                .put("otherUserId", otherUserId)
                .put("otherUserName", otherUser.map(UserEntity::getDisplayName).orElse("Unknown"))
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @PostMapping("/conversation/{conversationId}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String conversationId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            directMessageService.markMessagesAsRead(conversationId, userId);
            return ResponseEntity.ok(ResponseHelper.success("Messages marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }
}