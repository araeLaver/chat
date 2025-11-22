package com.beam;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 파일 업로드 보안 검증 유틸리티
 * - 허용된 파일 타입 화이트리스트
 * - 매직 넘버(파일 시그니처) 검증
 * - Path Traversal 공격 방지
 * - 악성 파일 확장자 차단
 */
@Component
public class FileSecurityValidator {

    // 허용된 파일 확장자 (화이트리스트)
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "webp", "bmp",  // 이미지
        "mp4", "webm", "mov", "avi",                 // 비디오
        "mp3", "wav", "ogg", "m4a",                  // 오디오
        "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx"  // 문서
    );

    // 차단된 파일 확장자 (블랙리스트)
    private static final List<String> BLOCKED_EXTENSIONS = Arrays.asList(
        "exe", "bat", "cmd", "sh", "ps1", "msi",     // 실행 파일
        "jar", "war", "dll", "so", "dylib",          // 바이너리
        "js", "vbs", "app", "deb", "rpm",            // 스크립트/패키지
        "php", "asp", "aspx", "jsp", "py", "rb"      // 서버 스크립트
    );

    // 파일 시그니처 (매직 넘버) - 주요 파일 타입
    private static final Map<String, byte[][]> FILE_SIGNATURES = new HashMap<>();

    static {
        // JPEG
        FILE_SIGNATURES.put("jpg", new byte[][]{
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        });

        // PNG
        FILE_SIGNATURES.put("png", new byte[][]{
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        });

        // GIF
        FILE_SIGNATURES.put("gif", new byte[][]{
            {0x47, 0x49, 0x46, 0x38, 0x37, 0x61},  // GIF87a
            {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}   // GIF89a
        });

        // PDF
        FILE_SIGNATURES.put("pdf", new byte[][]{
            {0x25, 0x50, 0x44, 0x46}  // %PDF
        });

        // MP4
        FILE_SIGNATURES.put("mp4", new byte[][]{
            {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70},
            {0x00, 0x00, 0x00, 0x1C, 0x66, 0x74, 0x79, 0x70}
        });

        // MP3
        FILE_SIGNATURES.put("mp3", new byte[][]{
            {0x49, 0x44, 0x33},  // ID3
            {(byte) 0xFF, (byte) 0xFB}
        });

        // ZIP (DOCX, XLSX, etc.)
        FILE_SIGNATURES.put("zip", new byte[][]{
            {0x50, 0x4B, 0x03, 0x04},
            {0x50, 0x4B, 0x05, 0x06},
            {0x50, 0x4B, 0x07, 0x08}
        });
    }

    /**
     * 파일 업로드 보안 검증
     */
    public void validateFile(MultipartFile file) throws SecurityException {
        if (file == null || file.isEmpty()) {
            throw new SecurityException("파일이 비어있습니다");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new SecurityException("파일명이 유효하지 않습니다");
        }

        // 1. Path Traversal 공격 방지
        validateFileName(originalFilename);

        // 2. 확장자 검증
        String extension = getFileExtension(originalFilename);
        validateExtension(extension);

        // 3. MIME 타입 검증
        String mimeType = file.getContentType();
        validateMimeType(mimeType, extension);

        // 4. 매직 넘버(파일 시그니처) 검증
        try {
            validateFileSignature(file, extension);
        } catch (IOException e) {
            throw new SecurityException("파일 내용을 읽을 수 없습니다");
        }
    }

    /**
     * 파일명 검증 (Path Traversal 방지)
     */
    private void validateFileName(String filename) {
        // null 바이트 주입 방지
        if (filename.contains("\0")) {
            throw new SecurityException("파일명에 유효하지 않은 문자가 포함되어 있습니다");
        }

        // 경로 조작 문자 차단
        if (filename.contains("..") ||
            filename.contains("/") ||
            filename.contains("\\") ||
            filename.contains(":")) {
            throw new SecurityException("파일명에 경로 문자가 포함되어 있습니다");
        }

        // 파일명 길이 제한
        if (filename.length() > 255) {
            throw new SecurityException("파일명이 너무 깁니다 (최대 255자)");
        }
    }

    /**
     * 확장자 검증
     */
    private void validateExtension(String extension) {
        String lowerExt = extension.toLowerCase();

        // 블랙리스트 확인
        if (BLOCKED_EXTENSIONS.contains(lowerExt)) {
            throw new SecurityException("업로드가 금지된 파일 형식입니다: " + extension);
        }

        // 화이트리스트 확인
        if (!ALLOWED_EXTENSIONS.contains(lowerExt)) {
            throw new SecurityException("지원하지 않는 파일 형식입니다: " + extension);
        }
    }

    /**
     * MIME 타입 검증
     */
    private void validateMimeType(String mimeType, String extension) {
        if (mimeType == null || mimeType.isEmpty()) {
            throw new SecurityException("MIME 타입을 확인할 수 없습니다");
        }

        // 이미지 확장자인데 이미지 MIME이 아닌 경우
        if (Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp").contains(extension.toLowerCase())) {
            if (!mimeType.startsWith("image/")) {
                throw new SecurityException("이미지 파일의 MIME 타입이 올바르지 않습니다");
            }
        }

        // 비디오 확장자인데 비디오 MIME이 아닌 경우
        if (Arrays.asList("mp4", "webm", "mov", "avi").contains(extension.toLowerCase())) {
            if (!mimeType.startsWith("video/")) {
                throw new SecurityException("비디오 파일의 MIME 타입이 올바르지 않습니다");
            }
        }
    }

    /**
     * 파일 시그니처(매직 넘버) 검증
     */
    private void validateFileSignature(MultipartFile file, String extension) throws IOException {
        String lowerExt = extension.toLowerCase();

        // ZIP 기반 문서 형식 (docx, xlsx, etc.)
        if (Arrays.asList("docx", "xlsx", "pptx").contains(lowerExt)) {
            lowerExt = "zip";
        } else if (lowerExt.equals("jpeg")) {
            lowerExt = "jpg";
        }

        byte[][] signatures = FILE_SIGNATURES.get(lowerExt);
        if (signatures == null) {
            // 매직 넘버가 정의되지 않은 파일은 건너뜀
            return;
        }

        try (InputStream is = file.getInputStream()) {
            // 파일의 처음 20바이트 읽기
            byte[] fileHeader = new byte[20];
            int bytesRead = is.read(fileHeader);

            if (bytesRead < 4) {
                throw new SecurityException("파일이 너무 작습니다");
            }

            // 시그니처 매칭
            boolean matches = false;
            for (byte[] signature : signatures) {
                if (matchesSignature(fileHeader, signature)) {
                    matches = true;
                    break;
                }
            }

            if (!matches) {
                throw new SecurityException("파일 형식이 확장자와 일치하지 않습니다");
            }
        }
    }

    /**
     * 바이트 시그니처 매칭
     */
    private boolean matchesSignature(byte[] fileHeader, byte[] signature) {
        if (fileHeader.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (fileHeader[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new SecurityException("파일 확장자가 없습니다");
        }

        int lastDot = filename.lastIndexOf(".");
        if (lastDot == filename.length() - 1) {
            throw new SecurityException("파일 확장자가 유효하지 않습니다");
        }

        return filename.substring(lastDot + 1);
    }

    /**
     * 안전한 파일명 생성
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }

        // 특수문자 제거, 알파벳/숫자/점/하이픈/언더스코어만 허용
        return filename.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }
}
