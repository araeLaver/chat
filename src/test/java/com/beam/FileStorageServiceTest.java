package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Unit Tests")
class FileStorageServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileSecurityValidator fileSecurityValidator;

    @InjectMocks
    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    private FileMetadataEntity testMetadata;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "maxFileSize", 10485760L);

        testMetadata = FileMetadataEntity.builder()
                .id(1L)
                .fileName("test.txt")
                .filePath("uuid-test.txt")
                .fileType("text/plain")
                .fileSize(100L)
                .uploaderId(1L)
                .downloadCount(0)
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Store File Tests")
    class StoreFileTests {

        @Test
        @DisplayName("Should store file successfully")
        void shouldStoreFileSuccessfully() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "Hello, World!".getBytes()
            );

            doNothing().when(fileSecurityValidator).validateFile(any());
            when(fileSecurityValidator.sanitizeFilename("test.txt")).thenReturn("test.txt");
            when(fileMetadataRepository.save(any(FileMetadataEntity.class)))
                    .thenAnswer(inv -> {
                        FileMetadataEntity entity = inv.getArgument(0);
                        entity.setId(1L);
                        return entity;
                    });

            // When
            FileMetadataEntity result = fileStorageService.storeFile(file, 1L, "1_2", null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFileName()).isEqualTo("test.txt");
            assertThat(result.getUploaderId()).isEqualTo(1L);
            assertThat(result.getConversationId()).isEqualTo("1_2");
            verify(fileSecurityValidator).validateFile(file);
        }

        @Test
        @DisplayName("Should fail when file is empty")
        void shouldFailWhenFileIsEmpty() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "empty.txt",
                    "text/plain",
                    new byte[0]
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(file, 1L, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot store empty file");
        }

        @Test
        @DisplayName("Should fail when file exceeds max size")
        void shouldFailWhenFileExceedsMaxSize() {
            // Given
            ReflectionTestUtils.setField(fileStorageService, "maxFileSize", 10L);
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large.txt",
                    "text/plain",
                    "This is a large file content".getBytes()
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(file, 1L, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File size exceeds maximum limit");
        }

        @Test
        @DisplayName("Should fail when security validation fails")
        void shouldFailWhenSecurityValidationFails() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "malicious.exe",
                    "application/x-msdownload",
                    "MZ...".getBytes()
            );

            doThrow(new SecurityException("Blocked file type"))
                    .when(fileSecurityValidator).validateFile(any());

            // When & Then
            assertThatThrownBy(() -> fileStorageService.storeFile(file, 1L, null, null))
                    .isInstanceOf(SecurityException.class)
                    .hasMessage("Blocked file type");
        }

        @Test
        @DisplayName("Should store file with room ID")
        void shouldStoreFileWithRoomId() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "room_file.txt",
                    "text/plain",
                    "Room content".getBytes()
            );

            doNothing().when(fileSecurityValidator).validateFile(any());
            when(fileSecurityValidator.sanitizeFilename("room_file.txt")).thenReturn("room_file.txt");
            when(fileMetadataRepository.save(any(FileMetadataEntity.class)))
                    .thenAnswer(inv -> {
                        FileMetadataEntity entity = inv.getArgument(0);
                        entity.setId(1L);
                        return entity;
                    });

            // When
            FileMetadataEntity result = fileStorageService.storeFile(file, 1L, null, 100L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRoomId()).isEqualTo(100L);
            assertThat(result.getConversationId()).isNull();
        }
    }

    @Nested
    @DisplayName("Load File Tests")
    class LoadFileTests {

        @Test
        @DisplayName("Should load file successfully")
        void shouldLoadFileSuccessfully() throws IOException {
            // Given - Create actual file
            Path testFile = tempDir.resolve("uuid-test.txt");
            Files.writeString(testFile, "File content");

            testMetadata.setFilePath("uuid-test.txt");
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));
            when(fileMetadataRepository.save(any(FileMetadataEntity.class)))
                    .thenReturn(testMetadata);

            // When
            Resource result = fileStorageService.loadFileAsResource(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.exists()).isTrue();
            verify(fileMetadataRepository).save(argThat(meta ->
                    meta.getDownloadCount() > 0
            ));
        }

        @Test
        @DisplayName("Should fail when file not found in database")
        void shouldFailWhenFileNotFoundInDB() {
            // Given
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> fileStorageService.loadFileAsResource(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("File not found");
        }

        @Test
        @DisplayName("Should fail when physical file not found")
        void shouldFailWhenPhysicalFileNotFound() {
            // Given - Metadata exists but file doesn't
            testMetadata.setFilePath("non-existent.txt");
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When & Then
            assertThatThrownBy(() -> fileStorageService.loadFileAsResource(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("File not found");
        }
    }

    @Nested
    @DisplayName("Load Thumbnail Tests")
    class LoadThumbnailTests {

        @Test
        @DisplayName("Should load thumbnail successfully")
        void shouldLoadThumbnailSuccessfully() throws IOException {
            // Given
            Path thumbnailFile = tempDir.resolve("thumb_uuid-test.jpg");
            Files.writeString(thumbnailFile, "thumbnail content");

            testMetadata.setThumbnailPath("thumb_uuid-test.jpg");
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When
            Resource result = fileStorageService.loadThumbnailAsResource(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.exists()).isTrue();
        }

        @Test
        @DisplayName("Should fail when thumbnail not available")
        void shouldFailWhenThumbnailNotAvailable() {
            // Given
            testMetadata.setThumbnailPath(null);
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When & Then - The exception is caught and re-thrown as "Thumbnail not found"
            assertThatThrownBy(() -> fileStorageService.loadThumbnailAsResource(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Thumbnail not found");
        }
    }

    @Nested
    @DisplayName("Delete File Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() throws IOException {
            // Given
            Path testFile = tempDir.resolve("uuid-test.txt");
            Files.writeString(testFile, "To be deleted");

            testMetadata.setFilePath("uuid-test.txt");
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When
            fileStorageService.deleteFile(1L, 1L);

            // Then
            verify(fileMetadataRepository).save(argThat(meta ->
                    meta.getIsDeleted() && meta.getDeletedAt() != null
            ));
        }

        @Test
        @DisplayName("Should fail when user has no permission")
        void shouldFailWhenNoPermission() {
            // Given
            testMetadata.setUploaderId(1L);
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When & Then
            assertThatThrownBy(() -> fileStorageService.deleteFile(1L, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No permission to delete this file");
        }

        @Test
        @DisplayName("Should delete thumbnail along with file")
        void shouldDeleteThumbnailWithFile() throws IOException {
            // Given
            Path testFile = tempDir.resolve("uuid-test.jpg");
            Path thumbnailFile = tempDir.resolve("thumb_uuid-test.jpg");
            Files.writeString(testFile, "Image content");
            Files.writeString(thumbnailFile, "Thumbnail content");

            testMetadata.setFilePath("uuid-test.jpg");
            testMetadata.setThumbnailPath("thumb_uuid-test.jpg");
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When
            fileStorageService.deleteFile(1L, 1L);

            // Then
            assertThat(Files.exists(testFile)).isFalse();
            assertThat(Files.exists(thumbnailFile)).isFalse();
        }

        @Test
        @DisplayName("Should mark as deleted even if physical file missing")
        void shouldMarkDeletedEvenIfFileMissing() {
            // Given - No physical file exists
            testMetadata.setFilePath("non-existent.txt");
            when(fileMetadataRepository.findByIdAndIsDeletedFalse(1L))
                    .thenReturn(Optional.of(testMetadata));

            // When
            fileStorageService.deleteFile(1L, 1L);

            // Then - Should still mark metadata as deleted
            verify(fileMetadataRepository).save(argThat(meta ->
                    meta.getIsDeleted()
            ));
        }
    }

    @Nested
    @DisplayName("Path Traversal Prevention Tests")
    class PathTraversalPreventionTests {

        @Test
        @DisplayName("Should prevent path traversal in store file")
        void shouldPreventPathTraversalInStore() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    "content".getBytes()
            );

            doNothing().when(fileSecurityValidator).validateFile(any());
            when(fileSecurityValidator.sanitizeFilename(anyString()))
                    .thenReturn("sanitized_name.txt");
            when(fileMetadataRepository.save(any(FileMetadataEntity.class)))
                    .thenAnswer(inv -> {
                        FileMetadataEntity entity = inv.getArgument(0);
                        entity.setId(1L);
                        return entity;
                    });

            // When
            FileMetadataEntity result = fileStorageService.storeFile(file, 1L, null, null);

            // Then - File should be stored with sanitized name
            assertThat(result).isNotNull();
            verify(fileSecurityValidator).sanitizeFilename(anyString());
        }
    }
}
