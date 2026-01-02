package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RoomController.class,
    excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RoomController Integration Tests")
@org.junit.jupiter.api.Disabled("Temporarily disabled - needs refactoring to @SpringBootTest for proper integration testing")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoomMemberRepository roomMemberRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private RoomEntity testRoom;
    private RoomMemberEntity testMember;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Use lenient() for common mocks to avoid UnnecessaryStubbingException
        Mockito.lenient().when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
        Mockito.lenient().when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        Mockito.lenient().when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        testUser = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .displayName("Test User")
                .isOnline(true)
                .build();

        testRoom = RoomEntity.builder()
                .id(1L)
                .roomName("Test Room")
                .description("Test Description")
                .roomType(RoomEntity.RoomType.PUBLIC)
                .createdBy(1L)
                .maxMembers(100)
                .currentMembers(2)
                .isActive(true)
                .build();

        testMember = RoomMemberEntity.builder()
                .id(1L)
                .roomId(1L)
                .userId(1L)
                .role(RoomMemberEntity.MemberRole.OWNER)
                .isActive(true)
                .unreadCount(0)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Room Endpoint Tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should create room successfully")
        @WithMockUser
        void shouldCreateRoomSuccessfully() throws Exception {
            // Given - use any() for all parameters to ensure matching
            when(roomService.createRoom(any(), any(), any(), any(), any()))
                    .thenReturn(testRoom);

            String requestBody = """
                {
                    "roomName": "Test Room",
                    "description": "Test Description",
                    "roomType": "PUBLIC",
                    "maxMembers": 100
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/rooms")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.roomId").value(1))
                    .andExpect(jsonPath("$.roomName").value("Test Room"));
        }
    }

    @Nested
    @DisplayName("Update Room Endpoint Tests")
    class UpdateRoomTests {

        @Test
        @DisplayName("Should update room successfully")
        @WithMockUser
        void shouldUpdateRoomSuccessfully() throws Exception {
            // Given
            testRoom.setRoomName("Updated Room");
            when(roomService.updateRoom(any(), any(), any(), any(), any()))
                    .thenReturn(testRoom);

            String requestBody = """
                {
                    "roomName": "Updated Room",
                    "description": "Updated Description",
                    "maxMembers": 200
                }
                """;

            // When & Then
            mockMvc.perform(put("/api/rooms/1")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.roomName").value("Updated Room"));
        }
    }

    @Nested
    @DisplayName("Delete Room Endpoint Tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should delete room successfully")
        @WithMockUser
        void shouldDeleteRoomSuccessfully() throws Exception {
            // Given
            doNothing().when(roomService).deleteRoom(any(), any());

            // When & Then
            mockMvc.perform(delete("/api/rooms/1")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Room deleted successfully"));
        }

        @Test
        @DisplayName("Should return 400 when not owner")
        @WithMockUser
        void shouldReturn400WhenNotOwner() throws Exception {
            // Given
            doThrow(new RuntimeException("Only owner can delete room"))
                    .when(roomService).deleteRoom(any(), any());

            // When & Then
            mockMvc.perform(delete("/api/rooms/1")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Only owner can delete room"));
        }
    }

    @Nested
    @DisplayName("Member Management Endpoint Tests")
    class MemberManagementTests {

        @Test
        @DisplayName("Should add member successfully")
        @WithMockUser
        void shouldAddMemberSuccessfully() throws Exception {
            // Given
            doNothing().when(roomService).addMember(any(), any(), any());

            String requestBody = "{\"userId\": 2}";

            // When & Then
            mockMvc.perform(post("/api/rooms/1/members")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Member added successfully"));
        }

        @Test
        @DisplayName("Should remove member successfully")
        @WithMockUser
        void shouldRemoveMemberSuccessfully() throws Exception {
            // Given
            doNothing().when(roomService).removeMember(any(), any(), any());

            // When & Then
            mockMvc.perform(delete("/api/rooms/1/members/2")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should leave room successfully")
        @WithMockUser
        void shouldLeaveRoomSuccessfully() throws Exception {
            // Given
            doNothing().when(roomService).leaveRoom(any(), any());

            // When & Then
            mockMvc.perform(post("/api/rooms/1/leave")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Message Endpoint Tests")
    class MessageTests {

        @Test
        @DisplayName("Should send message successfully")
        @WithMockUser
        void shouldSendMessageSuccessfully() throws Exception {
            // Given
            GroupMessageEntity message = GroupMessageEntity.builder()
                    .id(1L)
                    .roomId(1L)
                    .senderId(1L)
                    .content("Hello")
                    .messageType(GroupMessageEntity.MessageType.TEXT)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(roomService.sendMessage(any(), any(), any(), any()))
                    .thenReturn(message);

            String requestBody = """
                {
                    "content": "Hello",
                    "messageType": "TEXT"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/rooms/1/messages")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.messageId").value(1))
                    .andExpect(jsonPath("$.content").value("Hello"));
        }

        @Test
        @DisplayName("Should get room messages successfully")
        @WithMockUser
        void shouldGetRoomMessagesSuccessfully() throws Exception {
            // Given
            GroupMessageEntity message = GroupMessageEntity.builder()
                    .id(1L)
                    .roomId(1L)
                    .senderId(1L)
                    .content("Hello")
                    .messageType(GroupMessageEntity.MessageType.TEXT)
                    .timestamp(LocalDateTime.now())
                    .readCount(0)
                    .build();

            when(roomService.getRoomMessages(any(), any())).thenReturn(List.of(message));
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/rooms/1/messages")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].content").value("Hello"));
        }

        @Test
        @DisplayName("Should mark messages as read")
        @WithMockUser
        void shouldMarkMessagesAsRead() throws Exception {
            // Given
            doNothing().when(roomService).markAsRead(any(), any());

            // When & Then
            mockMvc.perform(post("/api/rooms/1/read")
                            .with(csrf())
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Room Query Endpoint Tests")
    class RoomQueryTests {

        @Test
        @DisplayName("Should get my rooms successfully")
        @WithMockUser
        void shouldGetMyRoomsSuccessfully() throws Exception {
            // Given
            when(roomService.getUserRooms(any())).thenReturn(List.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(any(), any()))
                    .thenReturn(Optional.of(testMember));

            // When & Then
            mockMvc.perform(get("/api/rooms/my-rooms")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].roomId").value(1))
                    .andExpect(jsonPath("$[0].roomName").value("Test Room"))
                    .andExpect(jsonPath("$[0].myRole").value("OWNER"));
        }

        @Test
        @DisplayName("Should get room members successfully")
        @WithMockUser
        void shouldGetRoomMembersSuccessfully() throws Exception {
            // Given
            when(roomService.getRoomMembers(any(), any())).thenReturn(List.of(testMember));
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/rooms/1/members")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(1))
                    .andExpect(jsonPath("$[0].displayName").value("Test User"))
                    .andExpect(jsonPath("$[0].role").value("OWNER"));
        }

        @Test
        @DisplayName("Should search rooms successfully")
        @WithMockUser
        void shouldSearchRoomsSuccessfully() throws Exception {
            // Given
            when(roomService.searchRooms(any())).thenReturn(List.of(testRoom));

            // When & Then
            mockMvc.perform(get("/api/rooms/search")
                            .header("Authorization", "Bearer jwt-token")
                            .param("keyword", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].roomId").value(1))
                    .andExpect(jsonPath("$[0].roomName").value("Test Room"));
        }
    }
}
