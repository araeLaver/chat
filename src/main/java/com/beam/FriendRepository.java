package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<FriendEntity, Long> {

    @Query("SELECT f FROM FriendEntity f WHERE " +
           "(f.userId = :userId AND f.friendId = :friendId) OR " +
           "(f.userId = :friendId AND f.friendId = :userId)")
    Optional<FriendEntity> findFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Query("SELECT f FROM FriendEntity f WHERE " +
           "f.userId = :userId AND f.status = :status")
    List<FriendEntity> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendEntity.FriendStatus status);

    @Query("SELECT f FROM FriendEntity f WHERE " +
           "f.friendId = :userId AND f.status = 'PENDING'")
    List<FriendEntity> findPendingRequestsReceived(@Param("userId") Long userId);

    @Query("SELECT f FROM FriendEntity f WHERE " +
           "((f.userId = :userId OR f.friendId = :userId) AND f.status = 'ACCEPTED')")
    List<FriendEntity> findAcceptedFriends(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FriendEntity f WHERE " +
           "f.friendId = :userId AND f.status = 'PENDING'")
    Integer countPendingRequests(@Param("userId") Long userId);

    // 전체 대기 중인 친구 요청 수 (모니터링용, N+1 쿼리 방지)
    @Query("SELECT COUNT(f) FROM FriendEntity f WHERE f.status = 'PENDING'")
    long countAllPendingRequests();

    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
}
