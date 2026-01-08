package com.beam.util;

import com.beam.FileMetadataEntity;
import com.beam.UserEntity;
import com.beam.UserRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * FileMetadataEntity를 응답 Map으로 변환하는 유틸리티
 */
public final class FileMetadataMapper {

    private FileMetadataMapper() {
        // 유틸리티 클래스
    }

    /**
     * 파일 업로드 응답 생성
     */
    public static Map<String, Object> toUploadResponse(FileMetadataEntity metadata) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fileId", metadata.getId());
        response.put("fileName", metadata.getFileName());
        response.put("fileSize", metadata.getFileSize());
        response.put("fileType", metadata.getFileType());
        response.put("category", metadata.getCategory().toString());
        response.put("hasThumbnail", metadata.getThumbnailPath() != null);
        response.put("message", "File uploaded successfully");
        return response;
    }

    /**
     * 파일 목록 아이템 응답 생성
     */
    public static Map<String, Object> toListItem(FileMetadataEntity file) {
        Map<String, Object> item = new HashMap<>();
        item.put("fileId", file.getId());
        item.put("fileName", file.getFileName());
        item.put("fileSize", file.getFileSize());
        item.put("fileType", file.getFileType());
        item.put("category", file.getCategory().toString());
        item.put("hasThumbnail", file.getThumbnailPath() != null);
        item.put("uploadedAt", file.getUploadedAt().toString());
        return item;
    }

    /**
     * 업로더 정보를 포함한 파일 목록 아이템 응답 생성
     */
    public static Map<String, Object> toListItemWithUploader(FileMetadataEntity file, UserRepository userRepository) {
        Map<String, Object> item = toListItem(file);
        userRepository.findById(file.getUploaderId())
                .ifPresent(uploader -> {
                    item.put("uploaderName", uploader.getDisplayName());
                    item.put("uploaderId", uploader.getId());
                });
        return item;
    }

    /**
     * 상세 파일 정보 응답 생성
     */
    public static Map<String, Object> toDetailResponse(FileMetadataEntity file) {
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", file.getId());
        response.put("fileName", file.getFileName());
        response.put("fileSize", file.getFileSize());
        response.put("fileType", file.getFileType());
        response.put("category", file.getCategory().toString());
        response.put("hasThumbnail", file.getThumbnailPath() != null);
        response.put("uploadedAt", file.getUploadedAt().toString());
        response.put("conversationId", file.getConversationId());
        response.put("roomId", file.getRoomId());
        return response;
    }
}
