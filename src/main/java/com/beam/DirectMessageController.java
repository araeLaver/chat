package com.beam;

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
            String jwtToken = token.replace("Bearer ", "");
            Long senderId = jwtUtil.getUserIdFromToken(jwtToken);
            Long receiverId = Long.valueOf(request.get("receiverId").toString());
            String content = request.get("content").toString();

            DirectMessageEntity message = directMessageService.sendMessage(senderId, receiverId, content);

            Map<String, Object> response = new HashMap<>();
            response.put("messageId", message.getId());
            response.put("conversationId", message.getConversationId());
            response.put("content", message.getContent());
            response.put("timestamp", message.getTimestamp().toString());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

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
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable String conversationId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

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
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/conversation/start")
    public ResponseEntity<?> startConversation(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);
            Long otherUserId = Long.valueOf(request.get("userId").toString());

            ConversationEntity conversation = directMessageService.getOrCreateConversation(userId, otherUserId);

            Optional<UserEntity> otherUser = userRepository.findById(otherUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversation.getConversationId());
            response.put("otherUserId", otherUserId);
            response.put("otherUserName", otherUser.map(UserEntity::getDisplayName).orElse("Unknown"));
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/conversation/{conversationId}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String conversationId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            directMessageService.markMessagesAsRead(conversationId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Messages marked as read");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}