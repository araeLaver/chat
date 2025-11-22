package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    Optional<ConversationEntity> findByConversationId(String conversationId);

    @Query("SELECT c FROM ConversationEntity c WHERE " +
           "(c.user1Id = :userId OR c.user2Id = :userId) " +
           "ORDER BY c.lastMessageTime DESC")
    List<ConversationEntity> findUserConversations(@Param("userId") Long userId);

    @Query("SELECT c FROM ConversationEntity c WHERE " +
           "(c.user1Id = :user1Id AND c.user2Id = :user2Id) OR " +
           "(c.user1Id = :user2Id AND c.user2Id = :user1Id)")
    Optional<ConversationEntity> findByUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}