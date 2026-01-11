package com.beam;

import com.beam.dto.FileListItemProjection;
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

    /**
     * 전체 파일 저장소 크기 조회
     */
    @Query("SELECT SUM(f.fileSize) FROM FileMetadataEntity f WHERE f.isDeleted = false")
    Long getTotalFileSize();

    /**
     * 대화 파일 목록 조회 (N+1 문제 해결 - JOIN 사용)
     */
    @Query("SELECT f.id AS fileId, f.fileName AS fileName, f.fileSize AS fileSize, " +
           "f.fileType AS fileType, CAST(f.category AS string) AS category, " +
           "f.uploaderId AS uploaderId, u.displayName AS uploaderName, " +
           "f.uploadedAt AS uploadedAt, f.downloadCount AS downloadCount, " +
           "f.thumbnailPath AS thumbnailPath " +
           "FROM FileMetadataEntity f LEFT JOIN UserEntity u ON f.uploaderId = u.id " +
           "WHERE f.conversationId = :conversationId AND f.isDeleted = false " +
           "ORDER BY f.uploadedAt DESC")
    List<FileListItemProjection> findConversationFilesWithUploader(
            @Param("conversationId") String conversationId);

    /**
     * 채팅방 파일 목록 조회 (N+1 문제 해결 - JOIN 사용)
     */
    @Query("SELECT f.id AS fileId, f.fileName AS fileName, f.fileSize AS fileSize, " +
           "f.fileType AS fileType, CAST(f.category AS string) AS category, " +
           "f.uploaderId AS uploaderId, u.displayName AS uploaderName, " +
           "f.uploadedAt AS uploadedAt, f.downloadCount AS downloadCount, " +
           "f.thumbnailPath AS thumbnailPath " +
           "FROM FileMetadataEntity f LEFT JOIN UserEntity u ON f.uploaderId = u.id " +
           "WHERE f.roomId = :roomId AND f.isDeleted = false " +
           "ORDER BY f.uploadedAt DESC")
    List<FileListItemProjection> findRoomFilesWithUploader(@Param("roomId") Long roomId);
}