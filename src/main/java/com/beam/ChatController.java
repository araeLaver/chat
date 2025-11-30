package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
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

    // 이메일 인증 코드 발송
    @PostMapping("/auth/email/send-code")
    public ResponseEntity<?> sendEmailVerificationCode(@RequestBody Map<String, Object> request) {
        try {
            String email = request.get("email").toString();

            if (!isValidEmail(email)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "올바른 이메일 주소를 입력해주세요");
                return ResponseEntity.badRequest().body(error);
            }

            // 이미 가입된 이메일인지 확인
            if (userRepository.existsByEmail(email)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 가입된 이메일입니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 6자리 랜덤 인증번호 생성
            String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));

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

    // 이메일로 회원가입 완료
    @PostMapping("/auth/email/register")
    public ResponseEntity<?> completeEmailRegistration(@RequestBody Map<String, Object> request) {
        try {
            String email = request.get("email").toString();
            String displayName = request.get("displayName").toString();
            String username = request.get("username").toString();

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

    // 이메일로 로그인
    @PostMapping("/auth/email/login")
    public ResponseEntity<?> emailLogin(@RequestBody Map<String, Object> request) {
        try {
            String email = request.get("email").toString();

            UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("등록되지 않은 이메일입니다"));

            if (!user.getIsPhoneVerified() || !user.getIsActive()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "계정이 활성화되지 않았습니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 로그인 시에도 인증번호 발송 (OTP 방식)
            String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));
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

    // 이메일 로그인 인증 확인
    @PostMapping("/auth/email/login/verify")
    public ResponseEntity<?> verifyEmailLogin(@RequestBody Map<String, Object> request) {
        try {
            String email = request.get("email").toString();
            String code = request.get("verificationCode").toString();

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

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    @PostMapping("/auth/phone/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = request.get("phoneNumber").toString();

            if (!isValidKoreanPhoneNumber(phoneNumber)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "올바른 한국 휴대폰 번호를 입력해주세요");
                return ResponseEntity.badRequest().body(error);
            }

            // 이미 가입된 번호인지 확인
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "이미 가입된 휴대폰 번호입니다");
                return ResponseEntity.badRequest().body(error);
            }

            // 6자리 랜덤 인증번호 생성
            String verificationCode = String.format("%06d", (int)(Math.random() * 1000000));

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

            // TODO: 실제 SMS 발송 구현 (Naver Cloud Platform SMS API 등)
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

    @PostMapping("/auth/phone/verify")
    public ResponseEntity<?> verifyPhone(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = request.get("phoneNumber").toString();
            String code = request.get("verificationCode").toString();

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

    @PostMapping("/auth/phone/register")
    public ResponseEntity<?> completeRegistration(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = request.get("phoneNumber").toString();
            String displayName = request.get("displayName").toString();
            String username = request.get("username").toString();

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

    @PostMapping("/auth/phone/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = request.get("phoneNumber").toString();

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

    private boolean isValidKoreanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        String cleaned = phoneNumber.replaceAll("-", "");
        return cleaned.matches("^01[016789]\\d{7,8}$");
    }
}