package com.beam;

import com.beam.MessageReactionEntity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReactionEntity, Long> {

    List<MessageReactionEntity> findByMessageIdAndMessageType(Long messageId, MessageType messageType);

    Optional<MessageReactionEntity> findByMessageIdAndMessageTypeAndUserIdAndEmoji(
        Long messageId, MessageType messageType, Long userId, String emoji);

    @Query("SELECT r.emoji, COUNT(r) FROM MessageReactionEntity r " +
           "WHERE r.messageId = :messageId AND r.messageType = :messageType " +
           "GROUP BY r.emoji ORDER BY COUNT(r) DESC")
    List<Object[]> countReactionsByEmoji(@Param("messageId") Long messageId,
                                          @Param("messageType") MessageType messageType);

    @Query("SELECT r.emoji, r.userId FROM MessageReactionEntity r " +
           "WHERE r.messageId = :messageId AND r.messageType = :messageType")
    List<Object[]> findReactionsWithUsers(@Param("messageId") Long messageId,
                                           @Param("messageType") MessageType messageType);

    @Modifying
    @Query("DELETE FROM MessageReactionEntity r " +
           "WHERE r.messageId = :messageId AND r.messageType = :messageType " +
           "AND r.userId = :userId AND r.emoji = :emoji")
    void deleteByMessageIdAndMessageTypeAndUserIdAndEmoji(
        @Param("messageId") Long messageId,
        @Param("messageType") MessageType messageType,
        @Param("userId") Long userId,
        @Param("emoji") String emoji);

    boolean existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
        Long messageId, MessageType messageType, Long userId, String emoji);

    @Query("SELECT COUNT(r) FROM MessageReactionEntity r " +
           "WHERE r.messageId = :messageId AND r.messageType = :messageType AND r.emoji = :emoji")
    long countByMessageIdAndMessageTypeAndEmoji(
        @Param("messageId") Long messageId,
        @Param("messageType") MessageType messageType,
        @Param("emoji") String emoji);

    List<MessageReactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
