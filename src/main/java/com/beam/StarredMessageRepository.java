package com.beam;

import com.beam.StarredMessageEntity.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StarredMessageRepository extends JpaRepository<StarredMessageEntity, Long> {

    List<StarredMessageEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<StarredMessageEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<StarredMessageEntity> findByUserIdAndMessageIdAndMessageType(
        Long userId, Long messageId, MessageType messageType);

    boolean existsByUserIdAndMessageIdAndMessageType(
        Long userId, Long messageId, MessageType messageType);

    void deleteByUserIdAndMessageIdAndMessageType(
        Long userId, Long messageId, MessageType messageType);

    List<StarredMessageEntity> findByUserIdAndRoomIdOrderByCreatedAtDesc(Long userId, String roomId);

    List<StarredMessageEntity> findByUserIdAndConversationIdOrderByCreatedAtDesc(Long userId, String conversationId);

    @Query("SELECT COUNT(s) FROM StarredMessageEntity s WHERE s.userId = :userId")
    Integer countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) FROM StarredMessageEntity s WHERE s.messageId = :messageId AND s.messageType = :messageType")
    Integer countStars(@Param("messageId") Long messageId, @Param("messageType") MessageType messageType);
}
