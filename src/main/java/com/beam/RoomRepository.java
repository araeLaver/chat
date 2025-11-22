package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    Optional<RoomEntity> findByIdAndIsActiveTrue(Long id);

    List<RoomEntity> findByCreatedByAndIsActiveTrue(Long createdBy);

    List<RoomEntity> findByRoomTypeAndIsActiveTrue(RoomEntity.RoomType roomType);

    Optional<RoomEntity> findByRoomNameAndRoomType(String roomName, RoomEntity.RoomType roomType);

    @Query("SELECT r FROM RoomEntity r WHERE " +
           "r.isActive = true AND " +
           "(r.roomName LIKE %:keyword% OR r.description LIKE %:keyword%)")
    List<RoomEntity> searchRooms(@Param("keyword") String keyword);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.createdBy = :userId AND r.isActive = true")
    Integer countRoomsByUser(@Param("userId") Long userId);
}