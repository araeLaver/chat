package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload/dm")
    public ResponseEntity<?> uploadFileToDM(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            FileMetadataEntity metadata = fileStorageService.storeFile(file, userId, conversationId, null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileId", metadata.getId());
            response.put("fileName", metadata.getFileName());
            response.put("fileSize", metadata.getFileSize());
            response.put("fileType", metadata.getFileType());
            response.put("category", metadata.getCategory().toString());
            response.put("hasThumbnail", metadata.getThumbnailPath() != null);
            response.put("message", "File uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/upload/room")
    public ResponseEntity<?> uploadFileToRoom(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("roomId") Long roomId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            FileMetadataEntity metadata = fileStorageService.storeFile(file, userId, null, roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileId", metadata.getId());
            response.put("fileName", metadata.getFileName());
            response.put("fileSize", metadata.getFileSize());
            response.put("fileType", metadata.getFileType());
            response.put("category", metadata.getCategory().toString());
            response.put("hasThumbnail", metadata.getThumbnailPath() != null);
            response.put("message", "File uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            FileMetadataEntity metadata = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

            Resource resource = fileStorageService.loadFileAsResource(fileId);

            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                contentType = "application/octet-stream";
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/thumbnail/{fileId}")
    public ResponseEntity<Resource> getThumbnail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long fileId) {
        try {
            Resource resource = fileStorageService.loadThumbnailAsResource(fileId);

            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationFiles(
            @RequestHeader("Authorization") String token,
            @PathVariable String conversationId) {
        try {
            List<FileMetadataEntity> files = fileMetadataRepository
                .findByConversationIdAndIsDeletedFalseOrderByUploadedAtDesc(conversationId);

            List<Map<String, Object>> result = files.stream().map(file -> {
                Optional<UserEntity> uploaderOpt = userRepository.findById(file.getUploaderId());

                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("fileId", file.getId());
                fileMap.put("fileName", file.getFileName());
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("fileType", file.getFileType());
                fileMap.put("category", file.getCategory().toString());
                fileMap.put("uploaderId", file.getUploaderId());
                fileMap.put("uploaderName", uploaderOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                fileMap.put("uploadedAt", file.getUploadedAt().toString());
                fileMap.put("downloadCount", file.getDownloadCount());
                fileMap.put("hasThumbnail", file.getThumbnailPath() != null);

                return fileMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomFiles(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        try {
            List<FileMetadataEntity> files = fileMetadataRepository
                .findByRoomIdAndIsDeletedFalseOrderByUploadedAtDesc(roomId);

            List<Map<String, Object>> result = files.stream().map(file -> {
                Optional<UserEntity> uploaderOpt = userRepository.findById(file.getUploaderId());

                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("fileId", file.getId());
                fileMap.put("fileName", file.getFileName());
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("fileType", file.getFileType());
                fileMap.put("category", file.getCategory().toString());
                fileMap.put("uploaderId", file.getUploaderId());
                fileMap.put("uploaderName", uploaderOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
                fileMap.put("uploadedAt", file.getUploadedAt().toString());
                fileMap.put("downloadCount", file.getDownloadCount());
                fileMap.put("hasThumbnail", file.getThumbnailPath() != null);

                return fileMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/my-files")
    public ResponseEntity<?> getMyFiles(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            List<FileMetadataEntity> files = fileMetadataRepository
                .findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(userId);

            Long totalSize = fileMetadataRepository.getTotalFileSizeByUser(userId);

            List<Map<String, Object>> fileList = files.stream().map(file -> {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("fileId", file.getId());
                fileMap.put("fileName", file.getFileName());
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("fileType", file.getFileType());
                fileMap.put("category", file.getCategory().toString());
                fileMap.put("uploadedAt", file.getUploadedAt().toString());
                fileMap.put("downloadCount", file.getDownloadCount());
                fileMap.put("conversationId", file.getConversationId());
                fileMap.put("roomId", file.getRoomId());
                fileMap.put("hasThumbnail", file.getThumbnailPath() != null);

                return fileMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("files", fileList);
            response.put("totalFiles", files.size());
            response.put("totalSize", totalSize != null ? totalSize : 0);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long fileId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Long userId = jwtUtil.getUserIdFromToken(jwtToken);

            fileStorageService.deleteFile(fileId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/info/{fileId}")
    public ResponseEntity<?> getFileInfo(
            @RequestHeader("Authorization") String token,
            @PathVariable Long fileId) {
        try {
            FileMetadataEntity file = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

            Optional<UserEntity> uploaderOpt = userRepository.findById(file.getUploaderId());

            Map<String, Object> response = new HashMap<>();
            response.put("fileId", file.getId());
            response.put("fileName", file.getFileName());
            response.put("fileSize", file.getFileSize());
            response.put("fileType", file.getFileType());
            response.put("category", file.getCategory().toString());
            response.put("uploaderId", file.getUploaderId());
            response.put("uploaderName", uploaderOpt.map(UserEntity::getDisplayName).orElse("Unknown"));
            response.put("uploadedAt", file.getUploadedAt().toString());
            response.put("downloadCount", file.getDownloadCount());
            response.put("conversationId", file.getConversationId());
            response.put("roomId", file.getRoomId());
            response.put("hasThumbnail", file.getThumbnailPath() != null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}