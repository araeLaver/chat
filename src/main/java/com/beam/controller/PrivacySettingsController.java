package com.beam.controller;

import com.beam.JwtUtil;
import com.beam.UserEntity;
import com.beam.UserRepository;
import com.beam.dto.PrivacySettingsRequest;
import com.beam.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 프라이버시 설정 컨트롤러
 */
@RestController
@RequestMapping("/api/privacy")
@Tag(name = "Privacy", description = "프라이버시 설정 API")
public class PrivacySettingsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "프라이버시 설정 조회", description = "현재 사용자의 프라이버시 설정을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/settings")
    public ResponseEntity<?> getPrivacySettings(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = AuthUtil.extractUserId(authHeader, jwtUtil);
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> settings = new HashMap<>();
            settings.put("ghostMode", user.getGhostMode());
            settings.put("disableIpLogging", user.getDisableIpLogging());
            settings.put("autoTranslate", user.getAutoTranslate());
            settings.put("preferredLanguage", user.getPreferredLanguage());
            settings.put("isAnonymous", user.getIsAnonymous());

            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "프라이버시 설정 업데이트", description = "사용자의 프라이버시 설정을 업데이트합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 업데이트 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/settings")
    public ResponseEntity<?> updatePrivacySettings(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PrivacySettingsRequest request) {
        try {
            Long userId = AuthUtil.extractUserId(authHeader, jwtUtil);
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (request.getGhostMode() != null) {
                user.setGhostMode(request.getGhostMode());
            }
            if (request.getDisableIpLogging() != null) {
                user.setDisableIpLogging(request.getDisableIpLogging());
            }
            if (request.getAutoTranslate() != null) {
                user.setAutoTranslate(request.getAutoTranslate());
            }
            if (request.getPreferredLanguage() != null) {
                user.setPreferredLanguage(request.getPreferredLanguage());
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Privacy settings updated");
            response.put("ghostMode", user.getGhostMode());
            response.put("disableIpLogging", user.getDisableIpLogging());
            response.put("autoTranslate", user.getAutoTranslate());
            response.put("preferredLanguage", user.getPreferredLanguage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "고스트 모드 토글", description = "고스트 모드를 켜거나 끕니다 (온라인 상태 숨김)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "고스트 모드 토글 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/ghost-mode")
    public ResponseEntity<?> toggleGhostMode(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = AuthUtil.extractUserId(authHeader, jwtUtil);
            UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            boolean newGhostMode = !Boolean.TRUE.equals(user.getGhostMode());
            user.setGhostMode(newGhostMode);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ghostMode", newGhostMode);
            response.put("message", newGhostMode ? "Ghost mode enabled" : "Ghost mode disabled");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "사용자 상태 조회 (프라이버시 적용)", description = "다른 사용자의 상태를 조회합니다. 고스트 모드 적용")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상태 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/user-status/{targetUserId}")
    public ResponseEntity<?> getUserStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long targetUserId) {
        try {
            UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> status = new HashMap<>();
            status.put("userId", targetUser.getId());
            status.put("displayName", targetUser.getDisplayName());
            status.put("profileImage", targetUser.getProfileImage());

            // 고스트 모드가 활성화된 경우 온라인 상태 숨김
            if (Boolean.TRUE.equals(targetUser.getGhostMode())) {
                status.put("isOnline", false);
                status.put("lastSeen", null);
            } else {
                status.put("isOnline", targetUser.getIsOnline());
                status.put("lastSeen", targetUser.getLastSeen());
            }

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
