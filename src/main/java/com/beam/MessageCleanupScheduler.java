package com.beam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 메시지 자동 삭제 스케줄러
 * TTL(Time-To-Live)이 설정된 메시지를 주기적으로 정리
 */
@Component
public class MessageCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MessageCleanupScheduler.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    /**
     * 만료된 메시지 정리 (1분마다 실행)
     * - messages 테이블
     * - direct_messages 테이블
     * - group_messages 테이블
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void cleanupExpiredMessages() {
        LocalDateTime now = LocalDateTime.now();

        try {
            // 일반 메시지 정리
            int messagesDeleted = messageRepository.deleteByExpiresAtBefore(now);
            if (messagesDeleted > 0) {
                logger.info("Deleted {} expired messages", messagesDeleted);
            }

            // DM 메시지 정리
            int dmDeleted = directMessageRepository.deleteByExpiresAtBefore(now);
            if (dmDeleted > 0) {
                logger.info("Deleted {} expired direct messages", dmDeleted);
            }

            // 그룹 메시지 정리
            int groupDeleted = groupMessageRepository.deleteByExpiresAtBefore(now);
            if (groupDeleted > 0) {
                logger.info("Deleted {} expired group messages", groupDeleted);
            }

            int totalDeleted = messagesDeleted + dmDeleted + groupDeleted;
            if (totalDeleted > 0) {
                logger.info("Total expired messages cleaned up: {}", totalDeleted);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired messages: {}", e.getMessage(), e);
        }
    }
}
