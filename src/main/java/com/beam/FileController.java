package com.beam;

import com.beam.util.AuthUtil;
import com.beam.util.FileMetadataMapper;
import com.beam.util.ResponseHelper;
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
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            FileMetadataEntity metadata = fileStorageService.storeFile(file, userId, conversationId, null);
            return ResponseEntity.ok(FileMetadataMapper.toUploadResponse(metadata));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @PostMapping("/upload/room")
    public ResponseEntity<?> uploadFileToRoom(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("roomId") Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            FileMetadataEntity metadata = fileStorageService.storeFile(file, userId, null, roomId);
            return ResponseEntity.ok(FileMetadataMapper.toUploadResponse(metadata));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
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
            String contentType = determineContentType(resource, request);

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

            List<Map<String, Object>> result = files.stream()
                .map(file -> buildFileListItem(file))
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomFiles(
            @RequestHeader("Authorization") String token,
            @PathVariable Long roomId) {
        try {
            List<FileMetadataEntity> files = fileMetadataRepository
                .findByRoomIdAndIsDeletedFalseOrderByUploadedAtDesc(roomId);

            List<Map<String, Object>> result = files.stream()
                .map(file -> buildFileListItem(file))
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @GetMapping("/my-files")
    public ResponseEntity<?> getMyFiles(@RequestHeader("Authorization") String token) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);

            List<FileMetadataEntity> files = fileMetadataRepository
                .findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(userId);

            Long totalSize = fileMetadataRepository.getTotalFileSizeByUser(userId);

            List<Map<String, Object>> fileList = files.stream()
                .map(file -> buildMyFileListItem(file))
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("files", fileList);
            response.put("totalFiles", files.size());
            response.put("totalSize", totalSize != null ? totalSize : 0);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("Authorization") String token,
            @PathVariable Long fileId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            fileStorageService.deleteFile(fileId, userId);
            return ResponseEntity.ok(ResponseHelper.success("File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @GetMapping("/info/{fileId}")
    public ResponseEntity<?> getFileInfo(
            @RequestHeader("Authorization") String token,
            @PathVariable Long fileId) {
        try {
            FileMetadataEntity file = fileMetadataRepository.findByIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

            Map<String, Object> response = buildFileListItem(file);
            response.put("conversationId", file.getConversationId());
            response.put("roomId", file.getRoomId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    // Helper methods
    private String determineContentType(Resource resource, HttpServletRequest request) {
        try {
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    private Map<String, Object> buildFileListItem(FileMetadataEntity file) {
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
    }

    private Map<String, Object> buildMyFileListItem(FileMetadataEntity file) {
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
    }
}
