package com.beam;

import com.beam.exception.FileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 파일 저장 서비스
 * - 파일 업로드/다운로드
 * - 썸네일 비동기 생성
 * - 파일 보안 검증
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.thumbnail.width:200}")
    private int thumbnailWidth;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:10485760}")
    private Long maxFileSize;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileSecurityValidator fileSecurityValidator;

    public FileMetadataEntity storeFile(MultipartFile file, Long uploaderId,
                                         String conversationId, Long roomId) {
        if (file.isEmpty()) {
            throw FileException.empty();
        }

        if (file.getSize() > maxFileSize) {
            throw FileException.tooLarge(file.getSize(), maxFileSize);
        }

        // 보안 검증 추가
        fileSecurityValidator.validateFile(file);

        try {
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            // 파일명 sanitize
            originalFilename = fileSecurityValidator.sanitizeFilename(originalFilename);

            String fileExtension = getFileExtension(originalFilename);
            String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Path Traversal 방지: 업로드 경로 검증
            Path targetLocation = uploadPath.resolve(uniqueFileName).normalize();
            if (!targetLocation.startsWith(uploadPath)) {
                throw new SecurityException("Invalid file path");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 클라이언트 제공 MIME 대신 매직 바이트 기반 검증된 MIME 사용
            String mimeType;
            try {
                mimeType = fileSecurityValidator.detectMimeTypeFromContent(file);
            } catch (IOException e) {
                mimeType = fileSecurityValidator.getVerifiedMimeType(file);
            }
            FileMetadataEntity.FileCategory category = FileMetadataEntity.getCategoryFromMimeType(mimeType);

            FileMetadataEntity metadata = FileMetadataEntity.builder()
                .fileName(originalFilename)
                .filePath(uniqueFileName)
                .fileType(mimeType != null ? mimeType : "application/octet-stream")
                .fileSize(file.getSize())
                .uploaderId(uploaderId)
                .conversationId(conversationId)
                .roomId(roomId)
                .category(category)
                .uploadedAt(LocalDateTime.now())
                .build();

            FileMetadataEntity savedMetadata = fileMetadataRepository.save(metadata);

            // 이미지인 경우 비동기로 썸네일 생성
            if (category == FileMetadataEntity.FileCategory.IMAGE) {
                generateThumbnailAsync(savedMetadata.getId(), targetLocation.toString(), uniqueFileName);
            }

            return savedMetadata;

        } catch (IOException ex) {
            throw FileException.uploadFailed("Could not store file", ex);
        }
    }

    public Resource loadFileAsResource(Long fileId) {
        try {
            FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> FileException.notFound(fileId));

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(metadata.getFilePath()).normalize();

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                metadata.incrementDownloadCount();
                fileMetadataRepository.save(metadata);
                return resource;
            } else {
                throw FileException.notFound(fileId);
            }
        } catch (FileException ex) {
            throw ex;
        } catch (Exception ex) {
            throw FileException.notFound(fileId);
        }
    }

    public Resource loadThumbnailAsResource(Long fileId) {
        try {
            FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> FileException.notFound(fileId));

            if (metadata.getThumbnailPath() == null) {
                throw FileException.thumbnailNotFound(fileId);
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path thumbnailPath = uploadPath.resolve(metadata.getThumbnailPath()).normalize();

            Resource resource = new UrlResource(thumbnailPath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw FileException.thumbnailNotFound(fileId);
            }
        } catch (FileException ex) {
            throw ex;
        } catch (Exception ex) {
            throw FileException.thumbnailNotFound(fileId);
        }
    }

    public void deleteFile(Long fileId, Long userId) {
        FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
            .orElseThrow(() -> FileException.notFound(fileId));

        if (!metadata.getUploaderId().equals(userId)) {
            throw FileException.deleteDenied(fileId, userId);
        }

        metadata.setIsDeleted(true);
        metadata.setDeletedAt(LocalDateTime.now());
        fileMetadataRepository.save(metadata);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(metadata.getFilePath()).normalize();
            Files.deleteIfExists(filePath);

            if (metadata.getThumbnailPath() != null) {
                Path thumbnailPath = uploadPath.resolve(metadata.getThumbnailPath()).normalize();
                Files.deleteIfExists(thumbnailPath);
            }
        } catch (IOException ex) {
            logger.warn("Failed to delete physical file: {}", ex.getMessage());
        }
    }

    /**
     * 비동기 썸네일 생성
     * 파일 업로드 후 백그라운드에서 썸네일을 생성하고 DB 업데이트
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> generateThumbnailAsync(Long fileId, String originalFilePath, String originalFileName) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Starting async thumbnail generation for file: {}", fileId);
                String thumbnailPath = generateThumbnail(originalFilePath, originalFileName);

                // DB 업데이트
                fileMetadataRepository.findById(fileId).ifPresent(metadata -> {
                    metadata.setThumbnailPath(thumbnailPath);
                    fileMetadataRepository.save(metadata);
                    logger.debug("Thumbnail generated and saved for file: {}", fileId);
                });
            } catch (Exception e) {
                logger.warn("Failed to generate thumbnail for file {}: {}", fileId, e.getMessage());
            }
        });
    }

    private String generateThumbnail(String originalFilePath, String originalFileName) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(originalFilePath));
        if (originalImage == null) {
            throw new IOException("Cannot read image file");
        }

        int thumbnailHeight = (int) (originalImage.getHeight() * ((double) thumbnailWidth / originalImage.getWidth()));

        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        g.dispose();

        String thumbnailFileName = "thumb_" + originalFileName;
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path thumbnailPath = uploadPath.resolve(thumbnailFileName);

        String extension = getFileExtension(originalFileName).toLowerCase();
        String formatName = extension.equals("jpg") ? "jpeg" : extension;
        ImageIO.write(thumbnail, formatName, thumbnailPath.toFile());

        return thumbnailFileName;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}