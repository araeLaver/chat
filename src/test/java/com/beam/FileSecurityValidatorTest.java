package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FileSecurityValidator Unit Tests")
class FileSecurityValidatorTest {

    private FileSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileSecurityValidator();
    }

    @Nested
    @DisplayName("Path Traversal Prevention Tests")
    class PathTraversalTests {

        @Test
        @DisplayName("Should reject file with parent directory traversal")
        void shouldRejectParentDirectoryTraversal() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "../../../etc/passwd",
                    "text/plain",
                    "malicious content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("경로 문자");
        }

        @Test
        @DisplayName("Should reject file with forward slash")
        void shouldRejectForwardSlash() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "path/to/file.txt",
                    "text/plain",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("경로 문자");
        }

        @Test
        @DisplayName("Should reject file with backslash")
        void shouldRejectBackslash() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "path\\to\\file.txt",
                    "text/plain",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("경로 문자");
        }

        @Test
        @DisplayName("Should reject file with null byte injection")
        void shouldRejectNullByteInjection() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "file.txt\0.exe",
                    "text/plain",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("유효하지 않은 문자");
        }

        @Test
        @DisplayName("Should reject filename with colon")
        void shouldRejectColonInFilename() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "C:file.txt",
                    "text/plain",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("경로 문자");
        }
    }

    @Nested
    @DisplayName("Extension Validation Tests")
    class ExtensionValidationTests {

        @Test
        @DisplayName("Should accept allowed image extensions")
        void shouldAcceptAllowedImageExtensions() {
            // Given - Valid PNG signature
            byte[] pngSignature = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x00
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image.png",
                    "image/png",
                    pngSignature
            );

            // When & Then
            assertThatCode(() -> validator.validateFile(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject executable files")
        void shouldRejectExecutableFiles() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malware.exe",
                    "application/x-msdownload",
                    "MZ...".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("업로드가 금지된 파일 형식");
        }

        @Test
        @DisplayName("Should reject shell scripts")
        void shouldRejectShellScripts() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "script.sh",
                    "application/x-sh",
                    "#!/bin/bash".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("업로드가 금지된 파일 형식");
        }

        @Test
        @DisplayName("Should reject PHP files")
        void shouldRejectPHPFiles() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "webshell.php",
                    "text/x-php",
                    "<?php echo 'hacked'; ?>".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("업로드가 금지된 파일 형식");
        }

        @Test
        @DisplayName("Should reject JAR files")
        void shouldRejectJARFiles() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malicious.jar",
                    "application/java-archive",
                    "PK...".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("업로드가 금지된 파일 형식");
        }

        @Test
        @DisplayName("Should reject unsupported extensions")
        void shouldRejectUnsupportedExtensions() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "data.xyz",
                    "application/octet-stream",
                    "some data".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("지원하지 않는 파일 형식");
        }

        @Test
        @DisplayName("Should reject file without extension")
        void shouldRejectFileWithoutExtension() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "noextension",
                    "application/octet-stream",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("확장자");
        }
    }

    @Nested
    @DisplayName("MIME Type Validation Tests")
    class MimeTypeValidationTests {

        @Test
        @DisplayName("Should reject image extension with non-image MIME type")
        void shouldRejectImageExtensionWithWrongMime() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "fake.jpg",
                    "text/plain",
                    "not an image".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("MIME 타입");
        }

        @Test
        @DisplayName("Should reject video extension with non-video MIME type")
        void shouldRejectVideoExtensionWithWrongMime() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "fake.mp4",
                    "text/plain",
                    "not a video".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("MIME 타입");
        }

        @Test
        @DisplayName("Should reject file with null MIME type")
        void shouldRejectNullMimeType() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    null,
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("MIME 타입");
        }
    }

    @Nested
    @DisplayName("File Signature (Magic Number) Validation Tests")
    class FileSignatureTests {

        @Test
        @DisplayName("Should accept valid PNG file with correct signature")
        void shouldAcceptValidPNG() {
            // Given - Valid PNG signature
            byte[] pngContent = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "valid.png",
                    "image/png",
                    pngContent
            );

            // When & Then
            assertThatCode(() -> validator.validateFile(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid JPEG file with correct signature")
        void shouldAcceptValidJPEG() {
            // Given - Valid JPEG signature
            byte[] jpegContent = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "valid.jpg",
                    "image/jpeg",
                    jpegContent
            );

            // When & Then
            assertThatCode(() -> validator.validateFile(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid GIF87a file")
        void shouldAcceptValidGIF87a() {
            // Given - Valid GIF87a signature
            byte[] gifContent = new byte[]{
                    0x47, 0x49, 0x46, 0x38, 0x37, 0x61,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "valid.gif",
                    "image/gif",
                    gifContent
            );

            // When & Then
            assertThatCode(() -> validator.validateFile(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid PDF file with correct signature")
        void shouldAcceptValidPDF() {
            // Given - Valid PDF signature
            byte[] pdfContent = new byte[]{
                    0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
                    0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "valid.pdf",
                    "application/pdf",
                    pdfContent
            );

            // When & Then
            assertThatCode(() -> validator.validateFile(file))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject PNG extension with JPEG content")
        void shouldRejectMismatchedSignature() {
            // Given - JPEG content disguised as PNG
            byte[] jpegContent = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
            };

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "fake.png",
                    "image/png",
                    jpegContent
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("파일 형식이 확장자와 일치하지 않습니다");
        }

        @Test
        @DisplayName("Should reject file too small to verify")
        void shouldRejectTooSmallFile() {
            // Given - File smaller than minimum required
            byte[] tinyContent = new byte[]{0x50, 0x4B};

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "tiny.pdf",
                    "application/pdf",
                    tinyContent
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("너무 작습니다");
        }
    }

    @Nested
    @DisplayName("Filename Sanitization Tests")
    class SanitizeFilenameTests {

        @Test
        @DisplayName("Should sanitize special characters")
        void shouldSanitizeSpecialCharacters() {
            // When
            String result = validator.sanitizeFilename("file<>:\"'/\\|?*name.txt");

            // Then
            assertThat(result).isEqualTo("file__________name.txt");
        }

        @Test
        @DisplayName("Should keep Korean characters")
        void shouldKeepKoreanCharacters() {
            // When
            String result = validator.sanitizeFilename("한글파일.txt");

            // Then
            assertThat(result).isEqualTo("한글파일.txt");
        }

        @Test
        @DisplayName("Should keep alphanumeric and allowed characters")
        void shouldKeepAlphanumericAndAllowed() {
            // When
            String result = validator.sanitizeFilename("normal_file-name.123.txt");

            // Then
            assertThat(result).isEqualTo("normal_file-name.123.txt");
        }

        @Test
        @DisplayName("Should return unnamed for null filename")
        void shouldReturnUnnamedForNull() {
            // When
            String result = validator.sanitizeFilename(null);

            // Then
            assertThat(result).isEqualTo("unnamed");
        }
    }

    @Nested
    @DisplayName("Empty and Invalid File Tests")
    class EmptyAndInvalidFileTests {

        @Test
        @DisplayName("Should reject null file")
        void shouldRejectNullFile() {
            // When & Then
            assertThatThrownBy(() -> validator.validateFile(null))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("비어있습니다");
        }

        @Test
        @DisplayName("Should reject empty file")
        void shouldRejectEmptyFile() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "empty.txt",
                    "text/plain",
                    new byte[0]
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("비어있습니다");
        }

        @Test
        @DisplayName("Should reject file with empty filename")
        void shouldRejectEmptyFilename() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "",
                    "text/plain",
                    "content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateFile(file))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("파일명이 유효하지 않습니다");
        }
    }
}
