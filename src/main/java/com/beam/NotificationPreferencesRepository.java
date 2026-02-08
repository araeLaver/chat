package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferencesEntity, Long> {

    Optional<NotificationPreferencesEntity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
