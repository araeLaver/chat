package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatWebSocketController Tests")
class ChatWebSocketControllerTest {

    private ChatWebSocketController chatWebSocketController;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private DirectMessageService directMessageService;

    @Mock
    private RoomService roomService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Mock
    private Principal principal;

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long ROOM_ID = 100L;
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @BeforeEach
    void setUp() {
        chatWebSocketController = new ChatWebSocketController();
        ReflectionTestUtils.setField(chatWebSocketController, "messagingTemplate", messagingTemplate);
        ReflectionTestUtils.setField(chatWebSocketController, "directMessageService", directMessageService);
        ReflectionTestUtils.setField(chatWebSocketController, "roomService", roomService);
        ReflectionTestUtils.setField(chatWebSocketController, "jwtUtil", jwtUtil);

        when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(SENDER_ID);
        when(headerAccessor.getUser()).thenReturn(principal);
        when(principal.getName()).thenReturn(SENDER_ID.toString());
    }

    @Nested
    @DisplayName("sendDirectMessage Tests")
    class SendDirectMessageTests {

        @Test
        @DisplayName("Should send direct message successfully")
        void shouldSendDirectMessageSuccessfully() {
            DirectMessageEntity savedMessage = DirectMessageEntity.builder()
                    .senderId(SENDER_ID)
                    .receiverId(RECEIVER_ID)
                    .content("Hello!")
                    .timestamp(LocalDateTime.now())
                    .build();
            savedMessage.setId(1L);
            savedMessage.setConversationId("conv-1");

            when(directMessageService.sendMessage(SENDER_ID, RECEIVER_ID, "Hello!"))
                    .thenReturn(savedMessage);

            Map<String, Object> message = new HashMap<>();
            message.put("token", BEARER_TOKEN);
            message.put("receiverId", RECEIVER_ID.toString());
            message.put("content", "Hello!");

            chatWebSocketController.sendDirectMessage(message, headerAccessor);

            verify(directMessageService).sendMessage(SENDER_ID, RECEIVER_ID, "Hello!");
            verify(messagingTemplate, times(2)).convertAndSendToUser(
                    anyString(), eq("/queue/messages"), any(Map.class));
        }

        @Test
        @DisplayName("Should send to both sender and receiver")
        void shouldSendToBothSenderAndReceiver() {
            DirectMessageEntity savedMessage = DirectMessageEntity.builder()
                    .senderId(SENDER_ID)
                    .receiverId(RECEIVER_ID)
                    .content("Hello!")
                    .timestamp(LocalDateTime.now())
                    .build();
            savedMessage.setId(1L);
            savedMessage.setConversationId("conv-1");

            when(directMessageService.sendMessage(SENDER_ID, RECEIVER_ID, "Hello!"))
                    .thenReturn(savedMessage);

            Map<String, Object> message = new HashMap<>();
            message.put("token", VALID_TOKEN); // Without Bearer prefix
            message.put("receiverId", RECEIVER_ID.toString());
            message.put("content", "Hello!");

            chatWebSocketController.sendDirectMessage(message, headerAccessor);

            ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
            verify(messagingTemplate, times(2)).convertAndSendToUser(
                    userCaptor.capture(), eq("/queue/messages"), any(Map.class));

            assertThat(userCaptor.getAllValues()).containsExactlyInAnyOrder(
                    SENDER_ID.toString(), RECEIVER_ID.toString());
        }

        @Test
        @DisplayName("Should send error when token is null")
        void shouldSendErrorWhenTokenIsNull() {
            Map<String, Object> message = new HashMap<>();
            message.put("receiverId", RECEIVER_ID.toString());
            message.put("content", "Hello!");

            chatWebSocketController.sendDirectMessage(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(SENDER_ID.toString()), eq("/queue/errors"), any(Map.class));
            verifyNoInteractions(directMessageService);
        }

        @Test
        @DisplayName("Should send error when empty token")
        void shouldSendErrorWhenEmptyToken() {
            Map<String, Object> message = new HashMap<>();
            message.put("token", "");
            message.put("receiverId", RECEIVER_ID.toString());
            message.put("content", "Hello!");

            chatWebSocketController.sendDirectMessage(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(SENDER_ID.toString()), eq("/queue/errors"), any(Map.class));
        }

