package com.beam.websocket;

import com.beam.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WebSocketSessionManager Unit Tests")
class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
    }

    @Nested
    @DisplayName("Session Management Tests")
    class SessionManagementTests {

        @Test
        @DisplayName("Should add session successfully")
        void shouldAddSession() {
            // Given
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-1");

            // When
            sessionManager.addSession(session);

            // Then
            Set<WebSocketSession> sessions = sessionManager.getAllSessions();
            assertThat(sessions).contains(session);
        }

        @Test
        @DisplayName("Should remove session successfully")
        void shouldRemoveSession() {
            // Given
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-1");
            sessionManager.addSession(session);

            // When
            sessionManager.removeSession(session);

            // Then
            assertThat(sessionManager.getAllSessions()).doesNotContain(session);
        }

        @Test
        @DisplayName("Should find session by ID")
        void shouldFindSessionById() {
            // Given
            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            when(session1.getId()).thenReturn("session-1");
            when(session2.getId()).thenReturn("session-2");
            sessionManager.addSession(session1);
            sessionManager.addSession(session2);

            // When
            WebSocketSession found = sessionManager.findSessionById("session-2");

            // Then
            assertThat(found).isEqualTo(session2);
        }

        @Test
        @DisplayName("Should return null when session not found")
        void shouldReturnNullWhenSessionNotFound() {
            // When
            WebSocketSession found = sessionManager.findSessionById("non-existent");

            // Then
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Should handle multiple sessions")
        void shouldHandleMultipleSessions() {
            // Given
            WebSocketSession session1 = mock(WebSocketSession.class);
            WebSocketSession session2 = mock(WebSocketSession.class);
            WebSocketSession session3 = mock(WebSocketSession.class);
            when(session1.getId()).thenReturn("session-1");
            when(session2.getId()).thenReturn("session-2");
            when(session3.getId()).thenReturn("session-3");

            // When
            sessionManager.addSession(session1);
            sessionManager.addSession(session2);
            sessionManager.addSession(session3);

            // Then
            assertThat(sessionManager.getAllSessions()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("Should add and get user")
        void shouldAddAndGetUser() {
            // Given
            User user = new User("user-1", "testuser", "session-1");

            // When
            sessionManager.addUser("session-1", user);
            User retrieved = sessionManager.getUser("session-1");

            // Then
            assertThat(retrieved).isEqualTo(user);
            assertThat(retrieved.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should remove user")
        void shouldRemoveUser() {
            // Given
            User user = new User("user-1", "testuser", "session-1");
            sessionManager.addUser("session-1", user);

            // When
            User removed = sessionManager.removeUser("session-1");

            // Then
            assertThat(removed).isEqualTo(user);
            assertThat(sessionManager.getUser("session-1")).isNull();
        }

        @Test
        @DisplayName("Should return null for non-existent user")
        void shouldReturnNullForNonExistentUser() {
            // When
            User user = sessionManager.getUser("non-existent");

            // Then
            assertThat(user).isNull();
        }

        @Test
        @DisplayName("Should clean up user when session removed")
        void shouldCleanUpUserWhenSessionRemoved() {
            // Given
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-1");
            User user = new User("user-1", "testuser", "session-1");
            sessionManager.addSession(session);
            sessionManager.addUser("session-1", user);

            // When
            sessionManager.removeSession(session);

            // Then
            assertThat(sessionManager.getUser("session-1")).isNull();
        }
    }

    @Nested
    @DisplayName("Session Room Mapping Tests")
    class SessionRoomMappingTests {

        @Test
        @DisplayName("Should set and get session room")
        void shouldSetAndGetSessionRoom() {
            // When
            sessionManager.setSessionRoom("session-1", "room-1");
            String roomId = sessionManager.getSessionRoom("session-1");

            // Then
            assertThat(roomId).isEqualTo("room-1");
        }

        @Test
        @DisplayName("Should remove session room")
        void shouldRemoveSessionRoom() {
            // Given
            sessionManager.setSessionRoom("session-1", "room-1");

            // When
            sessionManager.removeSessionRoom("session-1");

            // Then
            assertThat(sessionManager.getSessionRoom("session-1")).isNull();
        }

        @Test
        @DisplayName("Should return null for unmapped session")
        void shouldReturnNullForUnmappedSession() {
            // When
            String roomId = sessionManager.getSessionRoom("unmapped-session");

            // Then
            assertThat(roomId).isNull();
        }

        @Test
        @DisplayName("Should update session room")
        void shouldUpdateSessionRoom() {
            // Given
            sessionManager.setSessionRoom("session-1", "room-1");

            // When
            sessionManager.setSessionRoom("session-1", "room-2");

            // Then
            assertThat(sessionManager.getSessionRoom("session-1")).isEqualTo("room-2");
        }

        @Test
        @DisplayName("Should clean up room mapping when session removed")
        void shouldCleanUpRoomMappingWhenSessionRemoved() {
            // Given
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("session-1");
            sessionManager.addSession(session);
            sessionManager.setSessionRoom("session-1", "room-1");

            // When
            sessionManager.removeSession(session);

            // Then
            assertThat(sessionManager.getSessionRoom("session-1")).isNull();
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent session additions")
        void shouldHandleConcurrentSessionAdditions() throws InterruptedException {
            // Given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    WebSocketSession session = mock(WebSocketSession.class);
                    when(session.getId()).thenReturn("session-" + index);
                    sessionManager.addSession(session);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            assertThat(sessionManager.getAllSessions()).hasSize(threadCount);
        }
    }
}
