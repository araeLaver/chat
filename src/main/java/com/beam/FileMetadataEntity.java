package com.beam;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_uploader", columnList = "uploaderId"),
    @Index(name = "idx_file_conversation", columnList = "conversationId"),
    @Index(name = "idx_room", columnList = "roomId")
})
public class FileMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private Long uploaderId;

    @Column(length = 100)
    private String conversationId;

    @Column
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileCategory category = FileCategory.OTHER;

    @Column(length = 500)
    private String thumbnailPath;

    @Column(nullable = false)
    private Integer downloadCount = 0;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public enum FileCategory {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        OTHER
    }

    public FileMetadataEntity() {
    }

    public FileMetadataEntity(Long id, String fileName, String filePath, String fileType, Long fileSize,
                              Long uploaderId, String conversationId, Long roomId, FileCategory category,
                              String thumbnailPath, Integer downloadCount, LocalDateTime expiresAt,
                              Boolean isDeleted, LocalDateTime deletedAt, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploaderId = uploaderId;
        this.conversationId = conversationId;
        this.roomId = roomId;
        this.category = category;
        this.thumbnailPath = thumbnailPath;
        this.downloadCount = downloadCount;
        this.expiresAt = expiresAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.uploadedAt = uploadedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.category == null) {
            this.category = FileCategory.OTHER;
        }
        if (this.downloadCount == null) {
            this.downloadCount = 0;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public static FileCategory getCategoryFromMimeType(String mimeType) {
        if (mimeType == null) return FileCategory.OTHER;

        if (mimeType.startsWith("image/")) return FileCategory.IMAGE;
        if (mimeType.startsWith("video/")) return FileCategory.VIDEO;
        if (mimeType.startsWith("audio/")) return FileCategory.AUDIO;
        if (mimeType.startsWith("application/pdf") ||
            mimeType.startsWith("application/msword") ||
            mimeType.startsWith("application/vnd.")) return FileCategory.DOCUMENT;

        return FileCategory.OTHER;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public FileCategory getCategory() {
        return category;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public void setCategory(FileCategory category) {
        this.category = category;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String fileName;
        private String filePath;
        private String fileType;
        private Long fileSize;
        private Long uploaderId;
        private String conversationId;
        private Long roomId;
        private FileCategory category = FileCategory.OTHER;
        private String thumbnailPath;
        private Integer downloadCount = 0;
        private LocalDateTime expiresAt;
        private Boolean isDeleted = false;
        private LocalDateTime deletedAt;
        private LocalDateTime uploadedAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder uploaderId(Long uploaderId) {
            this.uploaderId = uploaderId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder roomId(Long roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder category(FileCategory category) {
            this.category = category;
            return this;
        }

        public Builder thumbnailPath(String thumbnailPath) {
            this.thumbnailPath = thumbnailPath;
            return this;
        }

        public Builder downloadCount(Integer downloadCount) {
            this.downloadCount = downloadCount;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder isDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        public Builder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Builder uploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public FileMetadataEntity build() {
            FileMetadataEntity entity = new FileMetadataEntity();
            entity.id = this.id;
            entity.fileName = this.fileName;
            entity.filePath = this.filePath;
            entity.fileType = this.fileType;
            entity.fileSize = this.fileSize;
            entity.uploaderId = this.uploaderId;
            entity.conversationId = this.conversationId;
            entity.roomId = this.roomId;
            entity.category = this.category;
            entity.thumbnailPath = this.thumbnailPath;
            entity.downloadCount = this.downloadCount;
            entity.expiresAt = this.expiresAt;
            entity.isDeleted = this.isDeleted;
            entity.deletedAt = this.deletedAt;
            entity.uploadedAt = this.uploadedAt;
            return entity;
        }
    }
}
