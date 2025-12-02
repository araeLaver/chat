package com.beam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 *
 * <p>Handles user authentication, registration, and phone verification.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "사용자 인증 및 회원가입 API")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 사용자명 등)")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공",
                content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 사용자명 또는 비밀번호")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "로그아웃", description = "사용자 로그아웃 처리")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 토큰")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "게스트 로그인", description = "임시 게스트 계정을 생성하고 JWT 토큰을 발급합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게스트 로그인 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/guest")
    public ResponseEntity<?> guestLogin() {
        try {
            // 랜덤 게스트 사용자명 생성
            String guestUsername = "guest_" + System.currentTimeMillis();
            String guestDisplayName = "게스트" + (int)(Math.random() * 10000);

            // 게스트 사용자 생성
            UserEntity guestUser = UserEntity.builder()
                .username(guestUsername)
                .displayName(guestDisplayName)
                .password(passwordEncoder.encode("guest_" + System.currentTimeMillis()))
                .phoneNumber("guest_" + System.currentTimeMillis())
                .isPhoneVerified(true)
                .isActive(true)
                .isOnline(true)
                .build();

            userRepository.save(guestUser);

            // 기본 "일반 채팅" 방 찾기 또는 생성
            RoomEntity defaultRoom = roomRepository.findByRoomNameAndRoomType("일반 채팅", RoomEntity.RoomType.PUBLIC)
                .orElseGet(() -> {
                    RoomEntity newRoom = roomService.createRoom(
                        guestUser.getId(),
                        "일반 채팅",
                        "누구나 참여 가능한 일반 채팅방입니다",
                        RoomEntity.RoomType.PUBLIC,
                        1000
                    );
                    return newRoom;
                });

            // 사용자를 방에 추가 (이미 있으면 무시)
            try {
                roomService.addMember(defaultRoom.getId(), guestUser.getId(), guestUser.getId());
            } catch (Exception e) {
                // 이미 멤버인 경우 무시
            }

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(guestUser.getUsername(), guestUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게스트로 로그인되었습니다");
            response.put("token", token);
            response.put("user", Map.of(
                "id", guestUser.getId(),
                "username", guestUser.getUsername(),
                "displayName", guestUser.getDisplayName()
            ));
            response.put("defaultRoomId", defaultRoom.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "게스트 로그인 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @Operation(summary = "인증 코드 전송", description = "전화번호로 인증 코드를 전송합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 전송 성공"),
        @ApiResponse(responseCode = "400", description = "전화번호를 찾을 수 없음")
    })
    @PostMapping("/verify/send")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            String message = authService.sendVerificationCode(phoneNumber);
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "전화번호 인증", description = "인증 코드로 전화번호를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "전화번호 인증 성공"),
        @ApiResponse(responseCode = "400", description = "잘못되거나 만료된 인증 코드")
    })
    @PostMapping("/verify/confirm")
    public ResponseEntity<?> verifyPhoneNumber(@RequestBody Map<String, String> request) {
        try {
            String phoneNumber = request.get("phoneNumber");
            String code = request.get("code");
            boolean verified = authService.verifyPhoneNumber(phoneNumber, code);

            Map<String, Object> response = new HashMap<>();
            if (verified) {
                response.put("message", "Phone number verified successfully");
                response.put("verified", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Invalid or expired verification code");
                response.put("verified", false);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "이메일 인증 확인", description = "이메일로 받은 인증 코드를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
        @ApiResponse(responseCode = "400", description = "잘못되거나 만료된 인증 코드")
    })
    @PostMapping("/email/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            AuthResponse response = authService.verifyEmail(email, code);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "인증 코드 재발송", description = "이메일 인증 코드를 다시 발송합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 재발송 성공"),
        @ApiResponse(responseCode = "400", description = "이메일을 찾을 수 없음")
    })
    @PostMapping("/email/resend")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            authService.resendVerificationEmail(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Verification code sent");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}