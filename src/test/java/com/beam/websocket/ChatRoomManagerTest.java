package com.beam.websocket;

import com.beam.ChatRoom;
import com.beam.RoomType;
import com.beam.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatRoomManager Unit Tests")
class ChatRoomManagerTest {

    private ChatRoomManager chatRoomManager;

    @BeforeEach
    void setUp() {
        chatRoomManager = new ChatRoomManager();
        chatRoomManager.init();
    }

    @Nested
    @DisplayName("init Tests")
    class InitTests {

        @Test
        @DisplayName("Should initialize default rooms")
        void shouldInitializeDefaultRooms() {
            assertTrue(chatRoomManager.roomExists("general"));
            assertTrue(chatRoomManager.roomExists("tech"));
            assertTrue(chatRoomManager.roomExists("casual"));
        }

        @Test
        @DisplayName("Should have correct default room names")
        void shouldHaveCorrectDefaultRoomNames() {
            assertEquals("일반 채팅방", chatRoomManager.getRoom("general").getRoomName());
            assertEquals("개발 이야기", chatRoomManager.getRoom("tech").getRoomName());
            assertEquals("자유 토론", chatRoomManager.getRoom("casual").getRoomName());
        }

        @Test
        @DisplayName("Default rooms should be GROUP type")
        void defaultRoomsShouldBeGroupType() {
            assertEquals(RoomType.GROUP, chatRoomManager.getRoom("general").getRoomType());
            assertEquals(RoomType.GROUP, chatRoomManager.getRoom("tech").getRoomType());
            assertEquals(RoomType.GROUP, chatRoomManager.getRoom("casual").getRoomType());
        }
    }

    @Nested
    @DisplayName("getRoom Tests")
    class GetRoomTests {

        @Test
        @DisplayName("Should return room when exists")
        void shouldReturnRoomWhenExists() {
            ChatRoom room = chatRoomManager.getRoom("general");
            assertNotNull(room);
            assertEquals("general", room.getRoomId());
        }

        @Test
        @DisplayName("Should return null when room does not exist")
        void shouldReturnNullWhenRoomDoesNotExist() {
            ChatRoom room = chatRoomManager.getRoom("nonexistent");
            assertNull(room);
        }
    }

    @Nested
    @DisplayName("getAllRooms Tests")
    class GetAllRoomsTests {

        @Test
        @DisplayName("Should return all rooms")
        void shouldReturnAllRooms() {
            Collection<ChatRoom> rooms = chatRoomManager.getAllRooms();
            assertEquals(3, rooms.size());
        }

        @Test
        @DisplayName("Should include created rooms")
        void shouldIncludeCreatedRooms() {
            chatRoomManager.createRoom("test", "Test Room", RoomType.GROUP);
            Collection<ChatRoom> rooms = chatRoomManager.getAllRooms();
            assertEquals(4, rooms.size());
        }
    }

    @Nested
    @DisplayName("roomExists Tests")
    class RoomExistsTests {

        @Test
        @DisplayName("Should return true for existing room")
        void shouldReturnTrueForExistingRoom() {
            assertTrue(chatRoomManager.roomExists("general"));
        }

        @Test
        @DisplayName("Should return false for non-existing room")
        void shouldReturnFalseForNonExistingRoom() {
            assertFalse(chatRoomManager.roomExists("nonexistent"));
        }
    }

    @Nested
    @DisplayName("isRoomNameDuplicate Tests")
    class IsRoomNameDuplicateTests {

        @Test
        @DisplayName("Should return true for duplicate room name")
        void shouldReturnTrueForDuplicateRoomName() {
            assertTrue(chatRoomManager.isRoomNameDuplicate("일반 채팅방"));
        }

        @Test
        @DisplayName("Should return false for unique room name")
        void shouldReturnFalseForUniqueRoomName() {
            assertFalse(chatRoomManager.isRoomNameDuplicate("Unique Room Name"));
        }
    }

    @Nested
    @DisplayName("isDefaultRoom Tests")
    class IsDefaultRoomTests {

        @Test
        @DisplayName("Should return true for default rooms")
        void shouldReturnTrueForDefaultRooms() {
            assertTrue(chatRoomManager.isDefaultRoom("general"));
            assertTrue(chatRoomManager.isDefaultRoom("tech"));
            assertTrue(chatRoomManager.isDefaultRoom("casual"));
        }

        @Test
        @DisplayName("Should return false for non-default rooms")
        void shouldReturnFalseForNonDefaultRooms() {
            assertFalse(chatRoomManager.isDefaultRoom("custom"));
            assertFalse(chatRoomManager.isDefaultRoom("test"));
        }
    }

    @Nested
    @DisplayName("createRoom Tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should create room with basic parameters")
        void shouldCreateRoomWithBasicParameters() {
            ChatRoom room = chatRoomManager.createRoom("test", "Test Room", RoomType.GROUP);

            assertNotNull(room);
            assertEquals("test", room.getRoomId());
            assertEquals("Test Room", room.getRoomName());
            assertEquals(RoomType.GROUP, room.getRoomType());
            assertTrue(chatRoomManager.roomExists("test"));
        }

