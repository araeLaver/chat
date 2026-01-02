package com.beam;

import com.beam.websocket.ChatRoomManager;
import com.beam.websocket.WebSocketMessageSender;
import com.beam.websocket.WebSocketSessionManager;
import com.beam.websocket.handler.ChatMessageHandler;
import com.beam.websocket.handler.RoomMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketHandler Unit Tests")
class ChatWebSocketHandlerTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ChatRoomManager roomManager;

    @Mock
    private WebSocketMessageSender messageSender;

    @Mock
    private RoomMessageHandler roomHandler;

    @Mock
    private ChatMessageHandler chatHandler;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private ChatWebSocketHandler handler;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Object> sessionAttributes;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        sessionAttributes = new HashMap<>();
        headers = new HttpHeaders();

        lenient().when(session.getId()).thenReturn("test-session-id");
        lenient().when(session.getAttributes()).thenReturn(sessionAttributes);
        lenient().when(session.getHandshakeHeaders()).thenReturn(headers);
        lenient().when(session.isOpen()).thenReturn(true);
    }

    @Nested
    @DisplayName("Connection Establishment Tests")
    class ConnectionEstablishmentTests {

        @Test
        @DisplayName("Should establish connection with valid JWT token")
        void shouldEstablishConnectionWithValidToken() throws Exception {
            // Given
            headers.put("Authorization", List.of("Bearer valid-jwt-token"));
            when(jwtUtil.validateToken("valid-jwt-token")).thenReturn(true);
            when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("testuser");
            when(jwtUtil.getUserIdFromToken("valid-jwt-token")).thenReturn(1L);

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(sessionManager).addSession(session);
            verify(messageSender).sendRoomList(session);
            verify(session, never()).close(any(CloseStatus.class));
        }

        @Test
        @DisplayName("Should reject connection with invalid JWT token")
        void shouldRejectConnectionWithInvalidToken() throws Exception {
            // Given
            headers.put("Authorization", List.of("Bearer invalid-token"));
            when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(session).close(argThat(status ->
                status.getCode() == CloseStatus.POLICY_VIOLATION.getCode()
            ));
            verify(sessionManager, never()).addSession(session);
        }

        @Test
        @DisplayName("Should allow guest connection without token")
        void shouldAllowGuestConnectionWithoutToken() throws Exception {
            // Given - No Authorization header

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(sessionManager).addSession(session);
            verify(messageSender).sendRoomList(session);
        }

        @Test
        @DisplayName("Should allow guest mode with 'guest' token")
        void shouldAllowGuestModeWithGuestToken() throws Exception {
            // Given
            headers.put("Authorization", List.of("Bearer guest"));

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(sessionManager).addSession(session);
            verify(messageSender).sendRoomList(session);
            verify(jwtUtil, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("Should extract token from Sec-WebSocket-Protocol header")
        void shouldExtractTokenFromWebSocketProtocol() throws Exception {
            // Given
            headers.put("Sec-WebSocket-Protocol", List.of("access_token, jwt-token-value"));
            when(jwtUtil.validateToken("jwt-token-value")).thenReturn(true);
            when(jwtUtil.getUsernameFromToken("jwt-token-value")).thenReturn("testuser");
            when(jwtUtil.getUserIdFromToken("jwt-token-value")).thenReturn(1L);

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(sessionManager).addSession(session);
        }
    }

    @Nested
    @DisplayName("Message Handling Tests")
    class MessageHandlingTests {

        @BeforeEach
        void setUpRateLimit() {
            when(rateLimitService.isWebSocketMessageAllowed(anyString())).thenReturn(true);
        }

        @Test
        @DisplayName("Should route joinRoom message to room handler")
        void shouldRouteJoinRoomMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("joinRoom");
            message.setRoomId("room-1");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(roomHandler).handleJoinRoom(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route createRoom message to room handler")
        void shouldRouteCreateRoomMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("createRoom");
            message.setRoomName("New Room");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(roomHandler).handleCreateRoom(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route createDirectMessage to room handler")
        void shouldRouteCreateDirectMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("createDirectMessage");
            message.setFriendId(2L);
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(roomHandler).handleCreateDirectMessage(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route deleteRoom message to room handler")
        void shouldRouteDeleteRoomMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("deleteRoom");
            message.setRoomId("room-1");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(roomHandler).handleDeleteRoom(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route text message to chat handler")
        void shouldRouteTextMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("message");
            message.setContent("Hello World");
            message.setSender("testuser");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleTextMessage(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route file message to chat handler")
        void shouldRouteFileMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("file");
            message.setContent("file-url");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleFileMessage(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route getHistory message to chat handler")
        void shouldRouteGetHistoryMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("getHistory");
            message.setRoomId("room-1");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleGetHistory(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should route markAsRead message to chat handler")
        void shouldRouteMarkAsReadMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("markAsRead");
            message.setRoomId("room-1");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleMarkAsRead(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should handle default message type as text message")
        void shouldHandleDefaultAsTextMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setType("unknownType");
            message.setContent("Some content");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleTextMessage(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should handle null message type as text message")
        void shouldHandleNullTypeAsTextMessage() throws Exception {
            // Given
            ChatMessage message = new ChatMessage();
            message.setContent("Message without type");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleTextMessage(eq(session), any(ChatMessage.class));
        }
    }

    @Nested
    @DisplayName("Rate Limiting Tests")
    class RateLimitingTests {

        @Test
        @DisplayName("Should allow message when rate limit not exceeded")
        void shouldAllowMessageWhenNotRateLimited() throws Exception {
            // Given
            when(rateLimitService.isWebSocketMessageAllowed("test-session-id")).thenReturn(true);
            ChatMessage message = new ChatMessage();
            message.setType("message");
            message.setContent("Hello");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(chatHandler).handleTextMessage(eq(session), any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should send error when rate limit exceeded")
        void shouldSendErrorWhenRateLimited() throws Exception {
            // Given
            when(rateLimitService.isWebSocketMessageAllowed("test-session-id")).thenReturn(false);
            ChatMessage message = new ChatMessage();
            message.setType("message");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));

            // When
            handler.handleTextMessage(session, textMessage);

            // Then
            verify(session).sendMessage(argThat(msg -> {
                String payload = ((TextMessage) msg).getPayload();
                return payload.contains("Rate limit exceeded");
            }));
            verify(chatHandler, never()).handleTextMessage(any(), any());
        }
    }

    @Nested
    @DisplayName("Connection Close Tests")
    class ConnectionCloseTests {

        @Test
        @DisplayName("Should clean up on connection close")
        void shouldCleanUpOnConnectionClose() throws Exception {
            // When
            handler.afterConnectionClosed(session, CloseStatus.NORMAL);

            // Then
            verify(roomHandler).leaveCurrentRoom(session);
            verify(sessionManager).removeSession(session);
            verify(rateLimitService).removeWebSocketLimiter("test-session-id");
        }

        @Test
        @DisplayName("Should clean up on abnormal close")
        void shouldCleanUpOnAbnormalClose() throws Exception {
            // When
            handler.afterConnectionClosed(session, CloseStatus.GOING_AWAY);

            // Then
            verify(roomHandler).leaveCurrentRoom(session);
            verify(sessionManager).removeSession(session);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() throws Exception {
            // Given
            when(rateLimitService.isWebSocketMessageAllowed(anyString())).thenReturn(true);
            TextMessage malformedMessage = new TextMessage("not valid json");

            // When - Should not throw exception
            handler.handleTextMessage(session, malformedMessage);

            // Then - No handlers should be called
            verify(chatHandler, never()).handleTextMessage(any(), any());
            verify(roomHandler, never()).handleJoinRoom(any(), any());
        }

        @Test
        @DisplayName("Should handle empty message payload")
        void shouldHandleEmptyPayload() throws Exception {
            // Given
            when(rateLimitService.isWebSocketMessageAllowed(anyString())).thenReturn(true);
            TextMessage emptyMessage = new TextMessage("");

            // When - Should not throw exception
            handler.handleTextMessage(session, emptyMessage);

            // Then
            verify(chatHandler, never()).handleTextMessage(any(), any());
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract token from Bearer format")
        void shouldExtractTokenFromBearerFormat() throws Exception {
            // Given
            headers.put("Authorization", List.of("Bearer my-jwt-token"));
            when(jwtUtil.validateToken("my-jwt-token")).thenReturn(true);
            when(jwtUtil.getUsernameFromToken("my-jwt-token")).thenReturn("user");
            when(jwtUtil.getUserIdFromToken("my-jwt-token")).thenReturn(1L);

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(jwtUtil).validateToken("my-jwt-token");
        }

        @Test
        @DisplayName("Should extract token without Bearer prefix")
        void shouldExtractTokenWithoutBearerPrefix() throws Exception {
            // Given
            headers.put("Authorization", List.of("raw-token"));
            when(jwtUtil.validateToken("raw-token")).thenReturn(true);
            when(jwtUtil.getUsernameFromToken("raw-token")).thenReturn("user");
            when(jwtUtil.getUserIdFromToken("raw-token")).thenReturn(1L);

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(jwtUtil).validateToken("raw-token");
        }

        @Test
        @DisplayName("Should extract token from session attributes")
        void shouldExtractTokenFromSessionAttributes() throws Exception {
            // Given
            sessionAttributes.put("token", "session-token");
            when(jwtUtil.validateToken("session-token")).thenReturn(true);
            when(jwtUtil.getUsernameFromToken("session-token")).thenReturn("user");
            when(jwtUtil.getUserIdFromToken("session-token")).thenReturn(1L);

            // When
            handler.afterConnectionEstablished(session);

            // Then
            verify(jwtUtil).validateToken("session-token");
        }
    }
}
