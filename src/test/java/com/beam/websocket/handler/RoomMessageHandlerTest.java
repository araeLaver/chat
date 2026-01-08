package com.beam.websocket.handler;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.RoomType;
import com.beam.User;
import com.beam.websocket.ChatRoomManager;
import com.beam.websocket.WebSocketMessageSender;
import com.beam.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomMessageHandler Unit Tests")
class RoomMessageHandlerTest {

    @Mock
    private ChatRoomManager roomManager;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private WebSocketMessageSender messageSender;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private RoomMessageHandler handler;

    private static final String SESSION_ID = "test-session-id";
    private static final String ROOM_ID = "room-123";
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        lenient().when(session.getId()).thenReturn(SESSION_ID);
    }

    @Nested
    @DisplayName("handleJoinRoom Tests")
    class HandleJoinRoomTests {

        @Test
        @DisplayName("Should join specified room successfully")
        void shouldJoinSpecifiedRoom() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "joinRoom");
            message.setRoomId(ROOM_ID);

            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);
            ChatRoom mockRoom = createMockRoom(ROOM_ID, "Test Room");
            when(roomManager.getRoom(ROOM_ID)).thenReturn(mockRoom);

            // When
            handler.handleJoinRoom(session, message);

            // Then
            verify(sessionManager).addUser(eq(SESSION_ID), any(User.class));
            verify(roomManager).addUserToRoom(eq(ROOM_ID), any(User.class));
            verify(sessionManager).setSessionRoom(SESSION_ID, ROOM_ID);
        }

        @Test
        @DisplayName("Should join general room when roomId is null")
        void shouldJoinGeneralRoomWhenRoomIdIsNull() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "joinRoom");
            message.setRoomId(null);

            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);
            ChatRoom mockRoom = createMockRoom("general", "General");
            when(roomManager.getRoom("general")).thenReturn(mockRoom);

            // When
            handler.handleJoinRoom(session, message);

            // Then
            verify(roomManager).getRoom("general");
        }

        @Test
        @DisplayName("Should leave current room before joining new room")
        void shouldLeaveCurrentRoomBeforeJoining() throws Exception {
            // Given
            String currentRoomId = "old-room";
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "joinRoom");
            message.setRoomId(ROOM_ID);

            ChatRoom currentRoom = createMockRoomWithUser(currentRoomId, "Old Room", USERNAME, SESSION_ID);
            ChatRoom newRoom = createMockRoom(ROOM_ID, "New Room");

            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(currentRoomId, null);
            when(roomManager.getRoom(currentRoomId)).thenReturn(currentRoom);
            when(roomManager.getRoom(ROOM_ID)).thenReturn(newRoom);
            when(roomManager.removeUserFromRoom(eq(currentRoomId), eq(SESSION_ID))).thenReturn(new User(SESSION_ID, USERNAME, SESSION_ID));

            // When
            handler.handleJoinRoom(session, message);

            // Then
            verify(roomManager).removeUserFromRoom(currentRoomId, SESSION_ID);
            verify(sessionManager).removeSessionRoom(SESSION_ID);
        }
    }

    @Nested
    @DisplayName("handleCreateRoom Tests")
    class HandleCreateRoomTests {

        @Test
        @DisplayName("Should create room successfully")
        void shouldCreateRoomSuccessfully() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createRoom");
            message.setRoomName("New Room");
            message.setCreator(USERNAME);
            message.setDescription("Test Description");

            when(roomManager.isRoomNameDuplicate("New Room")).thenReturn(false);
            when(roomManager.generateGroupRoomId()).thenReturn("new-room-id");
            ChatRoom mockRoom = createMockRoom("new-room-id", "New Room");
            when(roomManager.getRoom("new-room-id")).thenReturn(mockRoom);

            // When
            handler.handleCreateRoom(session, message);

            // Then
            verify(roomManager).createRoom("new-room-id", "New Room", RoomType.GROUP, USERNAME, "Test Description");
            verify(messageSender).broadcastRoomListToAll();
            verify(messageSender).sendSuccessMessage(session, "방 'New Room'이 성공적으로 생성되었습니다!");
        }

        @Test
        @DisplayName("Should return error when room name is empty")
        void shouldReturnErrorWhenRoomNameIsEmpty() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createRoom");
            message.setRoomName("");

            // When
            handler.handleCreateRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "방 이름을 입력하세요.");
            verify(roomManager, never()).createRoom(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return error when room name is null")
        void shouldReturnErrorWhenRoomNameIsNull() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createRoom");
            message.setRoomName(null);

            // When
            handler.handleCreateRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "방 이름을 입력하세요.");
        }

        @Test
        @DisplayName("Should return error when room name is duplicate")
        void shouldReturnErrorWhenRoomNameIsDuplicate() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createRoom");
            message.setRoomName("Existing Room");

            when(roomManager.isRoomNameDuplicate("Existing Room")).thenReturn(true);

            // When
            handler.handleCreateRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "이미 존재하는 방 이름입니다. 다른 이름을 사용해주세요.");
            verify(roomManager, never()).createRoom(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("handleCreateDirectMessage Tests")
    class HandleCreateDirectMessageTests {

        @Test
        @DisplayName("Should create new DM room successfully")
        void shouldCreateNewDMRoomSuccessfully() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createDirectMessage");
            message.setUserId(1L);
            message.setFriendId(2L);
            message.setFriendName("Friend");

            String dmRoomId = "dm-1-2";
            when(roomManager.generateDirectMessageRoomId(1L, 2L)).thenReturn(dmRoomId);
            when(roomManager.getRoom(dmRoomId)).thenReturn(null);
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);

            ChatRoom newDmRoom = createMockRoom(dmRoomId, "DM: testuser ↔ Friend");
            when(roomManager.getRoom(dmRoomId)).thenReturn(null, newDmRoom);

            // When
            handler.handleCreateDirectMessage(session, message);

            // Then
            verify(roomManager).createRoom(eq(dmRoomId), eq("DM: testuser ↔ Friend"), eq(RoomType.DIRECT));
            verify(messageSender).broadcastRoomListToAll();
            verify(messageSender).sendSuccessMessage(eq(session), eq("1:1 채팅방에 입장했습니다."), eq("directMessageCreated"), eq(dmRoomId));
        }

        @Test
        @DisplayName("Should join existing DM room")
        void shouldJoinExistingDMRoom() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createDirectMessage");
            message.setUserId(1L);
            message.setFriendId(2L);
            message.setFriendName("Friend");

            String dmRoomId = "dm-1-2";
            ChatRoom existingRoom = createMockRoom(dmRoomId, "DM: testuser ↔ Friend");
            when(roomManager.generateDirectMessageRoomId(1L, 2L)).thenReturn(dmRoomId);
            when(roomManager.getRoom(dmRoomId)).thenReturn(existingRoom);
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);

            // When
            handler.handleCreateDirectMessage(session, message);

            // Then
            verify(roomManager, never()).createRoom(any(), any(), any());
            verify(messageSender).sendSuccessMessage(eq(session), eq("1:1 채팅방에 입장했습니다."), eq("directMessageCreated"), eq(dmRoomId));
        }

        @Test
        @DisplayName("Should return error when userId is null")
        void shouldReturnErrorWhenUserIdIsNull() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createDirectMessage");
            message.setUserId(null);
            message.setFriendId(2L);

            // When
            handler.handleCreateDirectMessage(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "사용자 ID가 필요합니다.");
        }

        @Test
        @DisplayName("Should return error when friendId is null")
        void shouldReturnErrorWhenFriendIdIsNull() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "createDirectMessage");
            message.setUserId(1L);
            message.setFriendId(null);

            // When
            handler.handleCreateDirectMessage(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "사용자 ID가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("handleDeleteRoom Tests")
    class HandleDeleteRoomTests {

        @Test
        @DisplayName("Should delete room successfully as creator")
        void shouldDeleteRoomSuccessfullyAsCreator() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "deleteRoom");
            message.setRoomId(ROOM_ID);

            ChatRoom room = createMockRoomWithCreator(ROOM_ID, "Test Room", USERNAME);
            when(roomManager.getRoom(ROOM_ID)).thenReturn(room);
            when(roomManager.isDefaultRoom(ROOM_ID)).thenReturn(false);

            // When
            handler.handleDeleteRoom(session, message);

            // Then
            verify(roomManager).deleteRoom(ROOM_ID);
            verify(messageSender).broadcastRoomListToAll();
            verify(messageSender).sendSuccessMessage(session, "방이 성공적으로 삭제되었습니다.");
        }

        @Test
        @DisplayName("Should return error when roomId is null")
        void shouldReturnErrorWhenRoomIdIsNull() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "deleteRoom");
            message.setRoomId(null);

            // When
            handler.handleDeleteRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "삭제할 방을 지정하세요.");
            verify(roomManager, never()).deleteRoom(any());
        }

        @Test
        @DisplayName("Should return error when room does not exist")
        void shouldReturnErrorWhenRoomDoesNotExist() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "deleteRoom");
            message.setRoomId(ROOM_ID);

            when(roomManager.getRoom(ROOM_ID)).thenReturn(null);

            // When
            handler.handleDeleteRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "존재하지 않는 방입니다.");
        }

        @Test
        @DisplayName("Should return error when trying to delete default room")
        void shouldReturnErrorWhenDeletingDefaultRoom() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "deleteRoom");
            message.setRoomId("general");

            ChatRoom room = createMockRoomWithCreator("general", "General", "admin");
            when(roomManager.getRoom("general")).thenReturn(room);
            when(roomManager.isDefaultRoom("general")).thenReturn(true);

            // When
            handler.handleDeleteRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "기본 방은 삭제할 수 없습니다.");
            verify(roomManager, never()).deleteRoom(any());
        }

        @Test
        @DisplayName("Should return error when non-creator tries to delete")
        void shouldReturnErrorWhenNonCreatorTriesToDelete() throws Exception {
            // Given
            ChatMessage message = new ChatMessage(USERNAME, "", "10:00:00", "deleteRoom");
            message.setRoomId(ROOM_ID);

            ChatRoom room = createMockRoomWithCreator(ROOM_ID, "Test Room", "otheruser");
            when(roomManager.getRoom(ROOM_ID)).thenReturn(room);
            when(roomManager.isDefaultRoom(ROOM_ID)).thenReturn(false);

            // When
            handler.handleDeleteRoom(session, message);

            // Then
            verify(messageSender).sendErrorMessage(session, "방장만 방을 삭제할 수 있습니다.");
            verify(roomManager, never()).deleteRoom(any());
        }
    }

    @Nested
    @DisplayName("joinRoom Tests")
    class JoinRoomTests {

        @Test
        @DisplayName("Should join room and send notifications")
        void shouldJoinRoomAndSendNotifications() throws Exception {
            // Given
            ChatRoom room = createMockRoom(ROOM_ID, "Test Room");
            when(roomManager.getRoom(ROOM_ID)).thenReturn(room);

            // When
            handler.joinRoom(session, ROOM_ID, USERNAME);

            // Then
            verify(sessionManager).addUser(eq(SESSION_ID), any(User.class));
            verify(roomManager).addUserToRoom(eq(ROOM_ID), any(User.class));
            verify(sessionManager).setSessionRoom(SESSION_ID, ROOM_ID);
            verify(messageSender).sendSystemMessage(eq(ROOM_ID), contains("입장하셨습니다"), eq("system"));
            verify(messageSender).sendRoomUserList(ROOM_ID);
        }

        @Test
        @DisplayName("Should not join when room does not exist")
        void shouldNotJoinWhenRoomDoesNotExist() throws Exception {
            // Given
            when(roomManager.getRoom(ROOM_ID)).thenReturn(null);

            // When
            handler.joinRoom(session, ROOM_ID, USERNAME);

            // Then
            verify(sessionManager, never()).addUser(any(), any());
            verify(roomManager, never()).addUserToRoom(any(), any());
        }
    }

    @Nested
    @DisplayName("leaveCurrentRoom Tests")
    class LeaveCurrentRoomTests {

        @Test
        @DisplayName("Should leave current room and send notifications")
        void shouldLeaveCurrentRoomAndSendNotifications() throws Exception {
            // Given
            ChatRoom room = createMockRoomWithUser(ROOM_ID, "Test Room", USERNAME, SESSION_ID);
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(ROOM_ID);
            when(roomManager.getRoom(ROOM_ID)).thenReturn(room);
            when(roomManager.removeUserFromRoom(ROOM_ID, SESSION_ID)).thenReturn(new User(SESSION_ID, USERNAME, SESSION_ID));

            // When
            handler.leaveCurrentRoom(session);

            // Then
            verify(roomManager).removeUserFromRoom(ROOM_ID, SESSION_ID);
            verify(sessionManager).removeSessionRoom(SESSION_ID);
            verify(messageSender).sendSystemMessage(eq(ROOM_ID), contains("퇴장하셨습니다"), eq("system"));
            verify(messageSender).sendRoomUserList(ROOM_ID);
        }

        @Test
        @DisplayName("Should do nothing when not in any room")
        void shouldDoNothingWhenNotInAnyRoom() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);

            // When
            handler.leaveCurrentRoom(session);

            // Then
            verify(roomManager, never()).removeUserFromRoom(any(), any());
            verify(sessionManager, never()).removeSessionRoom(any());
        }

        @Test
        @DisplayName("Should do nothing when room does not exist")
        void shouldDoNothingWhenRoomDoesNotExist() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(ROOM_ID);
            when(roomManager.getRoom(ROOM_ID)).thenReturn(null);

            // When
            handler.leaveCurrentRoom(session);

            // Then
            verify(roomManager, never()).removeUserFromRoom(any(), any());
        }
    }

    // Helper methods
    private ChatRoom createMockRoom(String roomId, String roomName) {
        ChatRoom room = mock(ChatRoom.class);
        lenient().when(room.getRoomId()).thenReturn(roomId);
        lenient().when(room.getRoomName()).thenReturn(roomName);
        lenient().when(room.getUsers()).thenReturn(new HashMap<>());
        return room;
    }

    private ChatRoom createMockRoomWithCreator(String roomId, String roomName, String creator) {
        ChatRoom room = createMockRoom(roomId, roomName);
        lenient().when(room.getCreator()).thenReturn(creator);
        return room;
    }

    private ChatRoom createMockRoomWithUser(String roomId, String roomName, String username, String sessionId) {
        ChatRoom room = createMockRoom(roomId, roomName);
        Map<String, User> users = new HashMap<>();
        users.put(sessionId, new User(sessionId, username, sessionId));
        lenient().when(room.getUsers()).thenReturn(users);
        return room;
    }
}
