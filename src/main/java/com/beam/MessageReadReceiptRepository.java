package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {

    // 특정 메시지를 특정 사용자가 읽었는지 확인
    Optional<MessageReadReceipt> findByMessageIdAndUserId(Long messageId, Long userId);

    // 특정 메시지의 모든 읽음 표시
    List<MessageReadReceipt> findByMessageId(Long messageId);

    // 특정 사용자가 읽은 메시지들
    List<MessageReadReceipt> findByUserId(Long userId);

    // 메시지를 읽은 사용자 수
    @Query("SELECT COUNT(r) FROM MessageReadReceipt r WHERE r.messageId = :messageId")
    long countByMessageId(@Param("messageId") Long messageId);

    // 채팅방의 안읽은 메시지 수 조회
    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.roomId = :roomId " +
           "AND m.sender != :username " +
           "AND NOT EXISTS (SELECT r FROM MessageReadReceipt r WHERE r.messageId = m.id AND r.userId = :userId)")
    long countUnreadMessagesInRoom(@Param("roomId") String roomId, @Param("userId") Long userId, @Param("username") String username);

    // 특정 메시지를 읽었는지 확인
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM MessageReadReceipt r " +
           "WHERE r.messageId = :messageId AND r.userId = :userId")
    boolean existsByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);
}