        @Test
        @DisplayName("Should handle exception and send error")
        void shouldHandleExceptionAndSendError() {
            when(directMessageService.sendMessage(anyLong(), anyLong(), anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            Map<String, Object> message = new HashMap<>();
            message.put("token", BEARER_TOKEN);
            message.put("receiverId", RECEIVER_ID.toString());
            message.put("content", "Hello!");

            chatWebSocketController.sendDirectMessage(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(SENDER_ID.toString()), eq("/queue/errors"), any(Map.class));
        }

        @Test
        @DisplayName("Should not send error when principal is null")
        void shouldNotSendErrorWhenPrincipalIsNull() {
            when(headerAccessor.getUser()).thenReturn(null);

            Map<String, Object> message = new HashMap<>();
            message.put("receiverId", RECEIVER_ID.toString());
            message.put("content", "Hello!");

            chatWebSocketController.sendDirectMessage(message, headerAccessor);

            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("sendRoomMessage Tests")
    class SendRoomMessageTests {

        @Test
        @DisplayName("Should send room message successfully")
        void shouldSendRoomMessageSuccessfully() {
            GroupMessageEntity savedMessage = GroupMessageEntity.builder()
                    .roomId(ROOM_ID)
                    .senderId(SENDER_ID)
                    .content("Hello room!")
                    .timestamp(LocalDateTime.now())
                    .build();
            savedMessage.setId(1L);

            when(roomService.sendMessage(eq(ROOM_ID), eq(SENDER_ID), eq("Hello room!"), any()))
                    .thenReturn(savedMessage);
            when(roomService.getRoomMembers(ROOM_ID, SENDER_ID))
                    .thenReturn(Arrays.asList());

            Map<String, Object> message = new HashMap<>();
            message.put("token", BEARER_TOKEN);
            message.put("roomId", ROOM_ID.toString());
            message.put("content", "Hello room!");

            chatWebSocketController.sendRoomMessage(message, headerAccessor);

            verify(roomService).sendMessage(eq(ROOM_ID), eq(SENDER_ID), eq("Hello room!"), any());
            verify(messagingTemplate).convertAndSend(eq("/topic/room." + ROOM_ID), any(Map.class));
        }

        @Test
        @DisplayName("Should send error when no authentication for room")
        void shouldSendErrorWhenNoAuthForRoom() {
            Map<String, Object> message = new HashMap<>();
            message.put("roomId", ROOM_ID.toString());
            message.put("content", "Hello room!");

            chatWebSocketController.sendRoomMessage(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(SENDER_ID.toString()), eq("/queue/errors"), any(Map.class));
            verifyNoInteractions(roomService);
        }

        @Test
        @DisplayName("Should handle exception in room message")
        void shouldHandleExceptionInRoomMessage() {
            when(roomService.sendMessage(anyLong(), anyLong(), anyString(), any()))
                    .thenThrow(new RuntimeException("Room not found"));

            Map<String, Object> message = new HashMap<>();
            message.put("token", BEARER_TOKEN);
            message.put("roomId", ROOM_ID.toString());
            message.put("content", "Hello room!");

            chatWebSocketController.sendRoomMessage(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(SENDER_ID.toString()), eq("/queue/errors"), any(Map.class));
        }

        @Test
        @DisplayName("Should not send error when principal is null on room error")
        void shouldNotSendErrorWhenPrincipalIsNullOnRoomError() {
            when(headerAccessor.getUser()).thenReturn(null);

            Map<String, Object> message = new HashMap<>();
            message.put("roomId", ROOM_ID.toString());
            message.put("content", "Hello room!");

            chatWebSocketController.sendRoomMessage(message, headerAccessor);

            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("handleTyping Tests")
    class HandleTypingTests {

        @Test
        @DisplayName("Should handle DM typing indicator")
        void shouldHandleDmTypingIndicator() {
            Map<String, Object> typingData = new HashMap<>();
            typingData.put("token", BEARER_TOKEN);
            typingData.put("type", "DM");
            typingData.put("receiverId", RECEIVER_ID.toString());
            typingData.put("isTyping", true);

            chatWebSocketController.handleTyping(typingData);

            verify(messagingTemplate).convertAndSendToUser(
                    eq(RECEIVER_ID.toString()), eq("/queue/typing"), any(Map.class));
        }

        @Test
        @DisplayName("Should handle room typing indicator")
        void shouldHandleRoomTypingIndicator() {
            Map<String, Object> typingData = new HashMap<>();
            typingData.put("token", BEARER_TOKEN);
            typingData.put("type", "ROOM");
            typingData.put("roomId", ROOM_ID.toString());
            typingData.put("isTyping", true);

            chatWebSocketController.handleTyping(typingData);

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room." + ROOM_ID + ".typing"), any(Map.class));
        }

        @Test
        @DisplayName("Should skip when no token in typing")
        void shouldSkipWhenNoTokenInTyping() {
            Map<String, Object> typingData = new HashMap<>();
            typingData.put("type", "DM");
            typingData.put("receiverId", RECEIVER_ID.toString());
            typingData.put("isTyping", true);

            chatWebSocketController.handleTyping(typingData);

            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Should handle exception in typing")
        void shouldHandleExceptionInTyping() {
            when(jwtUtil.getUserIdFromToken(anyString())).thenThrow(new RuntimeException("Invalid token"));

            Map<String, Object> typingData = new HashMap<>();
            typingData.put("token", BEARER_TOKEN);
            typingData.put("type", "DM");
            typingData.put("receiverId", RECEIVER_ID.toString());
            typingData.put("isTyping", true);

            // Should not throw, just log error
            chatWebSocketController.handleTyping(typingData);

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("updateUserStatus Tests")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Should update user status successfully")
        void shouldUpdateUserStatusSuccessfully() {
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("token", BEARER_TOKEN);
            statusData.put("status", "ONLINE");

            chatWebSocketController.updateUserStatus(statusData);

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/user-status"), mapCaptor.capture());

            Map<String, Object> response = mapCaptor.getValue();
            assertThat(response.get("userId")).isEqualTo(SENDER_ID);
            assertThat(response.get("status")).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("Should skip when no token in status update")
        void shouldSkipWhenNoTokenInStatusUpdate() {
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", "ONLINE");

            chatWebSocketController.updateUserStatus(statusData);

            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Should handle exception in status update")
        void shouldHandleExceptionInStatusUpdate() {
            when(jwtUtil.getUserIdFromToken(anyString())).thenThrow(new RuntimeException("Invalid token"));

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("token", BEARER_TOKEN);
            statusData.put("status", "ONLINE");

            // Should not throw, just log error
            chatWebSocketController.updateUserStatus(statusData);

            verifyNoInteractions(messagingTemplate);
        }
    }
}
