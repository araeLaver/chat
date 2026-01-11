package com.beam.dto;

import java.time.LocalDateTime;

/**
 * 대화 목록 조회용 Projection (N+1 문제 해결)
 */
public interface ConversationListItemProjection {
    String getConversationId();
    Long getUser1Id();
    Long getUser2Id();
    String getLastMessage();
    LocalDateTime getLastMessageTime();
    Integer getUnreadCountUser1();
    Integer getUnreadCountUser2();

    // User1 정보 (JOIN)
    String getUser1DisplayName();
    Boolean getUser1IsOnline();

    // User2 정보 (JOIN)
    String getUser2DisplayName();
    Boolean getUser2IsOnline();
}
