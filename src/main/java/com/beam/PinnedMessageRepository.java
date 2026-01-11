package com.beam;

import com.beam.PinnedMessageEntity.ContextType;
import com.beam.PinnedMessageEntity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinnedMessageRepository extends JpaRepository<PinnedMessageEntity, Long> {

    List<PinnedMessageEntity> findByContextTypeAndContextIdOrderByPinOrderAscCreatedAtDesc(
        ContextType contextType, String contextId);

    Optional<PinnedMessageEntity> findByMessageIdAndMessageTypeAndContextTypeAndContextId(
        Long messageId, MessageType messageType, ContextType contextType, String contextId);

    boolean existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
        Long messageId, MessageType messageType, ContextType contextType, String contextId);

    void deleteByMessageIdAndMessageTypeAndContextTypeAndContextId(
        Long messageId, MessageType messageType, ContextType contextType, String contextId);

    @Query("SELECT COUNT(p) FROM PinnedMessageEntity p WHERE p.contextType = :contextType AND p.contextId = :contextId")
    Integer countByContext(@Param("contextType") ContextType contextType, @Param("contextId") String contextId);

    @Query("SELECT COALESCE(MAX(p.pinOrder), 0) FROM PinnedMessageEntity p " +
           "WHERE p.contextType = :contextType AND p.contextId = :contextId")
    Integer getMaxPinOrder(@Param("contextType") ContextType contextType, @Param("contextId") String contextId);

    List<PinnedMessageEntity> findByPinnedByUserId(Long userId);
}
