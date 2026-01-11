package com.beam;

import com.beam.NotificationEntity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId AND n.isRead = false")
    Integer countUnreadNotifications(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.id = :notificationId AND n.userId = :userId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<NotificationEntity> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);

    @Query("SELECT n FROM NotificationEntity n WHERE n.userId = :userId " +
           "AND n.type IN :types ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByUserIdAndTypesOrderByCreatedAtDesc(
        @Param("userId") Long userId, @Param("types") List<NotificationType> types);

    void deleteByUserIdAndCreatedAtBefore(Long userId, LocalDateTime before);
}
