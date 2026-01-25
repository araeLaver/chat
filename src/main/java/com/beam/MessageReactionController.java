package com.beam;

import com.beam.MessageReactionEntity.MessageType;
import com.beam.MessageReactionService.ReactionSummary;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reactions")
@Tag(name = "Reactions", description = "메시지 이모지 리액션 API")
@SecurityRequirement(name = "bearerAuth")
public class MessageReactionController {

    @Autowired
    private MessageReactionService reactionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "리액션 추가", description = "메시지에 이모지 리액션을 추가합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "리액션 추가 성공"),
        @ApiResponse(responseCode = "400", description = "이미 동일한 리액션이 존재함"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<?> addReaction(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Long messageId = Long.valueOf(request.get("messageId").toString());
            String messageTypeStr = request.get("messageType").toString();
            String emoji = request.get("emoji").toString();

            MessageType messageType = MessageType.valueOf(messageTypeStr.toUpperCase());

            MessageReactionEntity reaction = reactionService.addReaction(
                messageId, messageType, userId, emoji);

            return ResponseEntity.ok(ResponseHelper.builder()
                .message("Reaction added")
                .put("reactionId", reaction.getId())
                .put("emoji", reaction.getEmoji())
                .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error("Invalid message type"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "리액션 삭제", description = "메시지에서 이모지 리액션을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "리액션 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping
    public ResponseEntity<?> removeReaction(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Long messageId = Long.valueOf(request.get("messageId").toString());
            String messageTypeStr = request.get("messageType").toString();
            String emoji = request.get("emoji").toString();

            MessageType messageType = MessageType.valueOf(messageTypeStr.toUpperCase());

            reactionService.removeReaction(messageId, messageType, userId, emoji);

            return ResponseEntity.ok(ResponseHelper.success("Reaction removed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error("Invalid message type"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "리액션 토글", description = "리액션을 추가하거나 제거합니다 (이미 있으면 제거, 없으면 추가).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "리액션 토글 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음")
    })
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleReaction(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            Long messageId = Long.valueOf(request.get("messageId").toString());
            String messageTypeStr = request.get("messageType").toString();
            String emoji = request.get("emoji").toString();

            MessageType messageType = MessageType.valueOf(messageTypeStr.toUpperCase());

            MessageReactionEntity reaction = reactionService.toggleReaction(
                messageId, messageType, userId, emoji);

            if (reaction != null) {
                return ResponseEntity.ok(ResponseHelper.builder()
                    .message("Reaction added")
                    .put("reactionId", reaction.getId())
                    .put("added", true)
                    .build());
            } else {
                return ResponseEntity.ok(ResponseHelper.builder()
                    .message("Reaction removed")
                    .put("added", false)
                    .build());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error("Invalid message type"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "메시지 리액션 조회", description = "특정 메시지의 모든 리액션을 이모지별로 집계하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "리액션 목록 반환")
    })
    @GetMapping("/{messageType}/{messageId}")
    public ResponseEntity<?> getReactions(
            @Parameter(description = "메시지 타입 (ROOM, DIRECT, GROUP)", required = true)
            @PathVariable String messageType,
            @Parameter(description = "메시지 ID", required = true)
            @PathVariable Long messageId) {
        try {
            MessageType type = MessageType.valueOf(messageType.toUpperCase());
            List<ReactionSummary> reactions = reactionService.getReactionSummary(messageId, type);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("messageId", messageId)
                .put("messageType", messageType)
                .put("reactions", reactions)
                .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error("Invalid message type"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "리액션 여부 확인", description = "사용자가 특정 메시지에 특정 이모지로 리액션했는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "리액션 여부 반환")
    })
    @GetMapping("/{messageType}/{messageId}/check")
    public ResponseEntity<?> hasReacted(
            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String token,
            @Parameter(description = "메시지 타입", required = true)
            @PathVariable String messageType,
            @Parameter(description = "메시지 ID", required = true)
            @PathVariable Long messageId,
            @Parameter(description = "이모지", required = true)
            @RequestParam String emoji) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            MessageType type = MessageType.valueOf(messageType.toUpperCase());

            boolean hasReacted = reactionService.hasUserReacted(messageId, type, userId, emoji);

            return ResponseEntity.ok(ResponseHelper.builder()
                .put("hasReacted", hasReacted)
                .put("emoji", emoji)
                .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseHelper.error("Invalid message type"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }
}
