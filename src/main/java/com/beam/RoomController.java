package com.beam;

import com.beam.dto.*;
import com.beam.util.AuthUtil;
import com.beam.util.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Room Controller
 *
 * <p>Manages group chat rooms including creation, members, and messages.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/rooms")
@Tag(name = "Rooms", description = "그룹 채팅방 관리 API")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "채팅방 생성", description = "새로운 그룹 채팅방을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<?> createRoom(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateRoomRequest request) {
        Long userId = AuthUtil.extractUserId(token, jwtUtil);

        RoomEntity.RoomType roomType = RoomEntity.RoomType.valueOf(request.getRoomType());

        RoomEntity room = roomService.createRoom(
            userId,
            request.getRoomName(),
            request.getDescription(),
            roomType,
            request.getMaxMembers()
        );

        return ResponseEntity.ok(ResponseHelper.builder()
            .message("방이 생성되었습니다")
            .put("roomId", room.getId())
            .put("roomName", room.getRoomName())
            .put("roomType", room.getRoomType().toString())
            .build());
    }


    @Operation(summary = "채팅방 정보 수정", description = "채팅방의 이름, 설명, 최대 인원을 수정합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "권한 없음 또는 잘못된 요청")
    })
    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateRoom(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request) {
        Long userId = AuthUtil.extractUserId(token, jwtUtil);

        RoomEntity room = roomService.updateRoom(
            roomId,
            userId,
            request.getRoomName(),
            request.getDescription(),
            request.getMaxMembers()
        );

        return ResponseEntity.ok(ResponseHelper.builder()
            .message("Room updated successfully")
            .put("roomId", room.getId())
            .put("roomName", room.getRoomName())
            .build());
    }

    @Operation(summary = "채팅방 삭제", description = "채팅방을 삭제합니다 (방장만 가능)")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            roomService.deleteRoom(roomId, userId);
            return ResponseEntity.ok(ResponseHelper.success("Room deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "멤버 추가", description = "채팅방에 새로운 멤버를 추가합니다")
    @PostMapping("/{roomId}/members")
    public ResponseEntity<?> addMember(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Valid @RequestBody AddMemberRequest request) {
        Long inviterId = AuthUtil.extractUserId(token, jwtUtil);
        roomService.addMember(roomId, request.getUserId(), inviterId);
        return ResponseEntity.ok(ResponseHelper.success("Member added successfully"));
    }

    @Operation(summary = "멤버 제거", description = "채팅방에서 멤버를 강제 퇴장시킵니다 (관리자/방장만 가능)")
    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "제거할 사용자 ID") @PathVariable Long userId) {
        try {
            Long removerId = AuthUtil.extractUserId(token, jwtUtil);
            roomService.removeMember(roomId, userId, removerId);
            return ResponseEntity.ok(ResponseHelper.success("Member removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다")
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            roomService.leaveRoom(roomId, userId);
            return ResponseEntity.ok(ResponseHelper.success("Left room successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "메시지 전송", description = "채팅방에 메시지를 전송합니다")
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<?> sendMessage(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request) {
        Long userId = AuthUtil.extractUserId(token, jwtUtil);

        GroupMessageEntity.MessageType messageType = GroupMessageEntity.MessageType.valueOf(request.getMessageType());

        GroupMessageEntity message = roomService.sendMessage(roomId, userId, request.getContent(), messageType);

        return ResponseEntity.ok(ResponseHelper.builder()
            .put("messageId", message.getId())
            .put("content", message.getContent())
            .put("timestamp", message.getTimestamp().toString())
            .build());
    }

    @Operation(summary = "메시지 조회", description = "채팅방의 최근 메시지 100개를 조회합니다")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getRoomMessages(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<GroupMessageEntity> messages = roomService.getRoomMessages(roomId, userId);

            List<Map<String, Object>> result = messages.stream().map(msg -> {
                Optional<UserEntity> senderOpt = userRepository.findById(msg.getSenderId());

                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("senderId", msg.getSenderId());
                msgMap.put("senderName", senderOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                msgMap.put("content", msg.getContent());
                msgMap.put("messageType", msg.getMessageType().toString());
                msgMap.put("timestamp", msg.getTimestamp().toString());
                msgMap.put("readCount", msg.getReadCount());
                msgMap.put("isMine", msg.getSenderId().equals(userId));

                return msgMap;
            }).collect(Collectors.toList());

            Collections.reverse(result);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "메시지 읽음 처리", description = "채팅방의 메시지를 읽음으로 표시합니다")
    @PostMapping("/{roomId}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            roomService.markAsRead(roomId, userId);
            return ResponseEntity.ok(ResponseHelper.success("Messages marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "내 채팅방 목록", description = "내가 참여 중인 모든 채팅방을 조회합니다")
    @GetMapping("/my-rooms")
    public ResponseEntity<?> getMyRooms(@RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<RoomEntity> rooms = roomService.getUserRooms(userId);

            List<Map<String, Object>> result = rooms.stream().map(room -> {
                Optional<RoomMemberEntity> membershipOpt = roomMemberRepository
                    .findByRoomIdAndUserIdAndIsActiveTrue(room.getId(), userId);

                Map<String, Object> roomMap = new HashMap<>();
                roomMap.put("roomId", room.getId());
                roomMap.put("roomName", room.getRoomName());
                roomMap.put("description", room.getDescription());
                roomMap.put("roomType", room.getRoomType().toString());
                roomMap.put("currentMembers", room.getCurrentMembers());
                roomMap.put("maxMembers", room.getMaxMembers());
                roomMap.put("lastMessage", room.getLastMessage());
                roomMap.put("lastMessageTime", room.getLastMessageTime() != null
                    ? room.getLastMessageTime().toString() : null);
                roomMap.put("unreadCount", membershipOpt.map(RoomMemberEntity::getUnreadCount).orElse(0));
                roomMap.put("myRole", membershipOpt.map(m -> m.getRole().toString()).orElse("MEMBER"));

                return roomMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "채팅방 멤버 조회", description = "채팅방의 모든 멤버를 조회합니다")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<?> getRoomMembers(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<RoomMemberEntity> members = roomService.getRoomMembers(roomId, userId);

            List<Map<String, Object>> result = members.stream().map(member -> {
                Optional<UserEntity> userOpt = userRepository.findById(member.getUserId());

                Map<String, Object> memberMap = new HashMap<>();
                memberMap.put("userId", member.getUserId());
                memberMap.put("username", userOpt.map(UserEntity::getUsername).orElse("Unknown"));
                memberMap.put("displayName", userOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                memberMap.put("role", member.getRole().toString());
                memberMap.put("isOnline", userOpt.map(UserEntity::getIsOnline).orElse(false));
                memberMap.put("joinedAt", member.getJoinedAt().toString());

                return memberMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "채팅방 검색", description = "키워드로 채팅방을 검색합니다")
    @GetMapping("/search")
    public ResponseEntity<?> searchRooms(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "검색 키워드") @RequestParam String keyword) {
        try {
            List<RoomEntity> rooms = roomService.searchRooms(keyword);

            List<Map<String, Object>> result = rooms.stream().map(room -> {
                Map<String, Object> roomMap = new HashMap<>();
                roomMap.put("roomId", room.getId());
                roomMap.put("roomName", room.getRoomName());
                roomMap.put("description", room.getDescription());
                roomMap.put("roomType", room.getRoomType().toString());
                roomMap.put("currentMembers", room.getCurrentMembers());
                roomMap.put("maxMembers", room.getMaxMembers());

                return roomMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }
}
