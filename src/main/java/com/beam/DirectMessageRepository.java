package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, Long> {

    List<DirectMessageEntity> findByConversationIdOrderByTimestampAsc(String conversationId);

    List<DirectMessageEntity> findTop50ByConversationIdOrderByTimestampDesc(String conversationId);

    @Query("SELECT COUNT(d) FROM DirectMessageEntity d WHERE d.conversationId = :conversationId " +
           "AND d.receiverId = :userId AND d.isRead = false")
    Integer countUnreadMessages(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    @Query("SELECT d FROM DirectMessageEntity d WHERE d.conversationId = :conversationId " +
           "AND d.receiverId = :userId AND d.isRead = false")
    List<DirectMessageEntity> findUnreadMessages(@Param("conversationId") String conversationId,
                                                   @Param("userId") Long userId);
}