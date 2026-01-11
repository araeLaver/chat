package com.beam;

import com.beam.StarredMessageEntity.MessageType;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StarService Unit Tests")
class StarServiceTest {

    @Mock
    private StarredMessageRepository starredMessageRepository;

    @InjectMocks
    private StarService starService;

    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;
    private static final String ROOM_ID = "room-123";
    private static final MessageType MESSAGE_TYPE = MessageType.ROOM;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(starService, "maxStarsPerUser", 1000);
    }

    @Nested
    @DisplayName("starMessage tests")
    class StarMessageTests {

        @Test
        @DisplayName("should star message successfully")
        void starMessage_Success() {
            when(starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(false);
            when(starredMessageRepository.countByUserId(USER_ID)).thenReturn(10);
            when(starredMessageRepository.save(any(StarredMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            StarredMessageEntity result = starService.starMessage(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE, ROOM_ID, null, "Important message");

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(MESSAGE_ID, result.getMessageId());
            assertEquals(MESSAGE_TYPE, result.getMessageType());
            assertEquals(ROOM_ID, result.getRoomId());
            assertEquals("Important message", result.getNote());
            verify(starredMessageRepository).save(any(StarredMessageEntity.class));
        }

        @Test
        @DisplayName("should throw exception when message already starred")
        void starMessage_AlreadyStarred() {
            when(starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(true);

            assertThrows(IllegalStateException.class, () ->
                starService.starMessage(USER_ID, MESSAGE_ID, MESSAGE_TYPE, ROOM_ID, null, null));

            verify(starredMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when max stars reached")
        void starMessage_MaxStarsReached() {
            when(starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(false);
            when(starredMessageRepository.countByUserId(USER_ID)).thenReturn(1000);

            assertThrows(IllegalStateException.class, () ->
                starService.starMessage(USER_ID, MESSAGE_ID, MESSAGE_TYPE, ROOM_ID, null, null));

            verify(starredMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unstarMessage tests")
    class UnstarMessageTests {

        @Test
        @DisplayName("should unstar message successfully")
        void unstarMessage_Success() {
            doNothing().when(starredMessageRepository)
                .deleteByUserIdAndMessageIdAndMessageType(USER_ID, MESSAGE_ID, MESSAGE_TYPE);

            assertDoesNotThrow(() -> starService.unstarMessage(USER_ID, MESSAGE_ID, MESSAGE_TYPE));

            verify(starredMessageRepository).deleteByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE);
        }
    }

    @Nested
    @DisplayName("toggleStar tests")
    class ToggleStarTests {

        @Test
        @DisplayName("should add star when not exists")
        void toggleStar_Add() {
            when(starredMessageRepository.findByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(Optional.empty());
            when(starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(false);
            when(starredMessageRepository.countByUserId(USER_ID)).thenReturn(0);
            when(starredMessageRepository.save(any(StarredMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            StarredMessageEntity result = starService.toggleStar(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE, ROOM_ID, null, null);

            assertNotNull(result);
            verify(starredMessageRepository).save(any(StarredMessageEntity.class));
        }

        @Test
        @DisplayName("should remove star when exists")
        void toggleStar_Remove() {
            StarredMessageEntity existing = StarredMessageEntity.builder()
                .id(1L)
                .userId(USER_ID)
                .messageId(MESSAGE_ID)
                .messageType(MESSAGE_TYPE)
                .build();

            when(starredMessageRepository.findByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(Optional.of(existing));

            StarredMessageEntity result = starService.toggleStar(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE, ROOM_ID, null, null);

            assertNull(result);
            verify(starredMessageRepository).delete(existing);
        }
    }

    @Nested
    @DisplayName("getStarredMessages tests")
    class GetStarredMessagesTests {

        @Test
        @DisplayName("should return all starred messages")
        void getStarredMessages_All() {
            List<StarredMessageEntity> starredMessages = Arrays.asList(
                StarredMessageEntity.builder().id(1L).userId(USER_ID).messageId(101L).build(),
                StarredMessageEntity.builder().id(2L).userId(USER_ID).messageId(102L).build()
            );

            when(starredMessageRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(starredMessages);

            List<StarredMessageEntity> result = starService.getStarredMessages(USER_ID);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return paginated starred messages")
        void getStarredMessages_Paginated() {
            List<StarredMessageEntity> starredMessages = Arrays.asList(
                StarredMessageEntity.builder().id(1L).userId(USER_ID).messageId(101L).build()
            );
            Page<StarredMessageEntity> page = new PageImpl<>(starredMessages);

            when(starredMessageRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(page);

            Page<StarredMessageEntity> result = starService.getStarredMessages(USER_ID, 0, 20);

            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("should limit page size to 50")
        void getStarredMessages_LimitPageSize() {
            when(starredMessageRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any(PageRequest.class)))
                .thenReturn(Page.empty());

            starService.getStarredMessages(USER_ID, 0, 100);

            verify(starredMessageRepository).findByUserIdOrderByCreatedAtDesc(
                eq(USER_ID), argThat(pageable -> pageable.getPageSize() == 50));
        }
    }

    @Nested
    @DisplayName("getStarredMessagesInRoom tests")
    class GetStarredMessagesInRoomTests {

        @Test
        @DisplayName("should return starred messages in room")
        void getStarredMessagesInRoom_Success() {
            List<StarredMessageEntity> starredMessages = Arrays.asList(
                StarredMessageEntity.builder().id(1L).userId(USER_ID).roomId(ROOM_ID).build()
            );

            when(starredMessageRepository.findByUserIdAndRoomIdOrderByCreatedAtDesc(USER_ID, ROOM_ID))
                .thenReturn(starredMessages);

            List<StarredMessageEntity> result = starService.getStarredMessagesInRoom(USER_ID, ROOM_ID);

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("isStarred tests")
    class IsStarredTests {

        @Test
        @DisplayName("should return true when message is starred")
        void isStarred_True() {
            when(starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(true);

            assertTrue(starService.isStarred(USER_ID, MESSAGE_ID, MESSAGE_TYPE));
        }

        @Test
        @DisplayName("should return false when message is not starred")
        void isStarred_False() {
            when(starredMessageRepository.existsByUserIdAndMessageIdAndMessageType(
                USER_ID, MESSAGE_ID, MESSAGE_TYPE)).thenReturn(false);

            assertFalse(starService.isStarred(USER_ID, MESSAGE_ID, MESSAGE_TYPE));
        }
    }

    @Nested
    @DisplayName("getStarredCount tests")
    class GetStarredCountTests {

        @Test
        @DisplayName("should return starred count for user")
        void getStarredCount_Success() {
            when(starredMessageRepository.countByUserId(USER_ID)).thenReturn(25);

            Integer count = starService.getStarredCount(USER_ID);

            assertEquals(25, count);
        }
    }

    @Nested
    @DisplayName("getMessageStarCount tests")
    class GetMessageStarCountTests {

        @Test
        @DisplayName("should return star count for message")
        void getMessageStarCount_Success() {
            when(starredMessageRepository.countStars(MESSAGE_ID, MESSAGE_TYPE)).thenReturn(15);

            Integer count = starService.getMessageStarCount(MESSAGE_ID, MESSAGE_TYPE);

            assertEquals(15, count);
        }
    }

    @Nested
    @DisplayName("updateStarNote tests")
    class UpdateStarNoteTests {

        @Test
        @DisplayName("should update star note successfully")
        void updateStarNote_Success() {
            StarredMessageEntity starred = StarredMessageEntity.builder()
                .id(1L)
                .userId(USER_ID)
                .messageId(MESSAGE_ID)
                .note("Old note")
                .build();

            when(starredMessageRepository.findById(1L)).thenReturn(Optional.of(starred));
            when(starredMessageRepository.save(any(StarredMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> starService.updateStarNote(1L, USER_ID, "New note"));

            assertEquals("New note", starred.getNote());
            verify(starredMessageRepository).save(starred);
        }

        @Test
        @DisplayName("should throw exception when starred message not found")
        void updateStarNote_NotFound() {
            when(starredMessageRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                starService.updateStarNote(1L, USER_ID, "New note"));
        }

        @Test
        @DisplayName("should throw exception when not authorized")
        void updateStarNote_NotAuthorized() {
            StarredMessageEntity starred = StarredMessageEntity.builder()
                .id(1L)
                .userId(999L) // Different user
                .messageId(MESSAGE_ID)
                .build();

            when(starredMessageRepository.findById(1L)).thenReturn(Optional.of(starred));

            assertThrows(IllegalStateException.class, () ->
                starService.updateStarNote(1L, USER_ID, "New note"));
        }
    }
}
