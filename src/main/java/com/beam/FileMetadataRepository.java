package com.beam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {

    Optional<FileMetadataEntity> findByIdAndIsDeletedFalse(Long id);

    List<FileMetadataEntity> findByConversationIdAndIsDeletedFalseOrderByUploadedAtDesc(String conversationId);

    List<FileMetadataEntity> findByRoomIdAndIsDeletedFalseOrderByUploadedAtDesc(Long roomId);

    List<FileMetadataEntity> findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(Long uploaderId);

    @Query("SELECT f FROM FileMetadataEntity f WHERE " +
           "f.conversationId = :conversationId AND " +
           "f.category = :category AND " +
           "f.isDeleted = false " +
           "ORDER BY f.uploadedAt DESC")
    List<FileMetadataEntity> findByConversationAndCategory(@Param("conversationId") String conversationId,
                                                             @Param("category") FileMetadataEntity.FileCategory category);

    @Query("SELECT f FROM FileMetadataEntity f WHERE " +
           "f.roomId = :roomId AND " +
           "f.category = :category AND " +
           "f.isDeleted = false " +
           "ORDER BY f.uploadedAt DESC")
    List<FileMetadataEntity> findByRoomAndCategory(@Param("roomId") Long roomId,
                                                     @Param("category") FileMetadataEntity.FileCategory category);

    @Query("SELECT SUM(f.fileSize) FROM FileMetadataEntity f WHERE " +
           "f.uploaderId = :userId AND f.isDeleted = false")
    Long getTotalFileSizeByUser(@Param("userId") Long userId);
}