package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // 사용자 이름으로 검색 (친구 추가용)
    java.util.List<UserEntity> findByUsernameContaining(String keyword);

    // 온라인 사용자 수 조회 (N+1 쿼리 방지)
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.isOnline = true")
    long countOnlineUsers();
}