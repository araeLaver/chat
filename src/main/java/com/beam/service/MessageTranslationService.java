package com.beam.service;

import com.beam.ChatMessage;
import com.beam.TranslationService;
import com.beam.UserEntity;
import com.beam.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 메시지 번역 서비스
 * 수신자의 언어 설정에 따라 메시지를 자동 번역
 */
@Slf4j
@Service
public class MessageTranslationService {

    private final TranslationService translationService;
    private final UserRepository userRepository;

    @Autowired
    public MessageTranslationService(TranslationService translationService, UserRepository userRepository) {
        this.translationService = translationService;
        this.userRepository = userRepository;
    }

    /**
     * 수신자의 언어 설정에 따라 메시지 번역
     * @param message 원본 메시지
     * @param recipientId 수신자 ID
     * @return 번역된 메시지 (또는 원본)
     */
    public ChatMessage translateForRecipient(ChatMessage message, Long recipientId) {
        if (!translationService.isEnabled()) {
            return message;
        }

        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            return message;
        }

        Optional<UserEntity> recipientOpt = userRepository.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            return message;
        }

        UserEntity recipient = recipientOpt.get();

        // 자동 번역이 비활성화된 경우 원본 반환
        if (!Boolean.TRUE.equals(recipient.getAutoTranslate())) {
            return message;
        }

        String targetLanguage = recipient.getPreferredLanguage();
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = "ko"; // 기본 언어
        }

        // 원본 메시지 언어 감지
        String sourceLanguage = message.getSourceLanguage();
        if (sourceLanguage == null || sourceLanguage.isEmpty()) {
            sourceLanguage = translationService.detectLanguage(message.getContent());
        }

        // 같은 언어면 번역 불필요
        if (sourceLanguage.equals(targetLanguage)) {
            return message;
        }

        // 번역 수행
        try {
            String translatedContent = translationService.translate(
                message.getContent(),
                sourceLanguage,
                targetLanguage
            );

            // 번역된 메시지 생성 (원본 보존)
            ChatMessage translatedMessage = cloneMessage(message);
            translatedMessage.setOriginalContent(message.getContent());
            translatedMessage.setContent(translatedContent);
            translatedMessage.setSourceLanguage(sourceLanguage);
            translatedMessage.setIsTranslated(true);

            log.debug("Translated message for user {}: {} -> {}",
                recipientId, sourceLanguage, targetLanguage);

            return translatedMessage;
        } catch (Exception e) {
            log.error("Failed to translate message for recipient {}: {}", recipientId, e.getMessage());
            return message;
        }
    }

    /**
     * 발신자의 언어를 감지하고 메시지에 설정
     */
    public String detectAndSetLanguage(ChatMessage message) {
        if (!translationService.isEnabled() || message == null ||
            message.getContent() == null || message.getContent().trim().isEmpty()) {
            return "unknown";
        }

        String detectedLanguage = translationService.detectLanguage(message.getContent());
        message.setSourceLanguage(detectedLanguage);
        return detectedLanguage;
    }

    /**
     * 특정 언어로 메시지 번역
     */
    public ChatMessage translateTo(ChatMessage message, String targetLanguage) {
        if (!translationService.isEnabled() || message == null ||
            message.getContent() == null || message.getContent().trim().isEmpty()) {
            return message;
        }

        String sourceLanguage = message.getSourceLanguage();
        if (sourceLanguage == null) {
            sourceLanguage = translationService.detectLanguage(message.getContent());
        }

        if (sourceLanguage.equals(targetLanguage)) {
            return message;
        }

        String translatedContent = translationService.translate(
            message.getContent(),
            sourceLanguage,
            targetLanguage
        );

        ChatMessage translatedMessage = cloneMessage(message);
        translatedMessage.setOriginalContent(message.getContent());
        translatedMessage.setContent(translatedContent);
        translatedMessage.setSourceLanguage(sourceLanguage);
        translatedMessage.setIsTranslated(true);

        return translatedMessage;
    }

    /**
     * 번역 서비스 활성화 여부 확인
     */
    public boolean isTranslationEnabled() {
        return translationService.isEnabled();
    }

    /**
     * ChatMessage 복제
     */
    private ChatMessage cloneMessage(ChatMessage original) {
        ChatMessage clone = new ChatMessage();
        clone.setSender(original.getSender());
        clone.setContent(original.getContent());
        clone.setTimestamp(original.getTimestamp());
        clone.setType(original.getType());
        clone.setRoomId(original.getRoomId());
        clone.setSecurityType(original.getSecurityType());
        clone.setUserId(original.getUserId());
        clone.setFriendId(original.getFriendId());
        clone.setFriendName(original.getFriendName());
        clone.setRoomName(original.getRoomName());
        clone.setRoomType(original.getRoomType());
        clone.setCreator(original.getCreator());
        clone.setDescription(original.getDescription());
        clone.setTtlSeconds(original.getTtlSeconds());
        clone.setOriginalContent(original.getOriginalContent());
        clone.setSourceLanguage(original.getSourceLanguage());
        clone.setIsTranslated(original.getIsTranslated());
        clone.setReplyToId(original.getReplyToId());
        clone.setReplyToSender(original.getReplyToSender());
        clone.setReplyToContent(original.getReplyToContent());
        return clone;
    }
}
