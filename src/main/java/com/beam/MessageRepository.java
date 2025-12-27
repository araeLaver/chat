package com.beam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @Query("SELECT m FROM MessageEntity m WHERE m.roomId = :roomId ORDER BY m.timestamp DESC LIMIT 50")
    List<MessageEntity> findTop50ByRoomIdOrderByTimestampDesc(@Param("roomId") String roomId);

    List<MessageEntity> findByRoomIdOrderByTimestampAsc(String roomId);

    // 페이징 지원 버전 (대용량 채팅방 처리용)
    Page<MessageEntity> findByRoomIdOrderByTimestampAsc(String roomId, Pageable pageable);

    // 읽지 않은 메시지 ID 목록 조회 (배치 읽음 처리용)
    @Query("SELECT m.id FROM MessageEntity m WHERE m.roomId = :roomId " +
           "AND m.sender != :username " +
           "AND NOT EXISTS (SELECT r FROM MessageReadReceipt r WHERE r.messageId = m.id AND r.userId = :userId)")
    List<Long> findUnreadMessageIds(@Param("roomId") String roomId,
                                     @Param("userId") Long userId,
                                     @Param("username") String username);
}