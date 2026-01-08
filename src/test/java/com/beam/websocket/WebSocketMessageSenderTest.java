package com.beam.websocket;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.RoomType;
import com.beam.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketMessageSender Unit Tests")
class WebSocketMessageSenderTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ChatRoomManager roomManager;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private WebSocketMessageSender messageSender;

    @Nested
    @DisplayName("sendToSession Tests")
    class SendToSessionTests {

        @Test
        @DisplayName("Should send ChatMessage to open session")
        void shouldSendChatMessageToOpenSession() throws Exception {
            when(session.isOpen()).thenReturn(true);
            ChatMessage message = new ChatMessage("user", "content", "12:00:00", "chat");

            messageSender.sendToSession(session, message);

            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should send JSON string to open session")
        void shouldSendJsonStringToOpenSession() throws Exception {
            when(session.isOpen()).thenReturn(true);

            messageSender.sendToSession(session, "{\"test\":\"data\"}");

            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should not send to closed session")
        void shouldNotSendToClosedSession() throws Exception {
            when(session.isOpen()).thenReturn(false);
            ChatMessage message = new ChatMessage("user", "content", "12:00:00", "chat");

            messageSender.sendToSession(session, message);

            verify(session, never()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should handle null session gracefully")
        void shouldHandleNullSessionGracefully() throws Exception {
            ChatMessage message = new ChatMessage("user", "content", "12:00:00", "chat");
            assertDoesNotThrow(() -> messageSender.sendToSession(null, message));
        }
    }

    @Nested
    @DisplayName("broadcastToRoom Tests")
    class BroadcastToRoomTests {

        @Test
        @DisplayName("Should broadcast message to all users in room")
        void shouldBroadcastMessageToAllUsersInRoom() throws Exception {
            ChatRoom room = new ChatRoom("room1", "Test Room", RoomType.GROUP);
            User user1 = new User("1", "user1", "session1");
            User user2 = new User("2", "user2", "session2");
            room.addUser(user1);
            room.addUser(user2);

            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            when(roomManager.getRoom("room1")).thenReturn(room);
            when(sessionManager.findSessionById("session1")).thenReturn(session1);
            when(sessionManager.findSessionById("session2")).thenReturn(session2);

            ChatMessage message = new ChatMessage("user", "Hello", "12:00:00", "chat");
            messageSender.broadcastToRoom("room1", message);

            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should not broadcast when room does not exist")
        void shouldNotBroadcastWhenRoomDoesNotExist() throws Exception {
            when(roomManager.getRoom("nonexistent")).thenReturn(null);

            ChatMessage message = new ChatMessage("user", "Hello", "12:00:00", "chat");
            messageSender.broadcastToRoom("nonexistent", message);

            verify(sessionManager, never()).findSessionById(any());
        }

        @Test
        @DisplayName("Should skip closed sessions")
        void shouldSkipClosedSessions() throws Exception {
            ChatRoom room = new ChatRoom("room1", "Test Room", RoomType.GROUP);
            User user = new User("1", "user1", "session1");
            room.addUser(user);

            WebSocketSession closedSession = mock(WebSocketSession.class);
            when(closedSession.isOpen()).thenReturn(false);

            when(roomManager.getRoom("room1")).thenReturn(room);
            when(sessionManager.findSessionById("session1")).thenReturn(closedSession);

            ChatMessage message = new ChatMessage("user", "Hello", "12:00:00", "chat");
            messageSender.broadcastToRoom("room1", message);

            verify(closedSession, never()).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("broadcastRoomListToAll Tests")
    class BroadcastRoomListToAllTests {

        @Test
        @DisplayName("Should broadcast room list to all open sessions")
        void shouldBroadcastRoomListToAllOpenSessions() throws Exception {
            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            when(session1.isOpen()).thenReturn(true);
            when(session2.isOpen()).thenReturn(true);

            Set<WebSocketSession> sessions = new HashSet<>(Arrays.asList(session1, session2));
            when(sessionManager.getAllSessions()).thenReturn(sessions);

            ChatRoom room = new ChatRoom("general", "General", RoomType.GROUP);
            when(roomManager.getAllRooms()).thenReturn(Arrays.asList(room));

            messageSender.broadcastRoomListToAll();

            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should skip closed sessions when broadcasting")
        void shouldSkipClosedSessionsWhenBroadcasting() throws Exception {
            WebSocketSession openSession = mock(WebSocketSession.class);
            WebSocketSession closedSession = mock(WebSocketSession.class);
            when(openSession.isOpen()).thenReturn(true);
            when(closedSession.isOpen()).thenReturn(false);

            Set<WebSocketSession> sessions = new HashSet<>(Arrays.asList(openSession, closedSession));
            when(sessionManager.getAllSessions()).thenReturn(sessions);

            ChatRoom room = new ChatRoom("general", "General", RoomType.GROUP);
            when(roomManager.getAllRooms()).thenReturn(Arrays.asList(room));

            messageSender.broadcastRoomListToAll();

            verify(openSession).sendMessage(any(TextMessage.class));
            verify(closedSession, never()).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("sendRoomList Tests")
    class SendRoomListTests {

        @Test
        @DisplayName("Should send room list with all room details")
        void shouldSendRoomListWithAllRoomDetails() throws Exception {
            when(session.isOpen()).thenReturn(true);

            ChatRoom room = new ChatRoom("general", "General", RoomType.GROUP, "admin", "General chat");
            when(roomManager.getAllRooms()).thenReturn(Arrays.asList(room));

            messageSender.sendRoomList(session);

            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should send empty room list when no rooms")
        void shouldSendEmptyRoomListWhenNoRooms() throws Exception {
            when(session.isOpen()).thenReturn(true);
            when(roomManager.getAllRooms()).thenReturn(Arrays.asList());

            messageSender.sendRoomList(session);

            verify(session).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("sendRoomUserList Tests")
    class SendRoomUserListTests {

        @Test
        @DisplayName("Should send user list to room members")
        void shouldSendUserListToRoomMembers() throws Exception {
            ChatRoom room = new ChatRoom("room1", "Test Room", RoomType.GROUP);
            User user = new User("1", "user1", "session1");
            room.addUser(user);

            WebSocketSession userSession = mock(WebSocketSession.class);
            when(userSession.isOpen()).thenReturn(true);

            when(roomManager.getRoom("room1")).thenReturn(room);
            when(sessionManager.findSessionById("session1")).thenReturn(userSession);

            messageSender.sendRoomUserList("room1");

            verify(userSession).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should not send when room does not exist")
        void shouldNotSendWhenRoomDoesNotExist() throws Exception {
            when(roomManager.getRoom("nonexistent")).thenReturn(null);

            messageSender.sendRoomUserList("nonexistent");

            verify(sessionManager, never()).findSessionById(any());
        }
    }

    @Nested
    @DisplayName("sendSystemMessage Tests")
    class SendSystemMessageTests {

        @Test
        @DisplayName("Should send system message to room")
        void shouldSendSystemMessageToRoom() throws Exception {
            ChatRoom room = new ChatRoom("room1", "Test Room", RoomType.GROUP);
            User user = new User("1", "user1", "session1");
            room.addUser(user);

            WebSocketSession userSession = mock(WebSocketSession.class);
            when(userSession.isOpen()).thenReturn(true);

            when(roomManager.getRoom("room1")).thenReturn(room);
            when(sessionManager.findSessionById("session1")).thenReturn(userSession);

            messageSender.sendSystemMessage("room1", "User joined", "join");

            verify(userSession).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("sendErrorMessage Tests")
    class SendErrorMessageTests {

        @Test
        @DisplayName("Should send error message to session")
        void shouldSendErrorMessageToSession() throws Exception {
            when(session.isOpen()).thenReturn(true);

            messageSender.sendErrorMessage(session, "Something went wrong");

            verify(session).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("sendSuccessMessage Tests")
    class SendSuccessMessageTests {

        @Test
        @DisplayName("Should send success message to session")
        void shouldSendSuccessMessageToSession() throws Exception {
            when(session.isOpen()).thenReturn(true);

            messageSender.sendSuccessMessage(session, "Operation successful");

            verify(session).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should send success message with type and roomId")
        void shouldSendSuccessMessageWithTypeAndRoomId() throws Exception {
            when(session.isOpen()).thenReturn(true);

            messageSender.sendSuccessMessage(session, "Joined room", "join", "room1");

            verify(session).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("getCurrentTimestamp Tests")
    class GetCurrentTimestampTests {

        @Test
        @DisplayName("Should return formatted timestamp")
        void shouldReturnFormattedTimestamp() {
            String timestamp = messageSender.getCurrentTimestamp();

            assertNotNull(timestamp);
            assertTrue(timestamp.matches("\\d{2}:\\d{2}:\\d{2}"));
        }
    }
}
