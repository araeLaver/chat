package com.beam.dto;

import java.time.LocalDateTime;

/**
 * 파일 목록 조회를 위한 Projection 인터페이스
 * N+1 쿼리 문제를 해결하기 위해 JOIN으로 한 번에 조회
 */
public interface FileListItemProjection {

    Long getFileId();
    String getFileName();
    Long getFileSize();
    String getFileType();
    String getCategory();
    Long getUploaderId();
    String getUploaderName();
    LocalDateTime getUploadedAt();
    Integer getDownloadCount();
    String getThumbnailPath();
}
