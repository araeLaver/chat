package com.beam;

import com.beam.dto.ConversationListItemProjection;
import com.beam.util.AuthUtil;
import com.beam.util.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dm")
@Tag(name = "Direct Messages", description = "1:1 다이렉트 메시지 API")
@SecurityRequirement(name = "bearerAuth")
public class DirectMessageController {

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "DM 전송", description = "특정 사용자에게 다이렉트 메시지를 전송합니다. 메시지는 자동으로 암호화됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        @ApiResponse(responseCode = "400", description = "수신자를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long senderId = AuthUtil.extractUserId(token, jwtUtil);

            // Null 체크 및 유효성 검증
            Object receiverIdObj = request.get("receiverId");
            Object contentObj = request.get("content");

            if (receiverIdObj == null) {
                return ResponseEntity.badRequest().body(ResponseHelper.error("receiverId is required"));
            }
            if (contentObj == null) {
                return ResponseEntity.badRequest().body(ResponseHelper.error("content is required"));
            }

            Long receiverId;
            try {
                receiverId = Long.valueOf(receiverIdObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(ResponseHelper.error("Invalid receiverId format"));
            }

            String content = contentObj.toString();

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

    @Operation(summary = "대화 목록 조회", description = "현재 사용자의 모든 DM 대화 목록을 조회합니다. 읽지 않은 메시지 수와 상대방 온라인 상태가 포함됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            // N+1 문제 해결: JOIN으로 한 번에 조회
            List<ConversationListItemProjection> conversations =
                directMessageService.getUserConversationsWithUsers(userId);

            List<Map<String, Object>> result = conversations.stream().map(conv -> {
                // 현재 사용자가 user1인지 user2인지 확인하여 상대방 정보 추출
                boolean isUser1 = conv.getUser1Id().equals(userId);
                Long otherUserId = isUser1 ? conv.getUser2Id() : conv.getUser1Id();
                String otherUserName = isUser1 ? conv.getUser2DisplayName() : conv.getUser1DisplayName();
                Boolean isOnline = isUser1 ? conv.getUser2IsOnline() : conv.getUser1IsOnline();
                Integer unreadCount = isUser1 ? conv.getUnreadCountUser1() : conv.getUnreadCountUser2();

                Map<String, Object> convMap = new HashMap<>();
                convMap.put("conversationId", conv.getConversationId());
                convMap.put("otherUserId", otherUserId);
                convMap.put("otherUserName", otherUserName != null ? otherUserName : "Unknown");
                convMap.put("lastMessage", conv.getLastMessage());
                convMap.put("lastMessageTime", conv.getLastMessageTime() != null ? conv.getLastMessageTime().toString() : null);
                convMap.put("unreadCount", unreadCount != null ? unreadCount : 0);
                convMap.put("isOnline", isOnline != null ? isOnline : false);

                return convMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "대화 메시지 조회", description = "특정 대화의 모든 메시지를 시간순으로 조회합니다. 조회 시 읽지 않은 메시지가 자동으로 읽음 처리됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 목록 반환"),
        @ApiResponse(responseCode = "400", description = "대화를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationMessages(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "대화 ID", required = true) @PathVariable String conversationId) {
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

    @Operation(summary = "대화 시작", description = "특정 사용자와의 새로운 대화를 시작하거나 기존 대화를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대화 정보 반환"),
        @ApiResponse(responseCode = "400", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/conversation/start")
    public ResponseEntity<?> startConversation(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
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

    @Operation(summary = "메시지 읽음 처리", description = "특정 대화의 모든 읽지 않은 메시지를 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        @ApiResponse(responseCode = "400", description = "대화를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/conversation/{conversationId}/read")
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "대화 ID", required = true) @PathVariable String conversationId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            directMessageService.markMessagesAsRead(conversationId, userId);
            return ResponseEntity.ok(ResponseHelper.success("Messages marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }
}