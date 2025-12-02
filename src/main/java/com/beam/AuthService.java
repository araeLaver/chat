package com.beam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = "user_" + System.currentTimeMillis();
        }

        String verificationCode = String.format("%06d", new Random().nextInt(999999));

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phoneNumber(phoneNumber)
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .isActive(false)
                .isPhoneVerified(false)
                .build();

        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        user = userRepository.save(user);

        emailService.sendVerificationEmail(request.getEmail(), verificationCode);

        return AuthResponse.builder()
                .username(user.getUsername())
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .message("Verification code sent to email")
                .emailVerified(false)
                .build();
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        user.setLastSeen(LocalDateTime.now());
        user.setIsOnline(true);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .message("Login successful")
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setIsOnline(false);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Transactional
    public String sendVerificationCode(String phoneNumber) {
        UserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Phone number not found"));

        String code = String.format("%06d", new Random().nextInt(999999));

        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        logger.info("📱 인증 코드 전송 - 전화번호: {}, 코드: {}", phoneNumber, code);

        return "Verification code sent";
    }

    @Transactional
    public boolean verifyPhoneNumber(String phoneNumber, String code) {
        UserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Phone number not found"));

        if (user.getVerificationCode() == null ||
            !user.getVerificationCode().equals(code)) {
            return false;
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setIsPhoneVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    @Transactional
    public AuthResponse verifyEmail(String email, String code) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (user.getVerificationCode() == null ||
            !user.getVerificationCode().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        user.setIsActive(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .emailVerified(true)
                .message("Email verified successfully")
                .build();
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (user.getIsActive()) {
            throw new RuntimeException("Email already verified");
        }

        String verificationCode = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        emailService.sendVerificationEmail(email, verificationCode);
    }
}