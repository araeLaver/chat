package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("RoomController Integration Tests")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitService rateLimitService;

    private UserEntity testUser;
    private UserEntity otherUser;
    private String token;
    private RoomEntity testRoom;

    @BeforeEach
    void setUp() {
        // Mock rate limit service
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        // Create test users
        testUser = UserEntity.builder()
                .username("roomtestuser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("roomtest@example.com")
                .phoneNumber("01099990000")
                .displayName("Room Test User")
                .isActive(true)
                .isOnline(true)
                .build();
        testUser = userRepository.save(testUser);

        otherUser = UserEntity.builder()
                .username("otheruser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("other@example.com")
                .phoneNumber("01088889999")
                .displayName("Other User")
                .isActive(true)
                .isOnline(false)
                .build();
        otherUser = userRepository.save(otherUser);

        token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());
    }

    private RoomEntity createTestRoom(String roomName, RoomEntity.RoomType roomType) {
        RoomEntity room = RoomEntity.builder()
                .roomName(roomName)
                .description("Test Description")
                .roomType(roomType)
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
    @DisplayName("Create Room Endpoint Tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should create room successfully")
        void shouldCreateRoomSuccessfully() throws Exception {
            String requestBody = """
                {
                    "roomName": "New Test Room",
                    "description": "Test Description",
                    "roomType": "PUBLIC",
                    "maxMembers": 100
                }
                """;

            mockMvc.perform(post("/api/v1/rooms")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.roomId").exists())
                    .andExpect(jsonPath("$.roomName").value("New Test Room"))
                    .andExpect(jsonPath("$.roomType").value("PUBLIC"));
        }

        @Test
        @DisplayName("Should create private room")
        void shouldCreatePrivateRoom() throws Exception {
            String requestBody = """
                {
                    "roomName": "Private Room",
                    "description": "Private Description",
                    "roomType": "PRIVATE",
                    "maxMembers": 50
                }
                """;

            mockMvc.perform(post("/api/v1/rooms")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.roomType").value("PRIVATE"));
        }
    }

    @Nested
    @DisplayName("Update Room Endpoint Tests")
    class UpdateRoomTests {

        @Test
        @DisplayName("Should update room successfully")
        void shouldUpdateRoomSuccessfully() throws Exception {
            testRoom = createTestRoom("Original Room", RoomEntity.RoomType.PUBLIC);

            String requestBody = """
                {
                    "roomName": "Updated Room Name",
                    "description": "Updated Description",
                    "maxMembers": 200
                }
                """;

            mockMvc.perform(put("/api/v1/rooms/" + testRoom.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.roomName").value("Updated Room Name"));
        }
    }

    @Nested
    @DisplayName("Delete Room Endpoint Tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should delete room successfully as owner")
        void shouldDeleteRoomSuccessfully() throws Exception {
            testRoom = createTestRoom("Room to Delete", RoomEntity.RoomType.PUBLIC);

            mockMvc.perform(delete("/api/v1/rooms/" + testRoom.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Room deleted successfully"));
        }

        @Test
        @DisplayName("Should return 400 when not owner tries to delete")
        void shouldReturn400WhenNotOwner() throws Exception {
            testRoom = createTestRoom("Room to Delete", RoomEntity.RoomType.PUBLIC);
            String otherToken = jwtUtil.generateToken(otherUser.getUsername(), otherUser.getId());

            mockMvc.perform(delete("/api/v1/rooms/" + testRoom.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("Member Management Endpoint Tests")
    class MemberManagementTests {

        @Test
        @DisplayName("Should add member successfully")
        void shouldAddMemberSuccessfully() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            String requestBody = "{\"userId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/v1/rooms/" + testRoom.getId() + "/members")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Member added successfully"));
        }

        @Test
        @DisplayName("Should remove member successfully")
        void shouldRemoveMemberSuccessfully() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            // Add other user as member first
            RoomMemberEntity member = RoomMemberEntity.builder()
                    .roomId(testRoom.getId())
                    .userId(otherUser.getId())
                    .role(RoomMemberEntity.MemberRole.MEMBER)
                    .isActive(true)
                    .unreadCount(0)
                    .build();
            roomMemberRepository.save(member);

            mockMvc.perform(delete("/api/v1/rooms/" + testRoom.getId() + "/members/" + otherUser.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should leave room successfully")
        void shouldLeaveRoomSuccessfully() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            // Add other user as member
            RoomMemberEntity member = RoomMemberEntity.builder()
                    .roomId(testRoom.getId())
                    .userId(otherUser.getId())
                    .role(RoomMemberEntity.MemberRole.MEMBER)
                    .isActive(true)
                    .unreadCount(0)
                    .build();
            roomMemberRepository.save(member);

            String otherToken = jwtUtil.generateToken(otherUser.getUsername(), otherUser.getId());

            mockMvc.perform(post("/api/v1/rooms/" + testRoom.getId() + "/leave")
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Left room successfully"));
        }
    }

    @Nested
    @DisplayName("Message Endpoint Tests")
    class MessageTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            String requestBody = """
                {
                    "content": "Hello, World!",
                    "messageType": "TEXT"
                }
                """;

            mockMvc.perform(post("/api/v1/rooms/" + testRoom.getId() + "/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.messageId").exists())
                    .andExpect(jsonPath("$.content").value("Hello, World!"));
        }

        @Test
        @DisplayName("Should get room messages")
        void shouldGetRoomMessages() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            mockMvc.perform(get("/api/v1/rooms/" + testRoom.getId() + "/messages")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should mark messages as read")
        void shouldMarkMessagesAsRead() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            mockMvc.perform(post("/api/v1/rooms/" + testRoom.getId() + "/read")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Room Query Endpoint Tests")
    class RoomQueryTests {

        @Test
        @DisplayName("Should get my rooms")
        void shouldGetMyRooms() throws Exception {
            testRoom = createTestRoom("My Test Room", RoomEntity.RoomType.PUBLIC);

            mockMvc.perform(get("/api/v1/rooms/my-rooms")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].roomId").value(testRoom.getId()))
                    .andExpect(jsonPath("$[0].roomName").value("My Test Room"))
                    .andExpect(jsonPath("$[0].myRole").value("OWNER"));
        }

        @Test
        @DisplayName("Should get room members")
        void shouldGetRoomMembers() throws Exception {
            testRoom = createTestRoom("Test Room", RoomEntity.RoomType.PUBLIC);

            mockMvc.perform(get("/api/v1/rooms/" + testRoom.getId() + "/members")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].userId").value(testUser.getId()))
                    .andExpect(jsonPath("$[0].role").value("OWNER"));
        }

        @Test
        @DisplayName("Should search rooms")
        void shouldSearchRooms() throws Exception {
            testRoom = createTestRoom("Searchable Room", RoomEntity.RoomType.PUBLIC);

            mockMvc.perform(get("/api/v1/rooms/search")
                            .header("Authorization", "Bearer " + token)
                            .param("keyword", "Searchable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
