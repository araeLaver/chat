package com.beam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final int THUMBNAIL_WIDTH = 200;

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
            throw new RuntimeException("Cannot store empty file");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum limit of " + (maxFileSize / 1024 / 1024) + "MB");
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

            String mimeType = file.getContentType();
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

            if (category == FileMetadataEntity.FileCategory.IMAGE) {
                try {
                    String thumbnailPath = generateThumbnail(targetLocation.toString(), uniqueFileName);
                    metadata.setThumbnailPath(thumbnailPath);
                } catch (Exception e) {
                    logger.warn("Failed to generate thumbnail: {}", e.getMessage());
                }
            }

            return fileMetadataRepository.save(metadata);

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(Long fileId) {
        try {
            FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(metadata.getFilePath()).normalize();

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                metadata.incrementDownloadCount();
                fileMetadataRepository.save(metadata);
                return resource;
            } else {
                throw new RuntimeException("File not found");
            }
        } catch (Exception ex) {
            throw new RuntimeException("File not found", ex);
        }
    }

    public Resource loadThumbnailAsResource(Long fileId) {
        try {
            FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

            if (metadata.getThumbnailPath() == null) {
                throw new RuntimeException("Thumbnail not available");
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path thumbnailPath = uploadPath.resolve(metadata.getThumbnailPath()).normalize();

            Resource resource = new UrlResource(thumbnailPath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Thumbnail not found");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Thumbnail not found", ex);
        }
    }

    public void deleteFile(Long fileId, Long userId) {
        FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

        if (!metadata.getUploaderId().equals(userId)) {
            throw new RuntimeException("No permission to delete this file");
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

    private String generateThumbnail(String originalFilePath, String originalFileName) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(originalFilePath));
        if (originalImage == null) {
            throw new IOException("Cannot read image file");
        }

        int thumbnailHeight = (int) (originalImage.getHeight() * ((double) THUMBNAIL_WIDTH / originalImage.getWidth()));

        BufferedImage thumbnail = new BufferedImage(THUMBNAIL_WIDTH, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, THUMBNAIL_WIDTH, thumbnailHeight, null);
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