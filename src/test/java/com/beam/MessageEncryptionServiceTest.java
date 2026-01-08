package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageEncryptionService Tests")
class MessageEncryptionServiceTest {

    private static final String TEST_MASTER_KEY = EncryptionUtil.generateSecretKey();
    private MessageEncryptionService enabledService;
    private MessageEncryptionService disabledService;

    @BeforeEach
    void setUp() {
        enabledService = new MessageEncryptionService(TEST_MASTER_KEY, true);
        disabledService = new MessageEncryptionService("", false);
    }

    @Nested
    @DisplayName("Room Message Encryption Tests")
    class RoomMessageEncryptionTests {

        @Test
        @DisplayName("Should encrypt and decrypt room message")
        void shouldEncryptAndDecryptRoomMessage() {
            String content = "Hello, Room!";
            String roomId = "room123";

            String encrypted = enabledService.encryptRoomMessage(content, roomId);
            String decrypted = enabledService.decryptRoomMessage(encrypted, roomId, true);

            assertEquals(content, decrypted);
            assertNotEquals(content, encrypted);
        }

        @Test
        @DisplayName("Should return original content when encryption disabled")
        void shouldReturnOriginalWhenDisabled() {
            String content = "Plain text";
            String roomId = "room1";

            String result = disabledService.encryptRoomMessage(content, roomId);

            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should return original when isEncrypted is false")
        void shouldReturnOriginalWhenNotEncrypted() {
            String content = "Unencrypted message";
            String roomId = "room1";

            String result = enabledService.decryptRoomMessage(content, roomId, false);

            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should handle null content")
        void shouldHandleNullContent() {
            String roomId = "room1";

            assertNull(enabledService.encryptRoomMessage(null, roomId));
            assertNull(enabledService.decryptRoomMessage(null, roomId, true));
        }

        @Test
        @DisplayName("Should handle empty content")
        void shouldHandleEmptyContent() {
            String roomId = "room1";

            String encrypted = enabledService.encryptRoomMessage("", roomId);
            assertEquals("", encrypted);
        }
    }

    @Nested
    @DisplayName("Direct Message Encryption Tests")
    class DirectMessageEncryptionTests {

        @Test
        @DisplayName("Should encrypt and decrypt direct message")
        void shouldEncryptAndDecryptDirectMessage() {
            String content = "Secret DM";
            String conversationId = "dm_1_2";

            String encrypted = enabledService.encryptDirectMessage(content, conversationId);
            String decrypted = enabledService.decryptDirectMessage(encrypted, conversationId, true);

            assertEquals(content, decrypted);
            assertNotEquals(content, encrypted);
        }

        @Test
        @DisplayName("Should handle Korean text in DM")
        void shouldHandleKoreanTextInDm() {
            String content = "안녕하세요, 비밀 메시지입니다.";
            String conversationId = "dm_1_2";

            String encrypted = enabledService.encryptDirectMessage(content, conversationId);
            String decrypted = enabledService.decryptDirectMessage(encrypted, conversationId, true);

            assertEquals(content, decrypted);
        }

        @Test
        @DisplayName("Different conversations should have different encrypted content")
        void differentConversationsShouldHaveDifferentEncryptedContent() {
            String content = "Same message";
            String conv1 = "dm_1_2";
            String conv2 = "dm_1_3";

            String encrypted1 = enabledService.encryptDirectMessage(content, conv1);
            String encrypted2 = enabledService.encryptDirectMessage(content, conv2);

            // 같은 메시지도 다른 대화에서는 다르게 암호화됨
            // (키가 다르고 IV도 랜덤이므로)
            assertNotEquals(encrypted1, encrypted2);
        }
    }

    @Nested
    @DisplayName("Group Message Encryption Tests")
    class GroupMessageEncryptionTests {

        @Test
        @DisplayName("Should encrypt and decrypt group message")
        void shouldEncryptAndDecryptGroupMessage() {
            String content = "Group announcement";
            Long roomId = 123L;

            String encrypted = enabledService.encryptGroupMessage(content, roomId);
            String decrypted = enabledService.decryptGroupMessage(encrypted, roomId, true);

            assertEquals(content, decrypted);
            assertNotEquals(content, encrypted);
        }

        @Test
        @DisplayName("Should return original when encryption disabled")
        void shouldReturnOriginalWhenDisabled() {
            String content = "Plain text";
            Long roomId = 1L;

            String result = disabledService.encryptGroupMessage(content, roomId);

            assertEquals(content, result);
        }

        @Test
        @DisplayName("Different rooms should have different encrypted content")
        void differentRoomsShouldHaveDifferentEncryptedContent() {
            String content = "Same message";
            Long room1 = 1L;
            Long room2 = 2L;

            String encrypted1 = enabledService.encryptGroupMessage(content, room1);
            String encrypted2 = enabledService.encryptGroupMessage(content, room2);

            assertNotEquals(encrypted1, encrypted2);
        }
    }

    @Nested
    @DisplayName("Encryption Enabled Status Tests")
    class EncryptionEnabledStatusTests {

        @Test
        @DisplayName("Should be enabled when masterKey is set and enabled is true")
        void shouldBeEnabledWhenMasterKeyIsSetAndEnabledIsTrue() {
            assertTrue(enabledService.isEncryptionEnabled());
        }

        @Test
        @DisplayName("Should be disabled when enabled is false")
        void shouldBeDisabledWhenEnabledIsFalse() {
            assertFalse(disabledService.isEncryptionEnabled());
        }

        @Test
        @DisplayName("Should be disabled when masterKey is empty")
        void shouldBeDisabledWhenMasterKeyIsEmpty() {
            MessageEncryptionService service = new MessageEncryptionService("", true);
            assertFalse(service.isEncryptionEnabled());
        }

        @Test
        @DisplayName("Should be disabled when masterKey is null")
        void shouldBeDisabledWhenMasterKeyIsNull() {
            MessageEncryptionService service = new MessageEncryptionService(null, true);
            assertFalse(service.isEncryptionEnabled());
        }
    }

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should return original for unencrypted legacy message")
        void shouldReturnOriginalForUnencryptedLegacyMessage() {
            String legacyContent = "This is a legacy unencrypted message";
            String roomId = "room1";

            // isEncrypted가 false인 레거시 메시지는 복호화 시도 없이 원본 반환
            String result = enabledService.decryptRoomMessage(legacyContent, roomId, false);

            assertEquals(legacyContent, result);
        }

        @Test
        @DisplayName("Should handle corrupted encrypted data gracefully")
        void shouldHandleCorruptedEncryptedDataGracefully() {
            String corruptedData = "not-valid-encrypted-data";
            String roomId = "room1";

            // decryptSafe를 사용하므로 예외 대신 원본 반환
            String result = enabledService.decryptRoomMessage(corruptedData, roomId, true);

            assertEquals(corruptedData, result);
        }
    }
}
