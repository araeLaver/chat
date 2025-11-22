package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    @Autowired(required = false)
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private JwtUtil jwtUtil;

    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload Map<String, Object> message,
                                    SimpMessageHeaderAccessor headerAccessor) {
        try {
            String token = (String) message.get("token");
            Long senderId = null;

            if (token != null && !token.isEmpty()) {
                token = token.replace("Bearer ", "");
                senderId = jwtUtil.getUserIdFromToken(token);
            }

            if (senderId != null) {
                Long receiverId = Long.valueOf(message.get("receiverId").toString());
                String content = message.get("content").toString();

                DirectMessageEntity savedMessage = directMessageService.sendMessage(
                    senderId, receiverId, content);

                Map<String, Object> response = new HashMap<>();
                response.put("type", "DM");
                response.put("messageId", savedMessage.getId());
                response.put("conversationId", savedMessage.getConversationId());
                response.put("senderId", savedMessage.getSenderId());
                response.put("receiverId", savedMessage.getReceiverId());
                response.put("content", savedMessage.getContent());
                response.put("timestamp", savedMessage.getTimestamp().toString());

                messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/messages",
                    response
                );

                messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages",
                    response
                );
            } else {
                Map<String, String> error = Map.of("error", "Authentication required for DM");
                if (headerAccessor.getUser() != null) {
                    messagingTemplate.convertAndSendToUser(
                        headerAccessor.getUser().getName(),
                        "/queue/errors",
                        error
                    );
                }
            }
        } catch (Exception e) {
            Map<String, String> error = Map.of("error", e.getMessage());
            if (headerAccessor.getUser() != null) {
                messagingTemplate.convertAndSendToUser(
                    headerAccessor.getUser().getName(),
                    "/queue/errors",
                    error
                );
            }
        }
    }

    @MessageMapping("/room.send")
    public void sendRoomMessage(@Payload Map<String, Object> message,
                                 SimpMessageHeaderAccessor headerAccessor) {
        try {
            String token = (String) message.get("token");
            Long senderId = null;

            if (token != null && !token.isEmpty()) {
                token = token.replace("Bearer ", "");
                senderId = jwtUtil.getUserIdFromToken(token);
            }

            if (senderId != null) {
                Long roomId = Long.valueOf(message.get("roomId").toString());
                String content = message.get("content").toString();

                GroupMessageEntity savedMessage = roomService.sendMessage(
                    roomId, senderId, content, GroupMessageEntity.MessageType.TEXT);

                List<RoomMemberEntity> members = roomService.getRoomMembers(roomId, senderId);

                Map<String, Object> response = new HashMap<>();
                response.put("type", "ROOM");
                response.put("messageId", savedMessage.getId());
                response.put("roomId", savedMessage.getRoomId());
                response.put("senderId", savedMessage.getSenderId());
                response.put("content", savedMessage.getContent());
                response.put("timestamp", savedMessage.getTimestamp().toString());

                messagingTemplate.convertAndSend("/topic/room." + roomId, response);
            } else {
                Map<String, String> error = Map.of("error", "Authentication required for room chat");
                if (headerAccessor.getUser() != null) {
                    messagingTemplate.convertAndSendToUser(
                        headerAccessor.getUser().getName(),
                        "/queue/errors",
                        error
                    );
                }
            }
        } catch (Exception e) {
            Map<String, String> error = Map.of("error", e.getMessage());
            if (headerAccessor.getUser() != null) {
                messagingTemplate.convertAndSendToUser(
                    headerAccessor.getUser().getName(),
                    "/queue/errors",
                    error
                );
            }
        }
    }

    @MessageMapping("/typing")
    public void handleTyping(@Payload Map<String, Object> typingData) {
        try {
            String token = (String) typingData.get("token");
            if (token != null) {
                token = token.replace("Bearer ", "");
                Long userId = jwtUtil.getUserIdFromToken(token);

                String type = typingData.get("type").toString();

                Map<String, Object> response = new HashMap<>();
                response.put("userId", userId);
                response.put("isTyping", typingData.get("isTyping"));
                response.put("timestamp", LocalDateTime.now().toString());

                if ("DM".equals(type)) {
                    Long receiverId = Long.valueOf(typingData.get("receiverId").toString());
                    messagingTemplate.convertAndSendToUser(
                        receiverId.toString(),
                        "/queue/typing",
                        response
                    );
                } else if ("ROOM".equals(type)) {
                    Long roomId = Long.valueOf(typingData.get("roomId").toString());
                    messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", response);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling typing: " + e.getMessage());
        }
    }

    @MessageMapping("/user.status")
    public void updateUserStatus(@Payload Map<String, Object> statusData) {
        try {
            String token = (String) statusData.get("token");
            if (token != null) {
                token = token.replace("Bearer ", "");
                Long userId = jwtUtil.getUserIdFromToken(token);

                Map<String, Object> response = new HashMap<>();
                response.put("userId", userId);
                response.put("status", statusData.get("status"));
                response.put("timestamp", LocalDateTime.now().toString());

                messagingTemplate.convertAndSend("/topic/user-status", response);
            }
        } catch (Exception e) {
            System.err.println("Error updating user status: " + e.getMessage());
        }
    }
}