package com.beam;

import com.beam.MentionEntity.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentionService Unit Tests")
class MentionServiceTest {

    @Mock
    private MentionRepository mentionRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MentionService mentionService;

    private static final Long MESSAGE_ID = 100L;
    private static final Long SENDER_ID = 1L;
    private static final Long MENTIONED_USER_ID = 2L;
    private static final String ROOM_ID = "room-123";
    private static final MessageType MESSAGE_TYPE = MessageType.ROOM;

    @Nested
    @DisplayName("processMentions tests")
    class ProcessMentionsTests {

        @Test
        @DisplayName("should process mentions in content")
        void processMentions_WithMentions() {
            String content = "Hello @john and @jane!";

            UserEntity john = new UserEntity();
            john.setId(2L);
            john.setUsername("john");

            UserEntity jane = new UserEntity();
            jane.setId(3L);
            jane.setUsername("jane");

            when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));
            when(userRepository.findByUsername("jane")).thenReturn(Optional.of(jane));
            when(mentionRepository.existsByMessageIdAndMessageTypeAndMentionedUserId(
                eq(MESSAGE_ID), eq(MESSAGE_TYPE), anyLong())).thenReturn(false);
            when(mentionRepository.save(any(MentionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            List<MentionEntity> result = mentionService.processMentions(
                content, MESSAGE_ID, MESSAGE_TYPE, SENDER_ID, ROOM_ID, null);

            assertEquals(2, result.size());
            verify(notificationService, times(2)).createMentionNotification(
                anyLong(), eq(SENDER_ID), eq(MESSAGE_ID), eq(content), eq(ROOM_ID));
        }

        @Test
        @DisplayName("should not create self-mention")
        void processMentions_SelfMention() {
            String content = "Hello @sender!";

            UserEntity sender = new UserEntity();
            sender.setId(SENDER_ID);
            sender.setUsername("sender");

            when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));

            List<MentionEntity> result = mentionService.processMentions(
                content, MESSAGE_ID, MESSAGE_TYPE, SENDER_ID, ROOM_ID, null);

            assertTrue(result.isEmpty());
            verify(mentionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip duplicate mentions")
        void processMentions_DuplicateMention() {
            String content = "Hello @john and @john again!";

            UserEntity john = new UserEntity();
            john.setId(2L);
            john.setUsername("john");

            when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));
            when(mentionRepository.existsByMessageIdAndMessageTypeAndMentionedUserId(
                MESSAGE_ID, MESSAGE_TYPE, 2L))
                .thenReturn(false)
                .thenReturn(true);
            when(mentionRepository.save(any(MentionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            List<MentionEntity> result = mentionService.processMentions(
                content, MESSAGE_ID, MESSAGE_TYPE, SENDER_ID, ROOM_ID, null);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should return empty list for null content")
        void processMentions_NullContent() {
            List<MentionEntity> result = mentionService.processMentions(
                null, MESSAGE_ID, MESSAGE_TYPE, SENDER_ID, ROOM_ID, null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for empty content")
        void processMentions_EmptyContent() {
            List<MentionEntity> result = mentionService.processMentions(
                "", MESSAGE_ID, MESSAGE_TYPE, SENDER_ID, ROOM_ID, null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should skip non-existent users")
        void processMentions_NonExistentUser() {
            String content = "Hello @unknown!";

            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            List<MentionEntity> result = mentionService.processMentions(
                content, MESSAGE_ID, MESSAGE_TYPE, SENDER_ID, ROOM_ID, null);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getUnreadMentions tests")
    class GetUnreadMentionsTests {

        @Test
        @DisplayName("should return unread mentions")
        void getUnreadMentions_Success() {
            List<MentionEntity> mentions = Arrays.asList(
                MentionEntity.builder().id(1L).mentionedUserId(MENTIONED_USER_ID).isRead(false).build(),
                MentionEntity.builder().id(2L).mentionedUserId(MENTIONED_USER_ID).isRead(false).build()
            );

            when(mentionRepository.findByMentionedUserIdAndIsReadFalseOrderByCreatedAtDesc(MENTIONED_USER_ID))
                .thenReturn(mentions);

            List<MentionEntity> result = mentionService.getUnreadMentions(MENTIONED_USER_ID);

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("getUnreadMentionCount tests")
    class GetUnreadMentionCountTests {

        @Test
        @DisplayName("should return unread mention count")
        void getUnreadMentionCount_Success() {
            when(mentionRepository.countUnreadMentions(MENTIONED_USER_ID)).thenReturn(5);

            Integer count = mentionService.getUnreadMentionCount(MENTIONED_USER_ID);

            assertEquals(5, count);
        }
    }

    @Nested
    @DisplayName("markAllAsRead tests")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("should mark all mentions as read")
        void markAllAsRead_Success() {
            doNothing().when(mentionRepository).markAllAsRead(eq(MENTIONED_USER_ID), any(LocalDateTime.class));

            assertDoesNotThrow(() -> mentionService.markAllAsRead(MENTIONED_USER_ID));

            verify(mentionRepository).markAllAsRead(eq(MENTIONED_USER_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("extractMentionedUserIds tests")
    class ExtractMentionedUserIdsTests {

        @Test
        @DisplayName("should extract mentioned user IDs")
        void extractMentionedUserIds_Success() {
            String content = "Hello @john and @jane!";

            UserEntity john = new UserEntity();
            john.setId(2L);
            john.setUsername("john");

            UserEntity jane = new UserEntity();
            jane.setId(3L);
            jane.setUsername("jane");

            when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));
            when(userRepository.findByUsername("jane")).thenReturn(Optional.of(jane));

            List<Long> userIds = mentionService.extractMentionedUserIds(content);

            assertEquals(2, userIds.size());
            assertTrue(userIds.contains(2L));
            assertTrue(userIds.contains(3L));
        }

        @Test
        @DisplayName("should return empty list for null content")
        void extractMentionedUserIds_NullContent() {
            List<Long> userIds = mentionService.extractMentionedUserIds(null);

            assertTrue(userIds.isEmpty());
        }
    }
}
