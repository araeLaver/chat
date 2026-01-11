package com.beam;

import com.beam.NotificationEntity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long USER_ID = 1L;
    private static final Long ACTOR_USER_ID = 2L;
    private static final Long MESSAGE_ID = 100L;
    private static final String ROOM_ID = "room-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "contentMaxLength", 100);
    }

    @Nested
    @DisplayName("createNotification tests")
    class CreateNotificationTests {

        @Test
        @DisplayName("should create notification successfully")
        void createNotification_Success() {
            when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationEntity result = notificationService.createNotification(
                USER_ID, NotificationType.MENTION, MESSAGE_ID, ACTOR_USER_ID, "Test content", ROOM_ID);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(NotificationType.MENTION, result.getType());
            assertEquals(MESSAGE_ID, result.getReferenceId());
            assertEquals(ACTOR_USER_ID, result.getActorUserId());
            assertEquals("Test content", result.getContent());
            assertEquals(ROOM_ID, result.getRoomId());
            verify(notificationRepository).save(any(NotificationEntity.class));
        }

        @Test
        @DisplayName("should truncate long content")
        void createNotification_TruncateLongContent() {
            String longContent = "A".repeat(150);

            when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationEntity result = notificationService.createNotification(
                USER_ID, NotificationType.MENTION, MESSAGE_ID, ACTOR_USER_ID, longContent, ROOM_ID);

            assertEquals(100, result.getContent().length());
            assertTrue(result.getContent().endsWith("..."));
        }
    }

    @Nested
    @DisplayName("createMentionNotification tests")
    class CreateMentionNotificationTests {

        @Test
        @DisplayName("should create mention notification")
        void createMentionNotification_Success() {
            when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationEntity result = notificationService.createMentionNotification(
                USER_ID, ACTOR_USER_ID, MESSAGE_ID, "Hello @user", ROOM_ID);

            assertNotNull(result);
            assertEquals(NotificationType.MENTION, result.getType());
        }
    }

    @Nested
    @DisplayName("createReactionNotification tests")
    class CreateReactionNotificationTests {

        @Test
        @DisplayName("should create reaction notification with emoji")
        void createReactionNotification_Success() {
            when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationEntity result = notificationService.createReactionNotification(
                USER_ID, ACTOR_USER_ID, MESSAGE_ID, "👍", ROOM_ID);

            assertNotNull(result);
            assertEquals(NotificationType.REACTION, result.getType());
            assertTrue(result.getContent().contains("👍"));
        }
    }

    @Nested
    @DisplayName("createReplyNotification tests")
    class CreateReplyNotificationTests {

        @Test
        @DisplayName("should create reply notification")
        void createReplyNotification_Success() {
            when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationEntity result = notificationService.createReplyNotification(
                USER_ID, ACTOR_USER_ID, MESSAGE_ID, "This is a reply", ROOM_ID);

            assertNotNull(result);
            assertEquals(NotificationType.REPLY, result.getType());
        }
    }

    @Nested
    @DisplayName("createPinNotification tests")
    class CreatePinNotificationTests {

        @Test
        @DisplayName("should create pin notification")
        void createPinNotification_Success() {
            when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            NotificationEntity result = notificationService.createPinNotification(
                USER_ID, ACTOR_USER_ID, MESSAGE_ID, ROOM_ID);

            assertNotNull(result);
            assertEquals(NotificationType.PIN, result.getType());
        }
    }

    @Nested
    @DisplayName("getNotifications tests")
    class GetNotificationsTests {

        @Test
        @DisplayName("should return paginated notifications")
        void getNotifications_Paginated() {
            List<NotificationEntity> notifications = Arrays.asList(
                NotificationEntity.builder().id(1L).userId(USER_ID).type(NotificationType.MENTION).build(),
                NotificationEntity.builder().id(2L).userId(USER_ID).type(NotificationType.REACTION).build()
            );
            Page<NotificationEntity> page = new PageImpl<>(notifications);

            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(page);

            Page<NotificationEntity> result = notificationService.getNotifications(USER_ID, 0, 20);

            assertEquals(2, result.getContent().size());
        }

        @Test
        @DisplayName("should limit page size to 50")
        void getNotifications_LimitPageSize() {
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(Page.empty());

            notificationService.getNotifications(USER_ID, 0, 100);

            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(
                eq(USER_ID), argThat(pageable -> pageable.getPageSize() == 50));
        }
    }

    @Nested
    @DisplayName("getUnreadNotifications tests")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("should return unread notifications")
        void getUnreadNotifications_Success() {
            List<NotificationEntity> notifications = Arrays.asList(
                NotificationEntity.builder().id(1L).userId(USER_ID).isRead(false).build(),
                NotificationEntity.builder().id(2L).userId(USER_ID).isRead(false).build()
            );

            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID))
                .thenReturn(notifications);

            List<NotificationEntity> result = notificationService.getUnreadNotifications(USER_ID);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("getUnreadCount tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("should return unread count")
        void getUnreadCount_Success() {
            when(notificationRepository.countUnreadNotifications(USER_ID)).thenReturn(5);

            Integer count = notificationService.getUnreadCount(USER_ID);

            assertEquals(5, count);
        }
    }

    @Nested
    @DisplayName("markAllAsRead tests")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("should mark all notifications as read")
        void markAllAsRead_Success() {
            doNothing().when(notificationRepository).markAllAsRead(eq(USER_ID), any(LocalDateTime.class));

            assertDoesNotThrow(() -> notificationService.markAllAsRead(USER_ID));

            verify(notificationRepository).markAllAsRead(eq(USER_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("markAsRead tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("should mark specific notification as read")
        void markAsRead_Success() {
            Long notificationId = 1L;
            doNothing().when(notificationRepository).markAsRead(eq(notificationId), eq(USER_ID), any(LocalDateTime.class));

            assertDoesNotThrow(() -> notificationService.markAsRead(notificationId, USER_ID));

            verify(notificationRepository).markAsRead(eq(notificationId), eq(USER_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("deleteOldNotifications tests")
    class DeleteOldNotificationsTests {

        @Test
        @DisplayName("should delete old notifications")
        void deleteOldNotifications_Success() {
            doNothing().when(notificationRepository).deleteByUserIdAndCreatedAtBefore(eq(USER_ID), any(LocalDateTime.class));

            assertDoesNotThrow(() -> notificationService.deleteOldNotifications(USER_ID, 30));

            verify(notificationRepository).deleteByUserIdAndCreatedAtBefore(eq(USER_ID), any(LocalDateTime.class));
        }
    }
}
