package com.beam;

import com.beam.util.AuthUtil;
import com.beam.util.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "알림 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MentionService mentionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 50)")
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            Page<NotificationEntity> notifications = notificationService.getNotifications(userId, page, size);

            List<Map<String, Object>> result = notifications.getContent().stream()
                .map(this::mapNotificationToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("notifications", result)
                .put("totalPages", notifications.getTotalPages())
                .put("totalElements", notifications.getTotalElements())
                .put("currentPage", page)
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "읽지 않은 알림 조회", description = "읽지 않은 알림만 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "읽지 않은 알림 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            List<NotificationEntity> notifications = notificationService.getUnreadNotifications(userId);

            List<Map<String, Object>> result = notifications.stream()
                .map(this::mapNotificationToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "읽지 않은 알림 수 조회", description = "읽지 않은 알림의 개수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "읽지 않은 알림 수 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            Integer notificationCount = notificationService.getUnreadCount(userId);
            Integer mentionCount = mentionService.getUnreadMentionCount(userId);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("notificationCount", notificationCount)
                .put("mentionCount", mentionCount)
                .put("totalCount", notificationCount + mentionCount)
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            notificationService.markAllAsRead(userId);
            mentionService.markAllAsRead(userId);

            return ResponseEntity.ok(ResponseHelper.success("All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "특정 알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "알림 ID", required = true)
            @PathVariable Long notificationId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            notificationService.markAsRead(notificationId, userId);

            return ResponseEntity.ok(ResponseHelper.success("Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "멘션 목록 조회", description = "사용자가 멘션된 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "멘션 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/mentions")
    public ResponseEntity<?> getMentions(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "읽지 않은 멘션만 조회")
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<MentionEntity> mentions = unreadOnly
                ? mentionService.getUnreadMentions(userId)
                : mentionService.getAllMentions(userId);

            List<Map<String, Object>> result = mentions.stream()
                .map(this::mapMentionToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    private Map<String, Object> mapNotificationToResponse(NotificationEntity notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("type", notification.getType().name());
        map.put("referenceId", notification.getReferenceId());
        map.put("content", notification.getContent());
        map.put("roomId", notification.getRoomId());
        map.put("isRead", notification.getIsRead());
        map.put("createdAt", notification.getCreatedAt().toString());

        if (notification.getActorUserId() != null) {
            userRepository.findById(notification.getActorUserId())
                .ifPresent(actor -> {
                    map.put("actorUserId", actor.getId());
                    map.put("actorUsername", actor.getUsername());
                    map.put("actorDisplayName", actor.getDisplayName());
                });
        }

        return map;
    }

    private Map<String, Object> mapMentionToResponse(MentionEntity mention) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", mention.getId());
        map.put("messageId", mention.getMessageId());
        map.put("messageType", mention.getMessageType().name());
        map.put("roomId", mention.getRoomId());
        map.put("conversationId", mention.getConversationId());
        map.put("isRead", mention.getIsRead());
        map.put("createdAt", mention.getCreatedAt().toString());

        userRepository.findById(mention.getMentionerUserId())
            .ifPresent(mentioner -> {
                map.put("mentionerUserId", mentioner.getId());
                map.put("mentionerUsername", mentioner.getUsername());
                map.put("mentionerDisplayName", mentioner.getDisplayName());
            });

        return map;
    }
}
