package com.beam;

import com.beam.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication", description = "이메일/휴대폰 인증 및 회원가입 API")
public class ChatController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입을 위한 이메일 인증 코드를 발송합니다. 인증 코드는 5분간 유효합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공",
            content = @Content(schema = @Schema(example = "{\"success\": true, \"message\": \"인증번호가 이메일로 발송되었습니다\"}"))),
        @ApiResponse(responseCode = "400", description = "이미 가입된 이메일이거나 요청 오류",
            content = @Content(schema = @Schema(example = "{\"success\": false, \"message\": \"이미 가입된 이메일입니다\"}")))
    })
    @PostMapping("/auth/email/send-code")
    public ResponseEntity<?> sendEmailVerificationCode(@Valid @RequestBody EmailSendCodeRequest request) {
        try {
            String email = request.getEmail();

            // 이미 가입된 이메일인지 확인
            if (userRepository.existsByEmail(email)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 가입된 이메일입니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 6자리 랜덤 인증번호 생성
            String verificationCode = VerificationCodeGenerator.generate();

            // 기존 사용자가 있으면 업데이트, 없으면 생성
            Optional<UserEntity> existingUser = userRepository.findByEmail(email);
            UserEntity user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setVerificationCode(verificationCode);
                user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            } else {
                user = UserEntity.builder()
                    .email(email)
                    .username("user_" + System.currentTimeMillis())
                    .phoneNumber("temp_" + System.currentTimeMillis())
                    .displayName("사용자")
                    .password(passwordEncoder.encode("temp_password"))
                    .verificationCode(verificationCode)
                    .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5))
                    .isPhoneVerified(false)
                    .isActive(false)
                    .isOnline(false)
                    .build();
            }

            userRepository.save(user);

            // 이메일 발송
            emailService.sendVerificationEmail(email, verificationCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증번호가 이메일로 발송되었습니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "이메일 회원가입 완료", description = "이메일 인증 완료 후 사용자명과 표시명을 설정하여 회원가입을 완료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공, JWT 토큰 반환",
            content = @Content(schema = @Schema(example = "{\"success\": true, \"token\": \"jwt-token\", \"user\": {\"id\": 1, \"username\": \"user1\"}}"))),
        @ApiResponse(responseCode = "400", description = "이메일 인증 미완료 또는 중복 사용자명")
    })
    @PostMapping("/auth/email/register")
    public ResponseEntity<?> completeEmailRegistration(@Valid @RequestBody EmailRegisterRequest request) {
        try {
            String email = request.getEmail();
            String displayName = request.getDisplayName();
            String username = request.getUsername();

            UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다"));

            if (!user.getIsPhoneVerified() || !user.getIsActive()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이메일 인증을 먼저 완료해주세요");
                return ResponseEntity.badRequest().body(error);
            }

            // 중복 사용자명 확인
            if (userRepository.existsByUsername(username)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 사용중인 사용자명입니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 사용자 정보 업데이트
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setIsOnline(true);
            userRepository.save(user);

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());

            // 환영 이메일 발송
            emailService.sendWelcomeEmail(email, displayName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다");
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "이메일 로그인 요청", description = "이메일 주소로 OTP 로그인을 요청합니다. 인증 코드가 이메일로 발송됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
        @ApiResponse(responseCode = "400", description = "등록되지 않은 이메일 또는 비활성화 계정")
    })
    @PostMapping("/auth/email/login")
    public ResponseEntity<?> emailLogin(@Valid @RequestBody EmailSendCodeRequest request) {
        try {
            String email = request.getEmail();

            UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다"));

            if (!user.getIsPhoneVerified() || !user.getIsActive()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "계정이 활성화되지 않았습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 로그인 시에도 인증번호 발송 (OTP 방식)
            String verificationCode = VerificationCodeGenerator.generate();
            user.setVerificationCode(verificationCode);
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            emailService.sendVerificationEmail(email, verificationCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 인증번호가 이메일로 발송되었습니다");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "이메일 로그인 인증 확인", description = "이메일로 발송된 인증 코드를 확인하고 로그인을 완료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
        @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료")
    })
    @PostMapping("/auth/email/login/verify")
    public ResponseEntity<?> verifyEmailLogin(@Valid @RequestBody EmailVerifyRequest request) {
        try {
            String email = request.getEmail();
            String code = request.getVerificationCode();

            UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다"));

            // 인증번호 확인
            if (!code.equals(user.getVerificationCode())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "인증번호가 일치하지 않습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 인증번호 만료 확인
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "인증번호가 만료되었습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 로그인 처리
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            user.setIsOnline(true);
            userRepository.save(user);

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "휴대폰 인증 코드 발송", description = "회원가입을 위한 휴대폰 인증 코드를 발송합니다. (현재 SMS 미구현)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
        @ApiResponse(responseCode = "400", description = "이미 가입된 휴대폰 번호")
    })
    @PostMapping("/auth/phone/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody PhoneSendCodeRequest request) {
        try {
            String phoneNumber = request.getPhoneNumber().replaceAll("-", "");

            // 이미 가입된 번호인지 확인
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 가입된 휴대폰 번호입니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 6자리 랜덤 인증번호 생성
            String verificationCode = VerificationCodeGenerator.generate();

            // 기존 사용자가 있으면 업데이트, 없으면 생성
            Optional<UserEntity> existingUser = userRepository.findByPhoneNumber(phoneNumber);
            UserEntity user;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setVerificationCode(verificationCode);
                user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            } else {
                user = UserEntity.builder()
                    .phoneNumber(phoneNumber)
                    .username("user_" + phoneNumber.substring(phoneNumber.length() - 4))
                    .displayName("사용자")
                    .password(passwordEncoder.encode("temp_password"))
                    .verificationCode(verificationCode)
                    .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5))
                    .isPhoneVerified(false)
                    .isActive(false)
                    .isOnline(false)
                    .build();
            }

            userRepository.save(user);

            // Note: SMS integration pending - email verification recommended
            // smsService.sendVerificationCode(phoneNumber, verificationCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "인증번호가 발송되었습니다 (SMS 미구현 - 이메일 인증을 사용해주세요)");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "휴대폰 인증 확인", description = "발송된 인증 코드를 확인하여 휴대폰 인증을 완료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 성공"),
        @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료")
    })
    @PostMapping("/auth/phone/verify")
    public ResponseEntity<?> verifyPhone(@Valid @RequestBody PhoneVerifyRequest request) {
        try {
            String phoneNumber = request.getPhoneNumber().replaceAll("-", "");
            String code = request.getVerificationCode();

            UserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 휴대폰 번호입니다"));

            // 인증번호 확인
            if (!code.equals(user.getVerificationCode())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "인증번호가 일치하지 않습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 인증번호 만료 확인
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "인증번호가 만료되었습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 인증 완료 처리
            user.setIsPhoneVerified(true);
            user.setIsActive(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "핸드폰 인증이 완료되었습니다");
            response.put("phoneNumber", phoneNumber);
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "휴대폰 회원가입 완료", description = "휴대폰 인증 완료 후 사용자명과 표시명을 설정하여 회원가입을 완료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공, JWT 토큰 반환"),
        @ApiResponse(responseCode = "400", description = "휴대폰 인증 미완료 또는 중복 사용자명")
    })
    @PostMapping("/auth/phone/register")
    public ResponseEntity<?> completeRegistration(@Valid @RequestBody PhoneRegisterRequest request) {
        try {
            String phoneNumber = request.getPhoneNumber().replaceAll("-", "");
            String displayName = request.getDisplayName();
            String username = request.getUsername();

            UserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 휴대폰 번호입니다"));

            if (!user.getIsPhoneVerified()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "핸드폰 인증을 먼저 완료해주세요");
                return ResponseEntity.badRequest().body(error);
            }

            // 중복 사용자명 확인
            if (userRepository.existsByUsername(username)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 사용중인 사용자명입니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 사용자 정보 업데이트
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setIsOnline(true);
            userRepository.save(user);

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다");
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "phoneNumber", user.getPhoneNumber()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "휴대폰 로그인", description = "등록된 휴대폰 번호로 로그인합니다. 인증된 사용자에게 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
        @ApiResponse(responseCode = "400", description = "등록되지 않은 번호 또는 비활성화 계정")
    })
    @PostMapping("/auth/phone/login")
    public ResponseEntity<?> login(@Valid @RequestBody PhoneSendCodeRequest request) {
        try {
            String phoneNumber = request.getPhoneNumber().replaceAll("-", "");

            UserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 휴대폰 번호입니다"));

            if (!user.getIsPhoneVerified() || !user.getIsActive()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "계정이 활성화되지 않았습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 사용자 온라인 상태 업데이트
            user.setIsOnline(true);
            userRepository.save(user);

            // JWT 토큰 생성
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "phoneNumber", user.getPhoneNumber()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}