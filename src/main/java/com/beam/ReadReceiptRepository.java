package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceiptEntity, Long> {

    List<ReadReceiptEntity> findByMessageId(Long messageId);

    List<ReadReceiptEntity> findByGroupMessageId(Long groupMessageId);

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    boolean existsByGroupMessageIdAndUserId(Long groupMessageId, Long userId);

    @Query("SELECT COUNT(r) FROM ReadReceiptEntity r WHERE r.groupMessageId = :messageId")
    Integer countReadReceiptsByGroupMessage(@Param("messageId") Long messageId);

    @Query("SELECT r FROM ReadReceiptEntity r WHERE " +
           "r.groupMessageId IN :messageIds")
    List<ReadReceiptEntity> findByGroupMessageIds(@Param("messageIds") List<Long> messageIds);
}