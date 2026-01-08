package com.beam.websocket.handler;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.MessageEntity;
import com.beam.MessageSecurityType;
import com.beam.MessageService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageHandler Unit Tests")
class ChatMessageHandlerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private ChatRoomManager roomManager;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private WebSocketMessageSender messageSender;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private ChatMessageHandler handler;

    private static final String SESSION_ID = "test-session-id";
    private static final String ROOM_ID = "room-123";

    @BeforeEach
    void setUp() {
        lenient().when(session.getId()).thenReturn(SESSION_ID);
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @Nested
    @DisplayName("handleTextMessage Tests")
    class HandleTextMessageTests {

        @Test
        @DisplayName("Should send text message successfully when in a room")
        void shouldSendTextMessageSuccessfully() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(ROOM_ID);
            ChatRoom mockRoom = mock(ChatRoom.class);
            when(roomManager.getRoom(ROOM_ID)).thenReturn(mockRoom);

            ChatMessage chatMessage = new ChatMessage("testuser", "Hello!", "10:00:00", "message");

            // When
            handler.handleTextMessage(session, chatMessage);

            // Then
            verify(messageService).saveMessage(any(ChatMessage.class));
            verify(messageSender).broadcastToRoom(eq(ROOM_ID), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should not send message when not in a room")
        void shouldNotSendMessageWhenNotInRoom() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);

            ChatMessage chatMessage = new ChatMessage("testuser", "Hello!", "10:00:00", "message");

            // When
            handler.handleTextMessage(session, chatMessage);

            // Then
            verify(messageService, never()).saveMessage(any());
            verify(messageSender, never()).broadcastToRoom(anyString(), any());
        }

        @Test
        @DisplayName("Should not send message when room does not exist")
        void shouldNotSendMessageWhenRoomDoesNotExist() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(ROOM_ID);
            when(roomManager.getRoom(ROOM_ID)).thenReturn(null);

            ChatMessage chatMessage = new ChatMessage("testuser", "Hello!", "10:00:00", "message");

            // When
            handler.handleTextMessage(session, chatMessage);

            // Then
            verify(messageService, never()).saveMessage(any());
            verify(messageSender, never()).broadcastToRoom(anyString(), any());
        }
    }

    @Nested
    @DisplayName("handleFileMessage Tests")
    class HandleFileMessageTests {

        @Test
        @DisplayName("Should handle file message successfully")
        void shouldHandleFileMessageSuccessfully() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(ROOM_ID);

            ChatMessage chatMessage = new ChatMessage("testuser", "file.pdf", "10:00:00", "file");

            // When
            handler.handleFileMessage(session, chatMessage);

            // Then
            verify(messageService).saveMessage(any(ChatMessage.class));
            verify(messageSender).broadcastToRoom(eq(ROOM_ID), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should not handle file message when not in room")
        void shouldNotHandleFileMessageWhenNotInRoom() throws Exception {
            // Given
            when(sessionManager.getSessionRoom(SESSION_ID)).thenReturn(null);

            ChatMessage chatMessage = new ChatMessage("testuser", "file.pdf", "10:00:00", "file");

            // When
            handler.handleFileMessage(session, chatMessage);

            // Then
            verify(messageService, never()).saveMessage(any());
            verify(messageSender, never()).broadcastToRoom(anyString(), any());
        }
    }

    @Nested
    @DisplayName("handleGetHistory Tests")
    class HandleGetHistoryTests {

        @Test
        @DisplayName("Should get message history and mark as read")
        void shouldGetMessageHistoryAndMarkAsRead() throws Exception {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "", "10:00:00", "getHistory");
            chatMessage.setRoomId(ROOM_ID);
            chatMessage.setUserId(1L);

            List<MessageEntity> messages = new ArrayList<>();
            when(messageService.getRecentMessages(ROOM_ID)).thenReturn(messages);

            // When
            handler.handleGetHistory(session, chatMessage);

            // Then
            verify(messageService).getRecentMessages(ROOM_ID);
            verify(messageService).markRoomMessagesAsRead(eq(ROOM_ID), eq(1L), eq("testuser"));
        }

        @Test
        @DisplayName("Should not get history when roomId is null")
        void shouldNotGetHistoryWhenRoomIdIsNull() throws Exception {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "", "10:00:00", "getHistory");
            chatMessage.setRoomId(null);

            // When
            handler.handleGetHistory(session, chatMessage);

            // Then
            verify(messageService, never()).getRecentMessages(any());
        }

        @Test
        @DisplayName("Should not mark as read when userId is null")
        void shouldNotMarkAsReadWhenUserIdIsNull() throws Exception {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "", "10:00:00", "getHistory");
            chatMessage.setRoomId(ROOM_ID);
            chatMessage.setUserId(null);

            List<MessageEntity> messages = new ArrayList<>();
            when(messageService.getRecentMessages(ROOM_ID)).thenReturn(messages);

            // When
            handler.handleGetHistory(session, chatMessage);

            // Then
            verify(messageService).getRecentMessages(ROOM_ID);
            verify(messageService, never()).markRoomMessagesAsRead(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("handleMarkAsRead Tests")
    class HandleMarkAsReadTests {

        @Test
        @DisplayName("Should mark messages as read and broadcast update")
        void shouldMarkMessagesAsReadAndBroadcast() throws Exception {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "", "10:00:00", "markAsRead");
            chatMessage.setRoomId(ROOM_ID);
            chatMessage.setUserId(1L);

            when(messageSender.getCurrentTimestamp()).thenReturn("10:00:00");

            // When
            handler.handleMarkAsRead(session, chatMessage);

            // Then
            verify(messageService).markRoomMessagesAsRead(ROOM_ID, 1L, "testuser");
            verify(messageSender).broadcastToRoom(eq(ROOM_ID), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should not mark as read when roomId is null")
        void shouldNotMarkAsReadWhenRoomIdIsNull() throws Exception {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "", "10:00:00", "markAsRead");
            chatMessage.setRoomId(null);
            chatMessage.setUserId(1L);

            // When
            handler.handleMarkAsRead(session, chatMessage);

            // Then
            verify(messageService, never()).markRoomMessagesAsRead(any(), any(), any());
        }

        @Test
        @DisplayName("Should not mark as read when userId is null")
        void shouldNotMarkAsReadWhenUserIdIsNull() throws Exception {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "", "10:00:00", "markAsRead");
            chatMessage.setRoomId(ROOM_ID);
            chatMessage.setUserId(null);

            // When
            handler.handleMarkAsRead(session, chatMessage);

            // Then
            verify(messageService, never()).markRoomMessagesAsRead(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("sendMessageHistory Tests")
    class SendMessageHistoryTests {

        @Test
        @DisplayName("Should send message history to session")
        void shouldSendMessageHistoryToSession() throws Exception {
            // Given
            MessageEntity msg1 = createMessageEntity("user1", "Hello", "message");
            MessageEntity msg2 = createMessageEntity("user2", "Hi there", "message");

            List<MessageEntity> messages = List.of(msg1, msg2);
            when(messageService.getRecentMessages(ROOM_ID)).thenReturn(messages);

            // When
            handler.sendMessageHistory(session, ROOM_ID);

            // Then
            verify(messageService).getRecentMessages(ROOM_ID);
            verify(session, times(2)).sendMessage(any());
        }

        @Test
        @DisplayName("Should not send when session is closed")
        void shouldNotSendWhenSessionIsClosed() throws Exception {
            // Given
            when(session.isOpen()).thenReturn(false);

            MessageEntity msg = createMessageEntity("user1", "Hello", "message");

            when(messageService.getRecentMessages(ROOM_ID)).thenReturn(List.of(msg));

            // When
            handler.sendMessageHistory(session, ROOM_ID);

            // Then
            verify(session, never()).sendMessage(any());
        }

        @Test
        @DisplayName("Should handle empty message list")
        void shouldHandleEmptyMessageList() throws Exception {
            // Given
            when(messageService.getRecentMessages(ROOM_ID)).thenReturn(new ArrayList<>());

            // When
            handler.sendMessageHistory(session, ROOM_ID);

            // Then
            verify(messageService).getRecentMessages(ROOM_ID);
            verify(session, never()).sendMessage(any());
        }
    }

    // Helper method
    private MessageEntity createMessageEntity(String sender, String content, String messageType) {
        MessageEntity msg = new MessageEntity(sender, content, ROOM_ID, messageType);
        msg.setSecurityType(MessageSecurityType.NORMAL);
        return msg;
    }
}
