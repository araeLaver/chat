package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, Long> {

    Optional<RoomMemberEntity> findByRoomIdAndUserIdAndIsActiveTrue(Long roomId, Long userId);

    List<RoomMemberEntity> findByRoomIdAndIsActiveTrue(Long roomId);

    List<RoomMemberEntity> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT COUNT(rm) FROM RoomMemberEntity rm WHERE " +
           "rm.roomId = :roomId AND rm.isActive = true")
    Integer countActiveMembers(@Param("roomId") Long roomId);

    @Query("SELECT rm FROM RoomMemberEntity rm WHERE " +
           "rm.roomId = :roomId AND rm.role = :role AND rm.isActive = true")
    List<RoomMemberEntity> findByRoomIdAndRole(@Param("roomId") Long roomId,
                                                 @Param("role") RoomMemberEntity.MemberRole role);

    boolean existsByRoomIdAndUserIdAndIsActiveTrue(Long roomId, Long userId);

    @Query("SELECT SUM(rm.unreadCount) FROM RoomMemberEntity rm WHERE " +
           "rm.userId = :userId AND rm.isActive = true")
    Integer getTotalUnreadCount(@Param("userId") Long userId);

    /**
     * 방의 모든 활성 멤버를 한 번의 쿼리로 비활성화 (N+1 방지)
     */
    @Modifying
    @Query("UPDATE RoomMemberEntity rm SET rm.isActive = false, rm.leftAt = :leftAt " +
           "WHERE rm.roomId = :roomId AND rm.isActive = true")
    int deactivateAllMembersByRoomId(@Param("roomId") Long roomId, @Param("leftAt") LocalDateTime leftAt);

    /**
     * 방의 활성 멤버 userId 목록 조회
     */
    @Query("SELECT rm.userId FROM RoomMemberEntity rm WHERE rm.roomId = :roomId AND rm.isActive = true")
    List<Long> findActiveUserIdsByRoomId(@Param("roomId") Long roomId);
}