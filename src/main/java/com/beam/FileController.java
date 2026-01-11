package com.beam;

import com.beam.dto.FileListItemProjection;
import com.beam.util.AuthUtil;
import com.beam.util.FileMetadataMapper;
import com.beam.util.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "File Management", description = "파일 업로드/다운로드 및 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "DM 파일 업로드", description = "다이렉트 메시지 대화에 파일을 업로드합니다. 최대 10MB까지 지원됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "파일 크기 초과 또는 허용되지 않는 파일 형식"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/upload/dm")
    public ResponseEntity<?> uploadFileToDM(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "업로드할 파일", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "대화 ID", required = true) @RequestParam("conversationId") String conversationId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            FileMetadataEntity metadata = fileStorageService.storeFile(file, userId, conversationId, null);
            return ResponseEntity.ok(FileMetadataMapper.toUploadResponse(metadata));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "채팅방 파일 업로드", description = "그룹 채팅방에 파일을 업로드합니다. 최대 10MB까지 지원됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "파일 크기 초과 또는 허용되지 않는 파일 형식"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/upload/room")
    public ResponseEntity<?> uploadFileToRoom(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "업로드할 파일", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "채팅방 ID", required = true) @RequestParam("roomId") Long roomId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            FileMetadataEntity metadata = fileStorageService.storeFile(file, userId, null, roomId);
            return ResponseEntity.ok(FileMetadataMapper.toUploadResponse(metadata));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "파일 다운로드", description = "파일 ID로 파일을 다운로드합니다. 다운로드 횟수가 자동으로 기록됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 다운로드 성공", content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "400", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "파일 ID", required = true) @PathVariable Long fileId,
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

    @Operation(summary = "썸네일 조회", description = "이미지 파일의 썸네일을 조회합니다. 썸네일은 업로드 시 자동 생성됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "썸네일 반환", content = @Content(mediaType = "image/jpeg")),
        @ApiResponse(responseCode = "400", description = "썸네일 없음 또는 파일을 찾을 수 없음")
    })
    @GetMapping("/thumbnail/{fileId}")
    public ResponseEntity<Resource> getThumbnail(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "파일 ID", required = true) @PathVariable Long fileId) {
        try {
            Resource resource = fileStorageService.loadThumbnailAsResource(fileId);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "대화 파일 목록", description = "특정 DM 대화의 모든 파일 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationFiles(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "대화 ID", required = true) @PathVariable String conversationId) {
        try {
            // N+1 문제 해결: JOIN으로 한 번에 조회
            List<FileListItemProjection> files = fileMetadataRepository
                .findConversationFilesWithUploader(conversationId);

            List<Map<String, Object>> result = files.stream()
                .map(this::buildFileListItemFromProjection)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "채팅방 파일 목록", description = "특정 채팅방의 모든 파일 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 목록 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomFiles(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "채팅방 ID", required = true) @PathVariable Long roomId) {
        try {
            // N+1 문제 해결: JOIN으로 한 번에 조회
            List<FileListItemProjection> files = fileMetadataRepository
                .findRoomFilesWithUploader(roomId);

            List<Map<String, Object>> result = files.stream()
                .map(this::buildFileListItemFromProjection)
                .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "내 파일 목록", description = "현재 사용자가 업로드한 모든 파일 목록과 총 용량을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 목록 및 통계 반환"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my-files")
    public ResponseEntity<?> getMyFiles(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token) {
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

    @Operation(summary = "파일 삭제", description = "자신이 업로드한 파일을 삭제합니다. 소프트 삭제로 처리됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "파일을 찾을 수 없음 또는 권한 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "파일 ID", required = true) @PathVariable Long fileId) {
        try {
            Long userId = AuthUtil.extractUserId(token, jwtUtil);
            fileStorageService.deleteFile(fileId, userId);
            return ResponseEntity.ok(ResponseHelper.success("File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseHelper.errorFromException(e));
        }
    }

    @Operation(summary = "파일 정보 조회", description = "특정 파일의 상세 메타데이터를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 정보 반환"),
        @ApiResponse(responseCode = "400", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/info/{fileId}")
    public ResponseEntity<?> getFileInfo(
            @Parameter(description = "JWT 토큰", required = true) @RequestHeader("Authorization") String token,
            @Parameter(description = "파일 ID", required = true) @PathVariable Long fileId) {
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

    /**
     * Projection에서 파일 목록 아이템 생성 (N+1 문제 해결)
     */
    private Map<String, Object> buildFileListItemFromProjection(FileListItemProjection file) {
        Map<String, Object> fileMap = new HashMap<>();
        fileMap.put("fileId", file.getFileId());
        fileMap.put("fileName", file.getFileName());
        fileMap.put("fileSize", file.getFileSize());
        fileMap.put("fileType", file.getFileType());
        fileMap.put("category", file.getCategory());
        fileMap.put("uploaderId", file.getUploaderId());
        fileMap.put("uploaderName", file.getUploaderName() != null ? file.getUploaderName() : "Unknown");
        fileMap.put("uploadedAt", file.getUploadedAt().toString());
        fileMap.put("downloadCount", file.getDownloadCount());
        fileMap.put("hasThumbnail", file.getThumbnailPath() != null);

        return fileMap;
    }
}
