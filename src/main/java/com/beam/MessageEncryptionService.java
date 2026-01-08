package com.beam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageEncryptionService {

    private final String masterKey;
    private final boolean encryptionEnabled;

    public MessageEncryptionService(
            @Value("${encryption.master-key:}") String masterKey,
            @Value("${encryption.enabled:false}") boolean encryptionEnabled) {
        this.masterKey = masterKey;
        this.encryptionEnabled = encryptionEnabled && masterKey != null && !masterKey.isEmpty();
    }

    /**
     * Room 메시지 암호화
     */
    public String encryptRoomMessage(String content, String roomId) {
        if (!encryptionEnabled || content == null || content.isEmpty()) {
            return content;
        }
        String key = EncryptionUtil.generateRoomKey(roomId, masterKey);
        return EncryptionUtil.encrypt(content, key);
    }

    /**
     * Room 메시지 복호화
     */
    public String decryptRoomMessage(String content, String roomId, boolean isEncrypted) {
        if (!isEncrypted || content == null || content.isEmpty()) {
            return content;
        }
        String key = EncryptionUtil.generateRoomKey(roomId, masterKey);
        return EncryptionUtil.decryptSafe(content, key);
    }

    /**
     * DM 메시지 암호화
     */
    public String encryptDirectMessage(String content, String conversationId) {
        if (!encryptionEnabled || content == null || content.isEmpty()) {
            return content;
        }
        String key = EncryptionUtil.deriveConversationKey(masterKey, conversationId);
        return EncryptionUtil.encrypt(content, key);
    }

    /**
     * DM 메시지 복호화
     */
    public String decryptDirectMessage(String content, String conversationId, boolean isEncrypted) {
        if (!isEncrypted || content == null || content.isEmpty()) {
            return content;
        }
        String key = EncryptionUtil.deriveConversationKey(masterKey, conversationId);
        return EncryptionUtil.decryptSafe(content, key);
    }

    /**
     * 그룹 메시지 암호화
     */
    public String encryptGroupMessage(String content, Long roomId) {
        if (!encryptionEnabled || content == null || content.isEmpty()) {
            return content;
        }
        String key = EncryptionUtil.deriveGroupRoomKey(masterKey, roomId);
        return EncryptionUtil.encrypt(content, key);
    }

    /**
     * 그룹 메시지 복호화
     */
    public String decryptGroupMessage(String content, Long roomId, boolean isEncrypted) {
        if (!isEncrypted || content == null || content.isEmpty()) {
            return content;
        }
        String key = EncryptionUtil.deriveGroupRoomKey(masterKey, roomId);
        return EncryptionUtil.decryptSafe(content, key);
    }

    /**
     * 암호화 활성화 여부
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
}
