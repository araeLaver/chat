package com.beam.controller;

import com.beam.*;
import com.beam.DeviceTokenEntity.DeviceType;
import com.beam.dto.DeviceTokenRequest;
import com.beam.dto.NotificationPreferencesRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;

/**
 * Device Controller
 *
 * <p>Manages device tokens for push notifications and notification preferences.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Devices & Notifications", description = "디바이스 토큰 및 알림 설정 API")
public class DeviceController {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "디바이스 토큰 등록", description = "푸시 알림을 위한 디바이스 토큰을 등록합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/devices")
    @Transactional
    public ResponseEntity<?> registerDeviceToken(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody DeviceTokenRequest request) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        // Check if token already exists
        Optional<DeviceTokenEntity> existingToken = deviceTokenRepository
            .findByDeviceToken(request.getDeviceToken());

        if (existingToken.isPresent()) {
            DeviceTokenEntity existing = existingToken.get();
            // If token belongs to another user, update ownership
            if (!existing.getUserId().equals(userId)) {
                existing.setUserId(userId);
            }
            existing.setIsActive(true);
            existing.setDeviceName(request.getDeviceName());
            existing.setDeviceType(DeviceType.valueOf(request.getDeviceType().toUpperCase()));
            deviceTokenRepository.save(existing);
        } else {
            DeviceTokenEntity newToken = DeviceTokenEntity.builder()
                .userId(userId)
                .deviceToken(request.getDeviceToken())
                .deviceType(DeviceType.valueOf(request.getDeviceType().toUpperCase()))
                .deviceName(request.getDeviceName())
                .isActive(true)
                .build();
            deviceTokenRepository.save(newToken);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Device token registered");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "디바이스 토큰 해제", description = "등록된 디바이스 토큰을 해제합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 해제 성공"),
        @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음")
    })
    @DeleteMapping("/devices/{deviceToken}")
    @Transactional
    public ResponseEntity<?> unregisterDeviceToken(
            @RequestHeader("Authorization") String token,
            @PathVariable String deviceToken) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        Optional<DeviceTokenEntity> existingToken = deviceTokenRepository
            .findByUserIdAndDeviceToken(userId, deviceToken);

        if (existingToken.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Device token not found");
            return ResponseEntity.status(404).body(error);
        }

        deviceTokenRepository.delete(existingToken.get());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Device token unregistered");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 디바이스 목록 조회", description = "등록된 디바이스 토큰 목록을 조회합니다")
    @GetMapping("/devices")
    public ResponseEntity<?> getDevices(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        List<DeviceTokenEntity> devices = deviceTokenRepository.findByUserId(userId);

        List<Map<String, Object>> result = devices.stream().map(device -> {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", device.getId());
            deviceMap.put("deviceType", device.getDeviceType().toString());
            deviceMap.put("deviceName", device.getDeviceName());
            deviceMap.put("isActive", device.getIsActive());
            deviceMap.put("createdAt", device.getCreatedAt() != null ? device.getCreatedAt().toString() : null);
            return deviceMap;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "알림 설정 조회", description = "알림 설정을 조회합니다")
    @GetMapping("/notifications/preferences")
    public ResponseEntity<?> getNotificationPreferences(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        NotificationPreferencesEntity preferences = notificationPreferencesRepository
            .findByUserId(userId)
            .orElseGet(() -> {
                // Create default preferences if not exists
                NotificationPreferencesEntity defaultPrefs = NotificationPreferencesEntity.builder()
                    .userId(userId)
                    .pushEnabled(true)
                    .messagePreview(true)
                    .soundEnabled(true)
                    .vibrationEnabled(true)
                    .build();
                return notificationPreferencesRepository.save(defaultPrefs);
            });

        Map<String, Object> result = new HashMap<>();
        result.put("pushEnabled", preferences.getPushEnabled());
        result.put("messagePreview", preferences.getMessagePreview());
        result.put("soundEnabled", preferences.getSoundEnabled());
        result.put("vibrationEnabled", preferences.getVibrationEnabled());
        result.put("quietHoursStart", preferences.getQuietHoursStart() != null
            ? preferences.getQuietHoursStart().toString() : null);
        result.put("quietHoursEnd", preferences.getQuietHoursEnd() != null
            ? preferences.getQuietHoursEnd().toString() : null);

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "알림 설정 수정", description = "알림 설정을 수정합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "설정 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PutMapping("/notifications/preferences")
    @Transactional
    public ResponseEntity<?> updateNotificationPreferences(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        NotificationPreferencesEntity preferences = notificationPreferencesRepository
            .findByUserId(userId)
            .orElseGet(() -> {
                NotificationPreferencesEntity newPrefs = new NotificationPreferencesEntity();
                newPrefs.setUserId(userId);
                return newPrefs;
            });

        if (request.getPushEnabled() != null) {
            preferences.setPushEnabled(request.getPushEnabled());
        }
        if (request.getMessagePreview() != null) {
            preferences.setMessagePreview(request.getMessagePreview());
        }
        if (request.getSoundEnabled() != null) {
            preferences.setSoundEnabled(request.getSoundEnabled());
        }
        if (request.getVibrationEnabled() != null) {
            preferences.setVibrationEnabled(request.getVibrationEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            preferences.setQuietHoursStart(LocalTime.parse(request.getQuietHoursStart()));
        }
        if (request.getQuietHoursEnd() != null) {
            preferences.setQuietHoursEnd(LocalTime.parse(request.getQuietHoursEnd()));
        }

        notificationPreferencesRepository.save(preferences);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification preferences updated");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "모든 디바이스 로그아웃", description = "현재 사용자의 모든 디바이스 토큰을 비활성화합니다")
    @PostMapping("/devices/logout-all")
    @Transactional
    public ResponseEntity<?> logoutAllDevices(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(jwtToken);

        deviceTokenRepository.deactivateAllByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All devices logged out");

        return ResponseEntity.ok(response);
    }
}
