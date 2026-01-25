package com.beam;

import com.beam.exception.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 암호화 유틸리티
 * - AES-256-GCM 암호화/복호화
 * - 키 파생 및 캐싱
 * - 보안 로깅
 */
public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);

    // 파생된 키 캐시 (성능 최적화)
    private static final Map<String, String> derivedKeyCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw EncryptionException.keyGenerationFailed(e);
        }
    }

    public static String encrypt(String plainText, String keyString) {
        try {
            byte[] key = Base64.getDecoder().decode(keyString);
            SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);

            // GCM용 IV 생성
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호화된 데이터를 결합하여 반환
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw EncryptionException.encryptionFailed(e);
        }
    }

    public static String decrypt(String encryptedText, String keyString) {
        try {
            byte[] key = Base64.getDecoder().decode(keyString);
            SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);

            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // IV와 암호화된 데이터 분리
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw EncryptionException.decryptionFailed(e);
        }
    }

    public static String generateRoomKey(String roomId, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = roomId + ":" + password;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw EncryptionException.keyGenerationFailed("room key", e);
        }
    }

    /**
     * DM 대화용 키 파생 (캐싱 적용)
     */
    public static String deriveConversationKey(String masterKey, String conversationId) {
        String cacheKey = "dm:" + conversationId;
        return derivedKeyCache.computeIfAbsent(cacheKey, k -> {
            try {
                // 캐시 크기 제한
                if (derivedKeyCache.size() >= MAX_CACHE_SIZE) {
                    derivedKeyCache.clear();
                    logger.debug("Derived key cache cleared (max size reached)");
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String input = masterKey + ":dm:" + conversationId;
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw EncryptionException.keyDerivationFailed("conversation key", e);
            }
        });
    }

    /**
     * 그룹 메시지용 키 파생 (캐싱 적용)
     */
    public static String deriveGroupRoomKey(String masterKey, Long roomId) {
        String cacheKey = "group:" + roomId;
        return derivedKeyCache.computeIfAbsent(cacheKey, k -> {
            try {
                // 캐시 크기 제한
                if (derivedKeyCache.size() >= MAX_CACHE_SIZE) {
                    derivedKeyCache.clear();
                    logger.debug("Derived key cache cleared (max size reached)");
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String input = masterKey + ":group:" + roomId;
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw EncryptionException.keyDerivationFailed("group room key", e);
            }
        });
    }

    /**
     * 안전한 복호화 (보안 로깅 포함)
     *
     * @param encryptedText 암호화된 텍스트
     * @param keyString 복호화 키
     * @return 복호화된 텍스트 또는 실패 시 Optional.empty()
     */
    public static Optional<String> decryptSafeOptional(String encryptedText, String keyString) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return Optional.ofNullable(encryptedText);
        }
        try {
            return Optional.of(decrypt(encryptedText, keyString));
        } catch (Exception e) {
            // 보안 이벤트 로깅 (암호화된 데이터 복호화 실패는 잠재적 보안 문제)
            logger.warn("Decryption failed - possible tampering or key mismatch: {}",
                e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * 안전한 복호화 (하위 호환성 유지)
     * - 복호화 실패 시 원본 반환 (레거시 메시지 지원)
     * - 보안 로깅 추가
     *
     * @param encryptedText 암호화된 텍스트
     * @param keyString 복호화 키
     * @param isEncrypted 메시지가 암호화되었는지 여부 (DB 플래그)
     * @return 복호화된 텍스트 또는 원본
     */
    public static String decryptSafe(String encryptedText, String keyString, boolean isEncrypted) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        if (!isEncrypted) {
            // 암호화되지 않은 메시지는 원본 반환
            return encryptedText;
        }

        try {
            return decrypt(encryptedText, keyString);
        } catch (Exception e) {
            // 암호화된 메시지인데 복호화 실패 -> 보안 경고
            logger.warn("SECURITY: Failed to decrypt message marked as encrypted - possible data corruption or key mismatch");
            return "[복호화 실패 - 메시지를 표시할 수 없습니다]";
        }
    }

    /**
     * 안전한 복호화 (레거시 지원 - 암호화 여부 모름)
     */
    public static String decryptSafe(String encryptedText, String keyString) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            return decrypt(encryptedText, keyString);
        } catch (Exception e) {
            // 복호화 실패 시 레거시 메시지로 간주하고 원본 반환
            logger.debug("Decryption failed, returning original (likely unencrypted legacy message)");
            return encryptedText;
        }
    }

    /**
     * 파생 키 캐시 초기화
     */
    public static void clearKeyCache() {
        derivedKeyCache.clear();
        logger.info("Derived key cache cleared manually");
    }
}
