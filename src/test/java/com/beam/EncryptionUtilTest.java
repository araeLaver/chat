package com.beam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EncryptionUtil Tests")
class EncryptionUtilTest {

    @Nested
    @DisplayName("generateSecretKey Tests")
    class GenerateSecretKeyTests {

        @Test
        @DisplayName("Should generate non-null secret key")
        void shouldGenerateNonNullSecretKey() {
            String key = EncryptionUtil.generateSecretKey();
            assertNotNull(key);
            assertFalse(key.isEmpty());
        }

        @Test
        @DisplayName("Should generate unique keys each time")
        void shouldGenerateUniqueKeysEachTime() {
            String key1 = EncryptionUtil.generateSecretKey();
            String key2 = EncryptionUtil.generateSecretKey();
            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Should generate Base64 encoded key")
        void shouldGenerateBase64EncodedKey() {
            String key = EncryptionUtil.generateSecretKey();
            assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(key));
        }
    }

    @Nested
    @DisplayName("encrypt and decrypt Tests")
    class EncryptDecryptTests {

        @Test
        @DisplayName("Should encrypt and decrypt text successfully")
        void shouldEncryptAndDecryptTextSuccessfully() {
            String key = EncryptionUtil.generateSecretKey();
            String plainText = "Hello, World!";

            String encrypted = EncryptionUtil.encrypt(plainText, key);
            String decrypted = EncryptionUtil.decrypt(encrypted, key);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("Should encrypt to different output each time")
        void shouldEncryptToDifferentOutputEachTime() {
            String key = EncryptionUtil.generateSecretKey();
            String plainText = "Hello, World!";

            String encrypted1 = EncryptionUtil.encrypt(plainText, key);
            String encrypted2 = EncryptionUtil.encrypt(plainText, key);

            assertNotEquals(encrypted1, encrypted2);
        }

        @Test
        @DisplayName("Should handle Korean text")
        void shouldHandleKoreanText() {
            String key = EncryptionUtil.generateSecretKey();
            String plainText = "안녕하세요, 대한민국!";

            String encrypted = EncryptionUtil.encrypt(plainText, key);
            String decrypted = EncryptionUtil.decrypt(encrypted, key);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            String key = EncryptionUtil.generateSecretKey();
            String plainText = "";

            String encrypted = EncryptionUtil.encrypt(plainText, key);
            String decrypted = EncryptionUtil.decrypt(encrypted, key);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("Should handle long text")
        void shouldHandleLongText() {
            String key = EncryptionUtil.generateSecretKey();
            String plainText = "A".repeat(10000);

            String encrypted = EncryptionUtil.encrypt(plainText, key);
            String decrypted = EncryptionUtil.decrypt(encrypted, key);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("Should handle special characters")
        void shouldHandleSpecialCharacters() {
            String key = EncryptionUtil.generateSecretKey();
            String plainText = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

            String encrypted = EncryptionUtil.encrypt(plainText, key);
            String decrypted = EncryptionUtil.decrypt(encrypted, key);

            assertEquals(plainText, decrypted);
        }

        @Test
        @DisplayName("Should fail with wrong key")
        void shouldFailWithWrongKey() {
            String key1 = EncryptionUtil.generateSecretKey();
            String key2 = EncryptionUtil.generateSecretKey();
            String plainText = "Secret message";

            String encrypted = EncryptionUtil.encrypt(plainText, key1);

            assertThrows(RuntimeException.class, () ->
                EncryptionUtil.decrypt(encrypted, key2));
        }

        @Test
        @DisplayName("Should fail with invalid encrypted data")
        void shouldFailWithInvalidEncryptedData() {
            String key = EncryptionUtil.generateSecretKey();

            assertThrows(RuntimeException.class, () ->
                EncryptionUtil.decrypt("invalid-data", key));
        }
    }

    @Nested
    @DisplayName("generateRoomKey Tests")
    class GenerateRoomKeyTests {

        @Test
        @DisplayName("Should generate room key from roomId and password")
        void shouldGenerateRoomKeyFromRoomIdAndPassword() {
            String roomKey = EncryptionUtil.generateRoomKey("room123", "password");

            assertNotNull(roomKey);
            assertFalse(roomKey.isEmpty());
        }

        @Test
        @DisplayName("Should generate same key for same inputs")
        void shouldGenerateSameKeyForSameInputs() {
            String key1 = EncryptionUtil.generateRoomKey("room123", "password");
            String key2 = EncryptionUtil.generateRoomKey("room123", "password");

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("Should generate different keys for different roomIds")
        void shouldGenerateDifferentKeysForDifferentRoomIds() {
            String key1 = EncryptionUtil.generateRoomKey("room123", "password");
            String key2 = EncryptionUtil.generateRoomKey("room456", "password");

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Should generate different keys for different passwords")
        void shouldGenerateDifferentKeysForDifferentPasswords() {
            String key1 = EncryptionUtil.generateRoomKey("room123", "password1");
            String key2 = EncryptionUtil.generateRoomKey("room123", "password2");

            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Should generate Base64 encoded key")
        void shouldGenerateBase64EncodedKey() {
            String key = EncryptionUtil.generateRoomKey("room123", "password");
            assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(key));
        }
    }
}
