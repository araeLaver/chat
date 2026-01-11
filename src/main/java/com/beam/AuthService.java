package com.beam;

import com.beam.exception.AuthenticationException;
import com.beam.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw UserException.usernameExists(request.getUsername());
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw UserException.emailExists(request.getEmail());
        }

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = "user_" + System.currentTimeMillis();
        }

        String verificationCode = VerificationCodeGenerator.generate();

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
    public AuthResponse login(AuthRequest request, String ipAddress) {
        // Rate Limiting 체크
        authRateLimiter.checkLoginAttempt(ipAddress);

        UserEntity user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            authRateLimiter.recordLoginFailure(ipAddress);
            logger.warn("로그인 실패 - IP: {}, 사용자: {}", ipAddress, request.getUsername());
            throw AuthenticationException.invalidCredentials();
        }

        if (!user.getIsActive()) {
            throw AuthenticationException.accountDisabled();
        }

        // 로그인 성공 - 실패 기록 초기화
        authRateLimiter.recordLoginSuccess(ipAddress);

        user.setLastSeen(LocalDateTime.now());
        user.setIsOnline(true);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        logger.info("로그인 성공 - 사용자: {}, IP: {}", user.getUsername(), ipAddress);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .message("Login successful")
                .build();
    }

    /**
     * 기존 login 메서드 호환성 유지
     * @deprecated Use login(AuthRequest, String) instead
     */
    @Deprecated
    @Transactional
    public AuthResponse login(AuthRequest request) {
        return login(request, "unknown");
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
                .orElseThrow(() -> UserException.notFoundByEmail(phoneNumber));

        String code = VerificationCodeGenerator.generate();

        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        logger.info("📱 인증 코드 전송 - 전화번호: {}, 코드: {}", phoneNumber, code);

        return "Verification code sent";
    }

    @Transactional
    public boolean verifyPhoneNumber(String phoneNumber, String code) {
        UserEntity user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> UserException.notFoundByEmail(phoneNumber));

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
        // Rate Limiting 체크
        authRateLimiter.checkVerifyCodeAttempt(email);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> UserException.notFoundByEmail(email));

        if (user.getVerificationCode() == null ||
            !user.getVerificationCode().equals(code)) {
            authRateLimiter.recordVerifyCodeFailure(email);
            throw AuthenticationException.verificationFailed();
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw AuthenticationException.verificationExpired();
        }

        // 성공 - Rate Limit 초기화
        authRateLimiter.recordVerifyCodeSuccess(email);

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
                .orElseThrow(() -> UserException.notFoundByEmail(email));

        if (user.getIsActive()) {
            throw new IllegalArgumentException("Email already verified");
        }

        String verificationCode = VerificationCodeGenerator.generate();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        emailService.sendVerificationEmail(email, verificationCode);
    }
}