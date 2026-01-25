package com.beam;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("FileController Integration Tests")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private FileStorageService fileStorageService;

    private UserEntity testUser;
    private UserEntity otherUser;
    private String token;

    @BeforeEach
    void setUp() {
        // Mock rate limit service
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        // Create test users
        testUser = UserEntity.builder()
                .username("filetestuser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("filetest@example.com")
                .phoneNumber("01055556666")
                .displayName("File Test User")
                .isActive(true)
                .isOnline(true)
                .build();
        testUser = userRepository.save(testUser);

        otherUser = UserEntity.builder()
                .username("fileotheruser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("fileother@example.com")
                .phoneNumber("01077778888")
                .displayName("File Other User")
                .isActive(true)
                .isOnline(false)
                .build();
        otherUser = userRepository.save(otherUser);

        token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());
    }

    private ConversationEntity createConversation() {
        String conversationId = testUser.getId() < otherUser.getId()
                ? testUser.getId() + "_" + otherUser.getId()
                : otherUser.getId() + "_" + testUser.getId();

        ConversationEntity conversation = ConversationEntity.builder()
                .conversationId(conversationId)
                .user1Id(testUser.getId())
                .user2Id(otherUser.getId())
                .build();
        return conversationRepository.save(conversation);
    }

    private RoomEntity createTestRoom() {
        RoomEntity room = RoomEntity.builder()
                .roomName("Test Room")
                .description("Test Description")
                .roomType(RoomEntity.RoomType.PUBLIC)
                .createdBy(testUser.getId())
                .maxMembers(100)
                .currentMembers(1)
                .isActive(true)
                .build();
        room = roomRepository.save(room);

        RoomMemberEntity member = RoomMemberEntity.builder()
                .roomId(room.getId())
                .userId(testUser.getId())
                .role(RoomMemberEntity.MemberRole.OWNER)
                .isActive(true)
                .unreadCount(0)
                .build();
        roomMemberRepository.save(member);

        return room;
    }

    @Nested
    @DisplayName("Upload File to DM Endpoint Tests")
    class UploadFileToDMTests {

        @Test
        @DisplayName("Should upload file to DM successfully")
        void shouldUploadFileToDMSuccessfully() throws Exception {
            ConversationEntity conversation = createConversation();

            FileMetadataEntity mockMetadata = FileMetadataEntity.builder()
                    .id(1L)
                    .fileName("test.png")
                    .fileSize(1024L)
                    .fileType("image/png")
                    .category(FileMetadataEntity.FileCategory.IMAGE)
                    .uploaderId(testUser.getId())
                    .conversationId(conversation.getConversationId())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(fileStorageService.storeFile(any(), any(), any(), any())).thenReturn(mockMetadata);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "test image content".getBytes()
            );

            mockMvc.perform(multipart("/api/v1/files/upload/dm")
                            .file(file)
                            .param("conversationId", conversation.getConversationId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.fileId").value(1))
                    .andExpect(jsonPath("$.fileName").value("test.png"))
                    .andExpect(jsonPath("$.category").value("IMAGE"));
        }

        @Test
        @DisplayName("Should return error for invalid file")
        void shouldReturnErrorForInvalidFile() throws Exception {
            ConversationEntity conversation = createConversation();

            when(fileStorageService.storeFile(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Invalid file type"));

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.exe",
                    "application/octet-stream",
                    "malicious content".getBytes()
            );

            mockMvc.perform(multipart("/api/v1/files/upload/dm")
                            .file(file)
                            .param("conversationId", conversation.getConversationId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid file type"));
        }
    }

    @Nested
    @DisplayName("Upload File to Room Endpoint Tests")
    class UploadFileToRoomTests {

        @Test
        @DisplayName("Should upload file to room successfully")
        void shouldUploadFileToRoomSuccessfully() throws Exception {
            RoomEntity room = createTestRoom();

            FileMetadataEntity mockMetadata = FileMetadataEntity.builder()
                    .id(1L)
                    .fileName("document.pdf")
                    .fileSize(2048L)
                    .fileType("application/pdf")
                    .category(FileMetadataEntity.FileCategory.DOCUMENT)
                    .uploaderId(testUser.getId())
                    .roomId(room.getId())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(fileStorageService.storeFile(any(), any(), any(), any())).thenReturn(mockMetadata);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "pdf content".getBytes()
            );

            mockMvc.perform(multipart("/api/v1/files/upload/room")
                            .file(file)
                            .param("roomId", room.getId().toString())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.fileId").value(1))
                    .andExpect(jsonPath("$.fileName").value("document.pdf"))
                    .andExpect(jsonPath("$.category").value("DOCUMENT"));
        }
    }

    @Nested
    @DisplayName("Get Conversation Files Endpoint Tests")
    class GetConversationFilesTests {

        @Test
        @DisplayName("Should get conversation files successfully")
        void shouldGetConversationFilesSuccessfully() throws Exception {
            ConversationEntity conversation = createConversation();

            // Create file metadata
            FileMetadataEntity fileMetadata = FileMetadataEntity.builder()
                    .fileName("test.jpg")
                    .filePath("/uploads/test.jpg")
                    .fileSize(1024L)
                    .fileType("image/jpeg")
                    .category(FileMetadataEntity.FileCategory.IMAGE)
                    .uploaderId(testUser.getId())
                    .conversationId(conversation.getConversationId())
                    .uploadedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            fileMetadataRepository.save(fileMetadata);

            mockMvc.perform(get("/api/v1/files/conversation/" + conversation.getConversationId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].fileName").value("test.jpg"))
                    .andExpect(jsonPath("$[0].category").value("IMAGE"));
        }

        @Test
        @DisplayName("Should return empty list when no files")
        void shouldReturnEmptyListWhenNoFiles() throws Exception {
            ConversationEntity conversation = createConversation();

            mockMvc.perform(get("/api/v1/files/conversation/" + conversation.getConversationId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Room Files Endpoint Tests")
    class GetRoomFilesTests {

        @Test
        @DisplayName("Should get room files successfully")
        void shouldGetRoomFilesSuccessfully() throws Exception {
            RoomEntity room = createTestRoom();

            // Create file metadata
            FileMetadataEntity fileMetadata = FileMetadataEntity.builder()
                    .fileName("room-file.png")
                    .filePath("/uploads/room-file.png")
                    .fileSize(2048L)
                    .fileType("image/png")
                    .category(FileMetadataEntity.FileCategory.IMAGE)
                    .uploaderId(testUser.getId())
                    .roomId(room.getId())
                    .uploadedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            fileMetadataRepository.save(fileMetadata);

            mockMvc.perform(get("/api/v1/files/room/" + room.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].fileName").value("room-file.png"));
        }
    }

    @Nested
    @DisplayName("Delete File Endpoint Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() throws Exception {
            // Create file metadata
            FileMetadataEntity fileMetadata = FileMetadataEntity.builder()
                    .fileName("to-delete.jpg")
                    .filePath("/uploads/to-delete.jpg")
                    .fileSize(1024L)
                    .fileType("image/jpeg")
                    .category(FileMetadataEntity.FileCategory.IMAGE)
                    .uploaderId(testUser.getId())
                    .uploadedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            fileMetadata = fileMetadataRepository.save(fileMetadata);

            doNothing().when(fileStorageService).deleteFile(anyLong(), anyLong());

            mockMvc.perform(delete("/api/v1/files/" + fileMetadata.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("File deleted successfully"));
        }

        @Test
        @DisplayName("Should return error when deleting other users file")
        void shouldReturnErrorWhenDeletingOtherUsersFile() throws Exception {
            // Create file metadata owned by other user
            FileMetadataEntity fileMetadata = FileMetadataEntity.builder()
                    .fileName("other-user-file.jpg")
                    .filePath("/uploads/other-user-file.jpg")
                    .fileSize(1024L)
                    .fileType("image/jpeg")
                    .category(FileMetadataEntity.FileCategory.IMAGE)
                    .uploaderId(otherUser.getId())
                    .uploadedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();
            fileMetadata = fileMetadataRepository.save(fileMetadata);

            // Mock the service to throw exception when user tries to delete another user's file
            doThrow(new RuntimeException("No permission to delete this file"))
                    .when(fileStorageService).deleteFile(eq(fileMetadata.getId()), eq(testUser.getId()));

            mockMvc.perform(delete("/api/v1/files/" + fileMetadata.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}
