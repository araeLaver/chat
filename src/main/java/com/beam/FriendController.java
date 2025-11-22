package com.beam;

import com.beam.dto.*;
import io.swagger.v3.oas.annotations.Operation;
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
 * Friend Controller
 *
 * <p>Manages friend relationships including requests, acceptance, and blocking.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/friends")
@Tag(name = "Friends", description = "친구 관리 API")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냅니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "친구 요청 전송 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 친구 관계 존재")
    })
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody FriendRequestDto request) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        FriendEntity friendRequest = friendService.sendFriendRequest(userId, request.getFriendId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Friend request sent");
        response.put("requestId", friendRequest.getId());
        response.put("status", friendRequest.getStatus().toString());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "친구 요청 수락 성공"),
        @ApiResponse(responseCode = "400", description = "요청을 찾을 수 없거나 유효하지 않음")
    })
    @PostMapping("/accept")
    public ResponseEntity<?> acceptFriendRequest(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AcceptFriendRequestDto request) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        FriendEntity friendship = friendService.acceptFriendRequest(userId, request.getRequesterId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Friend request accepted");
        response.put("friendshipId", friendship.getId());
        response.put("status", friendship.getStatus().toString());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다")
    @PostMapping("/reject")
    public ResponseEntity<?> rejectFriendRequest(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AcceptFriendRequestDto request) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        friendService.rejectFriendRequest(userId, request.getRequesterId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Friend request rejected");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다")
    @PostMapping("/block")
    public ResponseEntity<?> blockUser(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody BlockUserRequestDto request) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        friendService.blockUser(userId, request.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User blocked");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다")
    @DeleteMapping("/unfriend/{friendId}")
    public ResponseEntity<?> unfriend(
            @RequestHeader("Authorization") String token,
            @PathVariable Long friendId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            friendService.unfriend(userId, friendId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Friend removed");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "친구 목록 조회", description = "내 친구 목록을 조회합니다")
    @GetMapping("/list")
    public ResponseEntity<?> getFriendList(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            List<FriendEntity> friendships = friendService.getFriendList(userId);

            List<Map<String, Object>> result = friendships.stream().map(friendship -> {
                Long friendId = friendship.getUserId().equals(userId)
                    ? friendship.getFriendId()
                    : friendship.getUserId();

                Optional<UserEntity> friendOpt = userRepository.findById(friendId);

                Map<String, Object> friendMap = new HashMap<>();
                friendMap.put("friendId", friendId);
                friendMap.put("username", friendOpt.map(UserEntity::getUsername).orElse("Unknown"));
                friendMap.put("displayName", friendOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                friendMap.put("phoneNumber", friendOpt.map(UserEntity::getPhoneNumber).orElse(null));
                friendMap.put("isOnline", friendOpt.map(UserEntity::getIsOnline).orElse(false));
                friendMap.put("lastSeen", friendOpt.map(u -> u.getLastSeen() != null ? u.getLastSeen().toString() : null).orElse(null));
                friendMap.put("friendsSince", friendship.getAcceptedAt() != null ? friendship.getAcceptedAt().toString() : null);

                return friendMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "받은 친구 요청 조회", description = "내가 받은 친구 요청 목록을 조회합니다")
    @GetMapping("/requests/received")
    public ResponseEntity<?> getPendingRequestsReceived(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            List<FriendEntity> requests = friendService.getPendingRequestsReceived(userId);

            List<Map<String, Object>> result = requests.stream().map(request -> {
                Long requesterId = request.getUserId();
                Optional<UserEntity> requesterOpt = userRepository.findById(requesterId);

                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("requestId", request.getId());
                requestMap.put("requesterId", requesterId);
                requestMap.put("username", requesterOpt.map(UserEntity::getUsername).orElse("Unknown"));
                requestMap.put("displayName", requesterOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                requestMap.put("requestedAt", request.getRequestedAt() != null ? request.getRequestedAt().toString() : null);

                return requestMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "보낸 친구 요청 조회", description = "내가 보낸 친구 요청 목록을 조회합니다")
    @GetMapping("/requests/sent")
    public ResponseEntity<?> getPendingRequestsSent(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            List<FriendEntity> requests = friendService.getPendingRequestsSent(userId);

            List<Map<String, Object>> result = requests.stream().map(request -> {
                Long friendId = request.getFriendId();
                Optional<UserEntity> friendOpt = userRepository.findById(friendId);

                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("requestId", request.getId());
                requestMap.put("friendId", friendId);
                requestMap.put("username", friendOpt.map(UserEntity::getUsername).orElse("Unknown"));
                requestMap.put("displayName", friendOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                requestMap.put("requestedAt", request.getRequestedAt() != null ? request.getRequestedAt().toString() : null);

                return requestMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "친구 요청 개수 조회", description = "받은 친구 요청의 개수를 조회합니다")
    @GetMapping("/requests/count")
    public ResponseEntity<?> getPendingRequestCount(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            Integer count = friendService.getPendingRequestCount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "사용자 검색", description = "사용자명 또는 전화번호로 사용자를 검색합니다")
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestHeader("Authorization") String token,
            @RequestParam String query) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            List<UserEntity> users = friendService.searchUsers(query);

            List<Map<String, Object>> result = users.stream()
                .filter(user -> !user.getId().equals(userId))
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("displayName", user.getDisplayName());
                    userMap.put("phoneNumber", user.getPhoneNumber());
                    userMap.put("isOnline", user.getIsOnline());

                    return userMap;
                }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
