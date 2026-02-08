package com.beam;

import com.beam.DeviceTokenEntity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {

    List<DeviceTokenEntity> findByUserIdAndIsActiveTrue(Long userId);

    List<DeviceTokenEntity> findByUserId(Long userId);

    Optional<DeviceTokenEntity> findByDeviceToken(String deviceToken);

    Optional<DeviceTokenEntity> findByUserIdAndDeviceToken(Long userId, String deviceToken);

    List<DeviceTokenEntity> findByUserIdAndDeviceType(Long userId, DeviceType deviceType);

    @Modifying
    @Query("UPDATE DeviceTokenEntity d SET d.isActive = false WHERE d.deviceToken = :token")
    void deactivateByToken(@Param("token") String token);

    @Modifying
    @Query("UPDATE DeviceTokenEntity d SET d.isActive = false WHERE d.userId = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);

    void deleteByDeviceToken(String deviceToken);

    void deleteByUserIdAndDeviceToken(Long userId, String deviceToken);

    @Query("SELECT d FROM DeviceTokenEntity d WHERE d.userId IN :userIds AND d.isActive = true")
    List<DeviceTokenEntity> findActiveTokensByUserIds(@Param("userIds") List<Long> userIds);

    boolean existsByDeviceToken(String deviceToken);
}
