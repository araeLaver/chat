package com.beam;

import com.beam.PinnedMessageEntity.ContextType;
import com.beam.PinnedMessageEntity.MessageType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pins")
@Tag(name = "Pinned Messages", description = "메시지 고정 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class PinController {

    @Autowired
    private PinService pinService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "메시지 고정", description = "메시지를 채팅방이나 대화에 고정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "고정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 고정됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<?> pinMessage(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Long messageId = Long.valueOf(request.get("messageId").toString());
            MessageType messageType = MessageType.valueOf(request.get("messageType").toString().toUpperCase());
            ContextType contextType = ContextType.valueOf(request.get("contextType").toString().toUpperCase());
            String contextId = request.get("contextId").toString();
            String note = request.get("note") != null ? request.get("note").toString() : null;

            PinnedMessageEntity pinned = pinService.pinMessage(
                messageId, messageType, contextType, contextId, userId, note);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("success", true)
                .put("pinnedMessage", mapPinnedMessageToResponse(pinned))
                .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "메시지 고정 해제", description = "고정된 메시지를 해제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "고정 해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{messageType}/{messageId}")
    public ResponseEntity<?> unpinMessage(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "메시지 타입", required = true)
            @PathVariable String messageType,
            @Parameter(description = "메시지 ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "컨텍스트 타입", required = true)
            @RequestParam String contextType,
            @Parameter(description = "컨텍스트 ID (방 ID 또는 대화 ID)", required = true)
            @RequestParam String contextId) {
        try {
            AuthUtil.extractUserId(token, jwtUtil);

            pinService.unpinMessage(
                messageId,
                MessageType.valueOf(messageType.toUpperCase()),
                ContextType.valueOf(contextType.toUpperCase()),
                contextId);

            return ResponseEntity.ok(ResponseHelper.success("Message unpinned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "고정된 메시지 목록 조회", description = "특정 컨텍스트의 고정된 메시지 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "고정 메시지 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/{contextType}/{contextId}")
    public ResponseEntity<?> getPinnedMessages(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "컨텍스트 타입 (ROOM 또는 CONVERSATION)", required = true)
            @PathVariable String contextType,
            @Parameter(description = "컨텍스트 ID (방 ID 또는 대화 ID)", required = true)
            @PathVariable String contextId) {
        try {
            AuthUtil.extractUserId(token, jwtUtil);

            List<PinnedMessageEntity> pinnedMessages = pinService.getPinnedMessages(
                ContextType.valueOf(contextType.toUpperCase()), contextId);

            List<Map<String, Object>> result = pinnedMessages.stream()
                .map(this::mapPinnedMessageToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("pinnedMessages", result)
                .put("count", result.size())
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "메시지 고정 여부 확인", description = "특정 메시지가 고정되어 있는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "고정 여부 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/check/{messageType}/{messageId}")
    public ResponseEntity<?> checkPinned(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "메시지 타입", required = true)
            @PathVariable String messageType,
            @Parameter(description = "메시지 ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "컨텍스트 타입", required = true)
            @RequestParam String contextType,
            @Parameter(description = "컨텍스트 ID", required = true)
            @RequestParam String contextId) {
        try {
            AuthUtil.extractUserId(token, jwtUtil);

            boolean isPinned = pinService.isPinned(
                messageId,
                MessageType.valueOf(messageType.toUpperCase()),
                ContextType.valueOf(contextType.toUpperCase()),
                contextId);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("isPinned", isPinned)
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "고정 순서 변경", description = "고정된 메시지의 순서를 변경합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "순서 변경 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/{pinnedMessageId}/order")
    public ResponseEntity<?> updatePinOrder(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "고정 메시지 ID", required = true)
            @PathVariable Long pinnedMessageId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            Integer newOrder = Integer.valueOf(request.get("order").toString());

            pinService.updatePinOrder(pinnedMessageId, newOrder, userId);

            return ResponseEntity.ok(ResponseHelper.success("Pin order updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    private Map<String, Object> mapPinnedMessageToResponse(PinnedMessageEntity pinned) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", pinned.getId());
        map.put("messageId", pinned.getMessageId());
        map.put("messageType", pinned.getMessageType().name());
        map.put("contextType", pinned.getContextType().name());
        map.put("contextId", pinned.getContextId());
        map.put("pinOrder", pinned.getPinOrder());
        map.put("note", pinned.getNote());
        map.put("createdAt", pinned.getCreatedAt().toString());

        userRepository.findById(pinned.getPinnedByUserId())
            .ifPresent(user -> {
                map.put("pinnedByUserId", user.getId());
                map.put("pinnedByUsername", user.getUsername());
                map.put("pinnedByDisplayName", user.getDisplayName());
            });

        return map;
    }
}