        @Test
        @DisplayName("Should create room with all parameters")
        void shouldCreateRoomWithAllParameters() {
            ChatRoom room = chatRoomManager.createRoom("test", "Test Room", RoomType.GROUP,
                "creator1", "Test Description");

            assertNotNull(room);
            assertEquals("test", room.getRoomId());
            assertEquals("Test Room", room.getRoomName());
            assertEquals(RoomType.GROUP, room.getRoomType());
            assertEquals("creator1", room.getCreator());
            assertEquals("Test Description", room.getDescription());
        }

        @Test
        @DisplayName("Should create DIRECT room type")
        void shouldCreateDirectRoomType() {
            ChatRoom room = chatRoomManager.createRoom("dm_1_2", "DM Room", RoomType.DIRECT);

            assertEquals(RoomType.DIRECT, room.getRoomType());
            assertTrue(room.isDirectMessage());
        }
    }

    @Nested
    @DisplayName("deleteRoom Tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should delete existing room")
        void shouldDeleteExistingRoom() {
            chatRoomManager.createRoom("test", "Test Room", RoomType.GROUP);
            assertTrue(chatRoomManager.roomExists("test"));

            chatRoomManager.deleteRoom("test");
            assertFalse(chatRoomManager.roomExists("test"));
        }

        @Test
        @DisplayName("Should handle deleting non-existing room gracefully")
        void shouldHandleDeletingNonExistingRoomGracefully() {
            assertDoesNotThrow(() -> chatRoomManager.deleteRoom("nonexistent"));
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should add user to room")
        void shouldAddUserToRoom() {
            User user = new User("1", "testuser", "session1");
            chatRoomManager.addUserToRoom("general", user);

            ChatRoom room = chatRoomManager.getRoom("general");
            assertEquals(1, room.getUserCount());
            assertNotNull(room.getUsers().get("session1"));
        }

        @Test
        @DisplayName("Should handle adding user to non-existing room")
        void shouldHandleAddingUserToNonExistingRoom() {
            User user = new User("1", "testuser", "session1");
            assertDoesNotThrow(() -> chatRoomManager.addUserToRoom("nonexistent", user));
        }

        @Test
        @DisplayName("Should remove user from room")
        void shouldRemoveUserFromRoom() {
            User user = new User("1", "testuser", "session1");
            chatRoomManager.addUserToRoom("general", user);

            User removed = chatRoomManager.removeUserFromRoom("general", "session1");

            assertNotNull(removed);
            assertEquals("testuser", removed.getUsername());
            assertEquals(0, chatRoomManager.getRoom("general").getUserCount());
        }

        @Test
        @DisplayName("Should return null when removing from non-existing room")
        void shouldReturnNullWhenRemovingFromNonExistingRoom() {
            User removed = chatRoomManager.removeUserFromRoom("nonexistent", "session1");
            assertNull(removed);
        }

        @Test
        @DisplayName("Should return null when removing non-existing user")
        void shouldReturnNullWhenRemovingNonExistingUser() {
            User removed = chatRoomManager.removeUserFromRoom("general", "nonexistent");
            assertNull(removed);
        }
    }

    @Nested
    @DisplayName("Room ID Generation Tests")
    class RoomIdGenerationTests {

        @Test
        @DisplayName("Should generate group room ID with prefix")
        void shouldGenerateGroupRoomIdWithPrefix() {
            String roomId = chatRoomManager.generateGroupRoomId();
            assertTrue(roomId.startsWith("group_"));
        }

        @Test
        @DisplayName("Should generate unique group room IDs")
        void shouldGenerateUniqueGroupRoomIds() throws InterruptedException {
            String roomId1 = chatRoomManager.generateGroupRoomId();
            Thread.sleep(1); // Ensure different timestamp
            String roomId2 = chatRoomManager.generateGroupRoomId();
            assertNotEquals(roomId1, roomId2);
        }

        @Test
        @DisplayName("Should generate DM room ID with sorted user IDs")
        void shouldGenerateDmRoomIdWithSortedUserIds() {
            String roomId1 = chatRoomManager.generateDirectMessageRoomId(1L, 2L);
            String roomId2 = chatRoomManager.generateDirectMessageRoomId(2L, 1L);

            assertEquals("dm_1_2", roomId1);
            assertEquals("dm_1_2", roomId2);
        }

        @Test
        @DisplayName("Should generate consistent DM room ID regardless of order")
        void shouldGenerateConsistentDmRoomIdRegardlessOfOrder() {
            String roomId1 = chatRoomManager.generateDirectMessageRoomId(100L, 50L);
            String roomId2 = chatRoomManager.generateDirectMessageRoomId(50L, 100L);

            assertEquals(roomId1, roomId2);
            assertEquals("dm_50_100", roomId1);
        }
    }
}
