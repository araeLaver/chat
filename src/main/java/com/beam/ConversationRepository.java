package com.beam;

import com.beam.dto.ConversationListItemProjection;
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

    /**
     * 사용자의 대화 목록을 사용자 정보와 함께 조회 (N+1 문제 해결)
     */
    @Query("SELECT c.conversationId AS conversationId, " +
           "c.user1Id AS user1Id, c.user2Id AS user2Id, " +
           "c.lastMessage AS lastMessage, c.lastMessageTime AS lastMessageTime, " +
           "c.unreadCountUser1 AS unreadCountUser1, c.unreadCountUser2 AS unreadCountUser2, " +
           "u1.displayName AS user1DisplayName, u1.isOnline AS user1IsOnline, " +
           "u2.displayName AS user2DisplayName, u2.isOnline AS user2IsOnline " +
           "FROM ConversationEntity c " +
           "LEFT JOIN UserEntity u1 ON c.user1Id = u1.id " +
           "LEFT JOIN UserEntity u2 ON c.user2Id = u2.id " +
           "WHERE c.user1Id = :userId OR c.user2Id = :userId " +
           "ORDER BY c.lastMessageTime DESC")
    List<ConversationListItemProjection> findUserConversationsWithUsers(@Param("userId") Long userId);
}