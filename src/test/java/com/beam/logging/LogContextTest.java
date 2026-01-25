package com.beam.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogContext Tests")
class LogContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("forRequest() Tests")
    class ForRequestTests {

        @Test
        @DisplayName("Should create context with request ID")
        void shouldCreateContextWithRequestId() {
            try (LogContext context = LogContext.forRequest()) {
                String requestId = MDC.get(LogContext.REQUEST_ID);
                assertThat(requestId).isNotNull();
                assertThat(requestId).hasSize(8);
            }
        }

        @Test
        @DisplayName("Should clear MDC on close")
        void shouldClearMdcOnClose() {
            LogContext context = LogContext.forRequest();
            assertThat(MDC.get(LogContext.REQUEST_ID)).isNotNull();

            context.close();
            assertThat(MDC.get(LogContext.REQUEST_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("forUser() Tests")
    class ForUserTests {

        @Test
        @DisplayName("Should create context with user ID and username")
        void shouldCreateContextWithUserIdAndUsername() {
            try (LogContext context = LogContext.forUser(123L, "testuser")) {
                assertThat(MDC.get(LogContext.REQUEST_ID)).isNotNull();
                assertThat(MDC.get(LogContext.USER_ID)).isEqualTo("123");
                assertThat(MDC.get(LogContext.USERNAME)).isEqualTo("testuser");
            }
        }

        @Test
        @DisplayName("Should handle null userId")
        void shouldHandleNullUserId() {
            try (LogContext context = LogContext.forUser(null, "testuser")) {
                assertThat(MDC.get(LogContext.USER_ID)).isNull();
                assertThat(MDC.get(LogContext.USERNAME)).isEqualTo("testuser");
            }
        }

        @Test
        @DisplayName("Should handle null username")
        void shouldHandleNullUsername() {
            try (LogContext context = LogContext.forUser(123L, null)) {
                assertThat(MDC.get(LogContext.USER_ID)).isEqualTo("123");
                assertThat(MDC.get(LogContext.USERNAME)).isNull();
            }
        }
    }

    @Nested
    @DisplayName("forWebSocket() Tests")
    class ForWebSocketTests {

        @Test
        @DisplayName("Should create context with session, user, and username")
        void shouldCreateContextWithAllParams() {
            try (LogContext context = LogContext.forWebSocket("session-123", 456L, "wsuser")) {
                assertThat(MDC.get(LogContext.REQUEST_ID)).isNotNull();
                assertThat(MDC.get(LogContext.SESSION_ID)).isEqualTo("session-123");
                assertThat(MDC.get(LogContext.USER_ID)).isEqualTo("456");
                assertThat(MDC.get(LogContext.USERNAME)).isEqualTo("wsuser");
            }
        }

        @Test
        @DisplayName("Should handle null session ID")
        void shouldHandleNullSessionId() {
            try (LogContext context = LogContext.forWebSocket(null, 456L, "wsuser")) {
                assertThat(MDC.get(LogContext.SESSION_ID)).isNull();
                assertThat(MDC.get(LogContext.USER_ID)).isEqualTo("456");
            }
        }
    }

    @Nested
    @DisplayName("Builder method Tests")
    class BuilderMethodTests {

        @Test
        @DisplayName("Should chain withRoom")
        void shouldChainWithRoom() {
            try (LogContext context = LogContext.forRequest().withRoom(789L)) {
                assertThat(MDC.get(LogContext.ROOM_ID)).isEqualTo("789");
            }
        }

        @Test
        @DisplayName("Should handle null room ID")
        void shouldHandleNullRoomId() {
            try (LogContext context = LogContext.forRequest().withRoom(null)) {
                assertThat(MDC.get(LogContext.ROOM_ID)).isNull();
            }
        }

        @Test
        @DisplayName("Should chain withAction")
        void shouldChainWithAction() {
            try (LogContext context = LogContext.forRequest().withAction("SEND_MESSAGE")) {
                assertThat(MDC.get(LogContext.ACTION)).isEqualTo("SEND_MESSAGE");
            }
        }

        @Test
        @DisplayName("Should handle null action")
        void shouldHandleNullAction() {
            try (LogContext context = LogContext.forRequest().withAction(null)) {
                assertThat(MDC.get(LogContext.ACTION)).isNull();
            }
        }

        @Test
        @DisplayName("Should chain withClientIp")
        void shouldChainWithClientIp() {
            try (LogContext context = LogContext.forRequest().withClientIp("192.168.1.100")) {
                assertThat(MDC.get(LogContext.CLIENT_IP)).isEqualTo("192.168.1.100");
            }
        }

        @Test
        @DisplayName("Should handle null client IP")
        void shouldHandleNullClientIp() {
            try (LogContext context = LogContext.forRequest().withClientIp(null)) {
                assertThat(MDC.get(LogContext.CLIENT_IP)).isNull();
            }
        }

        @Test
        @DisplayName("Should chain with custom key-value")
        void shouldChainWithCustomKeyValue() {
            try (LogContext context = LogContext.forRequest().with("customKey", "customValue")) {
                assertThat(MDC.get("customKey")).isEqualTo("customValue");
            }
        }

        @Test
        @DisplayName("Should handle null custom key")
        void shouldHandleNullCustomKey() {
            try (LogContext context = LogContext.forRequest().with(null, "value")) {
                // Should not throw, just ignore
            }
        }

        @Test
        @DisplayName("Should handle null custom value")
        void shouldHandleNullCustomValue() {
            try (LogContext context = LogContext.forRequest().with("key", null)) {
                assertThat(MDC.get("key")).isNull();
            }
        }

        @Test
        @DisplayName("Should support method chaining")
        void shouldSupportMethodChaining() {
            try (LogContext context = LogContext.forRequest()
                    .withRoom(100L)
                    .withAction("TEST")
                    .withClientIp("10.0.0.1")
                    .with("extra", "data")) {
                assertThat(MDC.get(LogContext.ROOM_ID)).isEqualTo("100");
                assertThat(MDC.get(LogContext.ACTION)).isEqualTo("TEST");
                assertThat(MDC.get(LogContext.CLIENT_IP)).isEqualTo("10.0.0.1");
                assertThat(MDC.get("extra")).isEqualTo("data");
            }
        }
    }

    @Nested
    @DisplayName("Static getter Tests")
    class StaticGetterTests {

        @Test
        @DisplayName("Should get current request ID")
        void shouldGetCurrentRequestId() {
            try (LogContext context = LogContext.forRequest()) {
                String requestId = LogContext.getCurrentRequestId();
                assertThat(requestId).isNotNull();
                assertThat(requestId).hasSize(8);
            }
        }

        @Test
        @DisplayName("Should return null when no request ID set")
        void shouldReturnNullWhenNoRequestIdSet() {
            assertThat(LogContext.getCurrentRequestId()).isNull();
        }

        @Test
        @DisplayName("Should get current user ID")
        void shouldGetCurrentUserId() {
            try (LogContext context = LogContext.forUser(999L, "user")) {
                Long userId = LogContext.getCurrentUserId();
                assertThat(userId).isEqualTo(999L);
            }
        }

        @Test
        @DisplayName("Should return null when no user ID set")
        void shouldReturnNullWhenNoUserIdSet() {
            assertThat(LogContext.getCurrentUserId()).isNull();
        }
    }

    @Nested
    @DisplayName("clear() Tests")
    class ClearTests {

        @Test
        @DisplayName("Should clear all MDC entries")
        void shouldClearAllMdcEntries() {
            LogContext.forRequest()
                    .withRoom(1L)
                    .withAction("ACTION")
                    .withClientIp("1.2.3.4");

            assertThat(MDC.get(LogContext.REQUEST_ID)).isNotNull();

            LogContext.clear();

            assertThat(MDC.get(LogContext.REQUEST_ID)).isNull();
            assertThat(MDC.get(LogContext.ROOM_ID)).isNull();
            assertThat(MDC.get(LogContext.ACTION)).isNull();
            assertThat(MDC.get(LogContext.CLIENT_IP)).isNull();
        }
    }
}
