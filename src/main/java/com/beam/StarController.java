package com.beam;

import com.beam.StarredMessageEntity.MessageType;
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
@RequestMapping("/api/stars")
@Tag(name = "Starred Messages", description = "메시지 별표 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class StarController {

    @Autowired
    private StarService starService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "메시지 별표", description = "메시지에 별표를 추가합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "별표 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 별표됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<?> starMessage(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Long messageId = Long.valueOf(request.get("messageId").toString());
            MessageType messageType = MessageType.valueOf(request.get("messageType").toString().toUpperCase());
            String roomId = request.get("roomId") != null ? request.get("roomId").toString() : null;
            String conversationId = request.get("conversationId") != null ? request.get("conversationId").toString() : null;
            String note = request.get("note") != null ? request.get("note").toString() : null;

            StarredMessageEntity starred = starService.starMessage(
                userId, messageId, messageType, roomId, conversationId, note);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("success", true)
                .put("starredMessage", mapStarredMessageToResponse(starred))
                .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "메시지 별표 해제", description = "메시지의 별표를 제거합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "별표 해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{messageType}/{messageId}")
    public ResponseEntity<?> unstarMessage(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "메시지 타입", required = true)
            @PathVariable String messageType,
            @Parameter(description = "메시지 ID", required = true)
            @PathVariable Long messageId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            starService.unstarMessage(userId, messageId, MessageType.valueOf(messageType.toUpperCase()));

            return ResponseEntity.ok(ResponseHelper.success("Message unstarred successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "별표 토글", description = "메시지의 별표 상태를 토글합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토글 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleStar(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Long messageId = Long.valueOf(request.get("messageId").toString());
            MessageType messageType = MessageType.valueOf(request.get("messageType").toString().toUpperCase());
            String roomId = request.get("roomId") != null ? request.get("roomId").toString() : null;
            String conversationId = request.get("conversationId") != null ? request.get("conversationId").toString() : null;
            String note = request.get("note") != null ? request.get("note").toString() : null;

            StarredMessageEntity result = starService.toggleStar(
                userId, messageId, messageType, roomId, conversationId, note);

            if (result != null) {
                return ResponseEntity.ok(ResponseHelper.builder()
                    .put("starred", true)
                    .put("starredMessage", mapStarredMessageToResponse(result))
                    .build());
            } else {
                return ResponseEntity.ok(ResponseHelper.builder()
                    .put("starred", false)
                    .build());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "내 별표 메시지 목록", description = "사용자의 별표된 메시지 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "별표 메시지 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<?> getMyStarredMessages(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 50)")
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Page<StarredMessageEntity> starredMessages = starService.getStarredMessages(userId, page, size);

            List<Map<String, Object>> result = starredMessages.getContent().stream()
                .map(this::mapStarredMessageToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("starredMessages", result)
                .put("totalPages", starredMessages.getTotalPages())
                .put("totalElements", starredMessages.getTotalElements())
                .put("currentPage", page)
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "채팅방 별표 메시지", description = "특정 채팅방의 별표된 메시지를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "별표 메시지 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getStarredMessagesInRoom(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable String roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<StarredMessageEntity> starredMessages = starService.getStarredMessagesInRoom(userId, roomId);

            List<Map<String, Object>> result = starredMessages.stream()
                .map(this::mapStarredMessageToResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "별표 여부 확인", description = "특정 메시지가 별표되어 있는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "별표 여부 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/check/{messageType}/{messageId}")
    public ResponseEntity<?> checkStarred(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "메시지 타입", required = true)
            @PathVariable String messageType,
            @Parameter(description = "메시지 ID", required = true)
            @PathVariable Long messageId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            boolean isStarred = starService.isStarred(
                userId, messageId, MessageType.valueOf(messageType.toUpperCase()));

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("isStarred", isStarred)
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "별표 메시지 수", description = "사용자의 별표 메시지 개수를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "별표 수 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/count")
    public ResponseEntity<?> getStarredCount(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Integer count = starService.getStarredCount(userId);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("count", count)
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "별표 메모 수정", description = "별표 메시지의 메모를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메모 수정 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/{starId}/note")
    public ResponseEntity<?> updateStarNote(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "별표 ID", required = true)
            @PathVariable Long starId,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            String note = request.get("note") != null ? request.get("note").toString() : null;

            starService.updateStarNote(starId, userId, note);

            return ResponseEntity.ok(ResponseHelper.success("Star note updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    private Map<String, Object> mapStarredMessageToResponse(StarredMessageEntity starred) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", starred.getId());
        map.put("messageId", starred.getMessageId());
        map.put("messageType", starred.getMessageType().name());
        map.put("roomId", starred.getRoomId());
        map.put("conversationId", starred.getConversationId());
        map.put("note", starred.getNote());
        map.put("createdAt", starred.getCreatedAt().toString());
        return map;
    }
}
