package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessageEntity, Long> {

    List<GroupMessageEntity> findByRoomIdAndIsDeletedFalseOrderByTimestampAsc(Long roomId);

    List<GroupMessageEntity> findTop100ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(Long roomId);

    @Query("SELECT gm FROM GroupMessageEntity gm WHERE " +
           "gm.roomId = :roomId AND " +
           "gm.timestamp > :since AND " +
           "gm.isDeleted = false " +
           "ORDER BY gm.timestamp ASC")
    List<GroupMessageEntity> findMessagesSince(@Param("roomId") Long roomId,
                                                @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(gm) FROM GroupMessageEntity gm WHERE " +
           "gm.roomId = :roomId AND " +
           "gm.timestamp > :since AND " +
           "gm.isDeleted = false")
    Integer countUnreadMessages(@Param("roomId") Long roomId,
                                  @Param("since") LocalDateTime since);
}