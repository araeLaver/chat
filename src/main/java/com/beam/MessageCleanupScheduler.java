package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// 비활성화: 보안 기능(VOLATILE) 제거로 인해 더 이상 필요하지 않음
// @Component
public class MessageCleanupScheduler {

    // @Autowired
    // private MessageRepository messageRepository;

    // 기존 메시지 정리 스케줄러는 일반 채팅에서는 불필요하므로 비활성화됨
}