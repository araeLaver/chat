package com.beam;

import com.beam.MentionEntity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MentionRepository extends JpaRepository<MentionEntity, Long> {

    List<MentionEntity> findByMentionedUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    List<MentionEntity> findByMentionedUserIdOrderByCreatedAtDesc(Long userId);

    List<MentionEntity> findByMessageIdAndMessageType(Long messageId, MessageType messageType);

    @Query("SELECT COUNT(m) FROM MentionEntity m WHERE m.mentionedUserId = :userId AND m.isRead = false")
    Integer countUnreadMentions(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE MentionEntity m SET m.isRead = true, m.readAt = :now " +
           "WHERE m.mentionedUserId = :userId AND m.isRead = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE MentionEntity m SET m.isRead = true, m.readAt = :now " +
           "WHERE m.id = :mentionId AND m.mentionedUserId = :userId")
    void markAsRead(@Param("mentionId") Long mentionId, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT m FROM MentionEntity m WHERE m.mentionedUserId = :userId " +
           "AND m.roomId = :roomId AND m.isRead = false")
    List<MentionEntity> findUnreadMentionsInRoom(@Param("userId") Long userId, @Param("roomId") String roomId);

    boolean existsByMessageIdAndMessageTypeAndMentionedUserId(Long messageId, MessageType messageType, Long mentionedUserId);
}
