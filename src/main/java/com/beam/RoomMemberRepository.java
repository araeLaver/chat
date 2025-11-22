package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}