package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventListener Tests")
class WebSocketEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private WebSocketEventListener webSocketEventListener;

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "test-session-id";

    @BeforeEach
    void setUp() {
        webSocketEventListener = new WebSocketEventListener();
        ReflectionTestUtils.setField(webSocketEventListener, "userRepository", userRepository);
        ReflectionTestUtils.setField(webSocketEventListener, "messagingTemplate", messagingTemplate);
    }

    private Message<byte[]> createMessage(String sessionId, Long userId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpMessageType", SimpMessageType.CONNECT);
        headers.put("simpSessionId", sessionId);

        Map<String, Object> sessionAttributes = new HashMap<>();
        if (userId != null) {
            sessionAttributes.put("userId", userId);
        }
        headers.put("simpSessionAttributes", sessionAttributes);

        return new GenericMessage<>(new byte[0], new MessageHeaders(headers));
    }

    @Nested
    @DisplayName("handleWebSocketConnectListener Tests")
    class HandleConnectTests {

        @Test
        @DisplayName("Should update user online status on connect")
        void shouldUpdateUserOnlineStatusOnConnect() {
            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .isOnline(false)
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, USER_ID);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            webSocketEventListener.handleWebSocketConnectListener(event);

            verify(userRepository).findById(USER_ID);
            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());

            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getIsOnline()).isTrue();
            assertThat(savedUser.getLastSeen()).isNotNull();
        }

        @Test
        @DisplayName("Should broadcast status update on connect")
        void shouldBroadcastStatusUpdateOnConnect() {
            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, USER_ID);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            webSocketEventListener.handleWebSocketConnectListener(event);

            verify(messagingTemplate).convertAndSend(eq("/topic/user-status"), any(Map.class));
        }

        @Test
        @DisplayName("Should not broadcast when messagingTemplate is null")
        void shouldNotBroadcastWhenMessagingTemplateIsNull() {
            ReflectionTestUtils.setField(webSocketEventListener, "messagingTemplate", null);

            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, USER_ID);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            webSocketEventListener.handleWebSocketConnectListener(event);

            verify(userRepository).save(any(UserEntity.class));
            // messagingTemplate is null, so no broadcast should happen
        }

        @Test
        @DisplayName("Should skip processing when userId is null")
        void shouldSkipProcessingWhenUserIdIsNull() {
            Message<byte[]> message = createMessage(SESSION_ID, null);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            webSocketEventListener.handleWebSocketConnectListener(event);

            verifyNoInteractions(userRepository);
            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Should skip processing when user not found")
        void shouldSkipProcessingWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            Message<byte[]> message = createMessage(SESSION_ID, USER_ID);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            webSocketEventListener.handleWebSocketConnectListener(event);

            verify(userRepository).findById(USER_ID);
            verify(userRepository, never()).save(any());
            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Should store session user mapping on connect")
        void shouldStoreSessionUserMappingOnConnect() {
            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, USER_ID);
            SessionConnectedEvent event = new SessionConnectedEvent(this, message);

            webSocketEventListener.handleWebSocketConnectListener(event);

            @SuppressWarnings("unchecked")
            Map<String, Long> sessionUserMap = (Map<String, Long>) ReflectionTestUtils.getField(webSocketEventListener, "sessionUserMap");
            assertThat(sessionUserMap).containsEntry(SESSION_ID, USER_ID);
        }
    }

    @Nested
    @DisplayName("handleWebSocketDisconnectListener Tests")
    class HandleDisconnectTests {

        @BeforeEach
        void setUpSessionMap() {
            @SuppressWarnings("unchecked")
            Map<String, Long> sessionUserMap = (Map<String, Long>) ReflectionTestUtils.getField(webSocketEventListener, "sessionUserMap");
            sessionUserMap.put(SESSION_ID, USER_ID);
        }

        @Test
        @DisplayName("Should update user offline status on disconnect")
        void shouldUpdateUserOfflineStatusOnDisconnect() {
            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .isOnline(true)
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, null);
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

            webSocketEventListener.handleWebSocketDisconnectListener(event);

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());

            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getIsOnline()).isFalse();
            assertThat(savedUser.getLastSeen()).isNotNull();
        }

        @Test
        @DisplayName("Should broadcast offline status on disconnect")
        void shouldBroadcastOfflineStatusOnDisconnect() {
            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, null);
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

            webSocketEventListener.handleWebSocketDisconnectListener(event);

            ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/user-status"), mapCaptor.capture());

            Map<String, Object> statusUpdate = mapCaptor.getValue();
            assertThat(statusUpdate.get("userId")).isEqualTo(USER_ID);
            assertThat(statusUpdate.get("isOnline")).isEqualTo(false);
        }

        @Test
        @DisplayName("Should remove session mapping on disconnect")
        void shouldRemoveSessionMappingOnDisconnect() {
            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, null);
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

            webSocketEventListener.handleWebSocketDisconnectListener(event);

            @SuppressWarnings("unchecked")
            Map<String, Long> sessionUserMap = (Map<String, Long>) ReflectionTestUtils.getField(webSocketEventListener, "sessionUserMap");
            assertThat(sessionUserMap).doesNotContainKey(SESSION_ID);
        }

        @Test
        @DisplayName("Should skip processing when session not in map")
        void shouldSkipProcessingWhenSessionNotInMap() {
            @SuppressWarnings("unchecked")
            Map<String, Long> sessionUserMap = (Map<String, Long>) ReflectionTestUtils.getField(webSocketEventListener, "sessionUserMap");
            sessionUserMap.clear();

            Message<byte[]> message = createMessage("unknown-session", null);
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "unknown-session", null);

            webSocketEventListener.handleWebSocketDisconnectListener(event);

            verifyNoInteractions(userRepository);
            verifyNoInteractions(messagingTemplate);
        }

        @Test
        @DisplayName("Should not broadcast when messagingTemplate is null on disconnect")
        void shouldNotBroadcastWhenMessagingTemplateIsNullOnDisconnect() {
            ReflectionTestUtils.setField(webSocketEventListener, "messagingTemplate", null);

            UserEntity user = UserEntity.builder()
                    .username("testuser")
                    .displayName("Test User")
                    .password("password")
                    .email("test@test.com")
                    .phoneNumber("01012345678")
                    .build();
            user.setId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenReturn(user);

            Message<byte[]> message = createMessage(SESSION_ID, null);
            SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

            webSocketEventListener.handleWebSocketDisconnectListener(event);

            verify(userRepository).save(any(UserEntity.class));
            // No broadcast since messagingTemplate is null
        }
    }
}
