package com.beam;

import com.beam.PinnedMessageEntity.ContextType;
import com.beam.PinnedMessageEntity.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PinService Unit Tests")
class PinServiceTest {

    @Mock
    private PinnedMessageRepository pinnedMessageRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PinService pinService;

    private static final Long MESSAGE_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String CONTEXT_ID = "room-123";
    private static final MessageType MESSAGE_TYPE = MessageType.ROOM;
    private static final ContextType CONTEXT_TYPE = ContextType.ROOM;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pinService, "maxPinsPerContext", 50);
    }

    @Nested
    @DisplayName("pinMessage tests")
    class PinMessageTests {

        @Test
        @DisplayName("should pin message successfully")
        void pinMessage_Success() {
            when(pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID)).thenReturn(false);
            when(pinnedMessageRepository.countByContext(CONTEXT_TYPE, CONTEXT_ID)).thenReturn(5);
            when(pinnedMessageRepository.getMaxPinOrder(CONTEXT_TYPE, CONTEXT_ID)).thenReturn(5);
            when(pinnedMessageRepository.save(any(PinnedMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            PinnedMessageEntity result = pinService.pinMessage(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID, USER_ID, "Important");

            assertNotNull(result);
            assertEquals(MESSAGE_ID, result.getMessageId());
            assertEquals(MESSAGE_TYPE, result.getMessageType());
            assertEquals(CONTEXT_TYPE, result.getContextType());
            assertEquals(CONTEXT_ID, result.getContextId());
            assertEquals(USER_ID, result.getPinnedByUserId());
            assertEquals(6, result.getPinOrder());
            assertEquals("Important", result.getNote());
            verify(pinnedMessageRepository).save(any(PinnedMessageEntity.class));
        }

        @Test
        @DisplayName("should throw exception when message already pinned")
        void pinMessage_AlreadyPinned() {
            when(pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID)).thenReturn(true);

            assertThrows(IllegalStateException.class, () ->
                pinService.pinMessage(MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID, USER_ID, null));

            verify(pinnedMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when max pins reached")
        void pinMessage_MaxPinsReached() {
            when(pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID)).thenReturn(false);
            when(pinnedMessageRepository.countByContext(CONTEXT_TYPE, CONTEXT_ID)).thenReturn(50);

            assertThrows(IllegalStateException.class, () ->
                pinService.pinMessage(MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID, USER_ID, null));

            verify(pinnedMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unpinMessage tests")
    class UnpinMessageTests {

        @Test
        @DisplayName("should unpin message successfully")
        void unpinMessage_Success() {
            doNothing().when(pinnedMessageRepository)
                .deleteByMessageIdAndMessageTypeAndContextTypeAndContextId(
                    MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID);

            assertDoesNotThrow(() ->
                pinService.unpinMessage(MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID));

            verify(pinnedMessageRepository).deleteByMessageIdAndMessageTypeAndContextTypeAndContextId(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID);
        }
    }

    @Nested
    @DisplayName("getPinnedMessages tests")
    class GetPinnedMessagesTests {

        @Test
        @DisplayName("should return pinned messages ordered by pin order")
        void getPinnedMessages_Success() {
            List<PinnedMessageEntity> pinnedMessages = Arrays.asList(
                PinnedMessageEntity.builder().id(1L).messageId(101L).pinOrder(1).build(),
                PinnedMessageEntity.builder().id(2L).messageId(102L).pinOrder(2).build()
            );

            when(pinnedMessageRepository.findByContextTypeAndContextIdOrderByPinOrderAscCreatedAtDesc(
                CONTEXT_TYPE, CONTEXT_ID)).thenReturn(pinnedMessages);

            List<PinnedMessageEntity> result = pinService.getPinnedMessages(CONTEXT_TYPE, CONTEXT_ID);

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getPinOrder());
            assertEquals(2, result.get(1).getPinOrder());
        }

        @Test
        @DisplayName("should return empty list when no pinned messages")
        void getPinnedMessages_Empty() {
            when(pinnedMessageRepository.findByContextTypeAndContextIdOrderByPinOrderAscCreatedAtDesc(
                CONTEXT_TYPE, CONTEXT_ID)).thenReturn(Collections.emptyList());

            List<PinnedMessageEntity> result = pinService.getPinnedMessages(CONTEXT_TYPE, CONTEXT_ID);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("isPinned tests")
    class IsPinnedTests {

        @Test
        @DisplayName("should return true when message is pinned")
        void isPinned_True() {
            when(pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID)).thenReturn(true);

            assertTrue(pinService.isPinned(MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID));
        }

        @Test
        @DisplayName("should return false when message is not pinned")
        void isPinned_False() {
            when(pinnedMessageRepository.existsByMessageIdAndMessageTypeAndContextTypeAndContextId(
                MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID)).thenReturn(false);

            assertFalse(pinService.isPinned(MESSAGE_ID, MESSAGE_TYPE, CONTEXT_TYPE, CONTEXT_ID));
        }
    }

    @Nested
    @DisplayName("getPinnedCount tests")
    class GetPinnedCountTests {

        @Test
        @DisplayName("should return pinned count")
        void getPinnedCount_Success() {
            when(pinnedMessageRepository.countByContext(CONTEXT_TYPE, CONTEXT_ID)).thenReturn(10);

            Integer count = pinService.getPinnedCount(CONTEXT_TYPE, CONTEXT_ID);

            assertEquals(10, count);
        }
    }

    @Nested
    @DisplayName("updatePinOrder tests")
    class UpdatePinOrderTests {

        @Test
        @DisplayName("should update pin order successfully")
        void updatePinOrder_Success() {
            PinnedMessageEntity pinnedMessage = PinnedMessageEntity.builder()
                .id(1L)
                .messageId(MESSAGE_ID)
                .pinOrder(1)
                .build();

            when(pinnedMessageRepository.findById(1L)).thenReturn(Optional.of(pinnedMessage));
            when(pinnedMessageRepository.save(any(PinnedMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> pinService.updatePinOrder(1L, 5, USER_ID));

            assertEquals(5, pinnedMessage.getPinOrder());
            verify(pinnedMessageRepository).save(pinnedMessage);
        }

        @Test
        @DisplayName("should throw exception when pinned message not found")
        void updatePinOrder_NotFound() {
            when(pinnedMessageRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                pinService.updatePinOrder(1L, 5, USER_ID));
        }
    }

    @Nested
    @DisplayName("updatePinNote tests")
    class UpdatePinNoteTests {

        @Test
        @DisplayName("should update pin note successfully")
        void updatePinNote_Success() {
            PinnedMessageEntity pinnedMessage = PinnedMessageEntity.builder()
                .id(1L)
                .messageId(MESSAGE_ID)
                .note("Old note")
                .build();

            when(pinnedMessageRepository.findById(1L)).thenReturn(Optional.of(pinnedMessage));
            when(pinnedMessageRepository.save(any(PinnedMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> pinService.updatePinNote(1L, "New note", USER_ID));

            assertEquals("New note", pinnedMessage.getNote());
            verify(pinnedMessageRepository).save(pinnedMessage);
        }
    }
}
