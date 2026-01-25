package com.beam;

import com.beam.MentionEntity.MessageType;
import com.beam.NotificationEntity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private MentionService mentionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final Long ACTOR_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(USER_ID);

        UserEntity actor = UserEntity.builder()
                .username("actor")
                .displayName("Actor User")
                .password("password")
                .email("actor@test.com")
                .phoneNumber("01012345679")
                .build();
        actor.setId(ACTOR_USER_ID);
        when(userRepository.findById(ACTOR_USER_ID)).thenReturn(Optional.of(actor));
    }

    private NotificationEntity createNotification(Long id, NotificationType type, boolean isRead) {
        NotificationEntity entity = NotificationEntity.builder()
                .userId(USER_ID)
                .type(type)
                .referenceId(100L)
                .content("Test notification")
                .roomId("room-1")
                .actorUserId(ACTOR_USER_ID)
                .isRead(isRead)
                .createdAt(LocalDateTime.now())
                .build();
        entity.setId(id);
        return entity;
    }

    private MentionEntity createMention(Long id, boolean isRead) {
        MentionEntity entity = MentionEntity.builder()
                .mentionedUserId(USER_ID)
                .mentionerUserId(ACTOR_USER_ID)
                .messageId(100L)
                .messageType(MessageType.ROOM)
                .roomId("room-1")
                .isRead(isRead)
                .createdAt(LocalDateTime.now())
                .build();
        entity.setId(id);
        return entity;
    }

    @Nested
    @DisplayName("GET /api/notifications Tests")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should get notifications with pagination")
        void shouldGetNotificationsWithPagination() throws Exception {
            List<NotificationEntity> content = Arrays.asList(
                    createNotification(1L, NotificationType.MENTION, false),
                    createNotification(2L, NotificationType.FRIEND_REQUEST, true)
            );
            Page<NotificationEntity> page = new PageImpl<>(content, PageRequest.of(0, 20), 2);
            when(notificationService.getNotifications(USER_ID, 0, 20)).thenReturn(page);

            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", VALID_TOKEN)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.currentPage").value(0));
        }

        @Test
        @DisplayName("Should return empty list when no notifications")
        void shouldReturnEmptyListWhenNoNotifications() throws Exception {
            Page<NotificationEntity> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);
            when(notificationService.getNotifications(USER_ID, 0, 20)).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread Tests")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should get unread notifications")
        void shouldGetUnreadNotifications() throws Exception {
            List<NotificationEntity> notifications = Arrays.asList(
                    createNotification(1L, NotificationType.MENTION, false)
            );
            when(notificationService.getUnreadNotifications(USER_ID)).thenReturn(notifications);

            mockMvc.perform(get("/api/v1/notifications/unread")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].isRead").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread-count Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should get unread count")
        void shouldGetUnreadCount() throws Exception {
            when(notificationService.getUnreadCount(USER_ID)).thenReturn(5);
            when(mentionService.getUnreadMentionCount(USER_ID)).thenReturn(3);

            mockMvc.perform(get("/api/v1/notifications/unread-count")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notificationCount").value(5))
                    .andExpect(jsonPath("$.mentionCount").value(3))
                    .andExpect(jsonPath("$.totalCount").value(8));
        }

        @Test
        @DisplayName("Should return zero when no unread")
        void shouldReturnZeroWhenNoUnread() throws Exception {
            when(notificationService.getUnreadCount(USER_ID)).thenReturn(0);
            when(mentionService.getUnreadMentionCount(USER_ID)).thenReturn(0);

            mockMvc.perform(get("/api/v1/notifications/unread-count")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/read-all Tests")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all as read")
        void shouldMarkAllAsRead() throws Exception {
            doNothing().when(notificationService).markAllAsRead(USER_ID);
            doNothing().when(mentionService).markAllAsRead(USER_ID);

            mockMvc.perform(post("/api/v1/notifications/read-all")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService).markAllAsRead(USER_ID);
            verify(mentionService).markAllAsRead(USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/{notificationId}/read Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() throws Exception {
            doNothing().when(notificationService).markAsRead(1L, USER_ID);

            mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", 1L)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(notificationService).markAsRead(1L, USER_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/mentions Tests")
    class GetMentionsTests {

        @Test
        @DisplayName("Should get all mentions")
        void shouldGetAllMentions() throws Exception {
            List<MentionEntity> mentions = Arrays.asList(
                    createMention(1L, false),
                    createMention(2L, true)
            );
            when(mentionService.getAllMentions(USER_ID)).thenReturn(mentions);

            mockMvc.perform(get("/api/v1/notifications/mentions")
                            .header("Authorization", VALID_TOKEN)
                            .param("unreadOnly", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should get unread mentions only")
        void shouldGetUnreadMentionsOnly() throws Exception {
            List<MentionEntity> mentions = Arrays.asList(
                    createMention(1L, false)
            );
            when(mentionService.getUnreadMentions(USER_ID)).thenReturn(mentions);

            mockMvc.perform(get("/api/v1/notifications/mentions")
                            .header("Authorization", VALID_TOKEN)
                            .param("unreadOnly", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle exception gracefully")
        void shouldHandleExceptionGracefully() throws Exception {
            when(jwtUtil.getUserIdFromToken(anyString())).thenThrow(new RuntimeException("Invalid token"));

            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid token"));
        }
    }
}
