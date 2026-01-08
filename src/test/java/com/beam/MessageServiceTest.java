package com.beam;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Unit Tests")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReadReceiptRepository readReceiptRepository;

    @InjectMocks
    private MessageService messageService;

    private static final String ROOM_ID = "room-123";
    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;
    private static final String USERNAME = "testuser";

    @Nested
    @DisplayName("saveMessage Tests")
    class SaveMessageTests {

        @Test
        @DisplayName("Should save message with all fields")
        void shouldSaveMessageWithAllFields() {
            // Given
            ChatMessage chatMessage = new ChatMessage("testuser", "Hello World", "10:00:00", "message");
            chatMessage.setRoomId(ROOM_ID);
            chatMessage.setSecurityType(MessageSecurityType.NORMAL);

            MessageEntity savedEntity = new MessageEntity("testuser", "Hello World", ROOM_ID, "message");
            when(messageRepository.save(any(MessageEntity.class))).thenReturn(savedEntity);

            // When
            MessageEntity result = messageService.saveMessage(chatMessage);

            // Then
            assertNotNull(result);
            assertEquals("testuser", result.getSender());
            assertEquals("Hello World", result.getContent());
            verify(messageRepository).save(any(MessageEntity.class));
        }

        @Test
        @DisplayName("Should default message type to 'message' when null")
        void shouldDefaultMessageTypeWhenNull() {
            // Given
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSender("testuser");
            chatMessage.setContent("Hello");
            chatMessage.setRoomId(ROOM_ID);
            chatMessage.setType(null);

            when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
                MessageEntity entity = invocation.getArgument(0);
                assertEquals("message", entity.getMessageType());
                return entity;
            });

            // When
            messageService.saveMessage(chatMessage);

            // Then
            verify(messageRepository).save(any(MessageEntity.class));
        }
    }

    @Nested
    @DisplayName("getRecentMessages Tests")
    class GetRecentMessagesTests {

        @Test
        @DisplayName("Should get recent messages for room")
        void shouldGetRecentMessagesForRoom() {
            // Given
            List<MessageEntity> messages = createTestMessages(10);
            when(messageRepository.findTop50ByRoomIdOrderByTimestampDesc(ROOM_ID)).thenReturn(messages);

            // When
            List<MessageEntity> result = messageService.getRecentMessages(ROOM_ID);

            // Then
            assertEquals(10, result.size());
            verify(messageRepository).findTop50ByRoomIdOrderByTimestampDesc(ROOM_ID);
        }

        @Test
        @DisplayName("Should return empty list when no messages")
        void shouldReturnEmptyListWhenNoMessages() {
            // Given
            when(messageRepository.findTop50ByRoomIdOrderByTimestampDesc(ROOM_ID)).thenReturn(new ArrayList<>());

            // When
            List<MessageEntity> result = messageService.getRecentMessages(ROOM_ID);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllRoomMessages Tests")
    class GetAllRoomMessagesTests {

        @Test
        @DisplayName("Should get all messages without pagination (deprecated)")
        void shouldGetAllMessagesWithoutPagination() {
            // Given
            List<MessageEntity> messages = createTestMessages(100);
            when(messageRepository.findByRoomIdOrderByTimestampAsc(ROOM_ID)).thenReturn(messages);

            // When
            @SuppressWarnings("deprecation")
            List<MessageEntity> result = messageService.getAllRoomMessages(ROOM_ID);

            // Then
            assertEquals(100, result.size());
        }

        @Test
        @DisplayName("Should get messages with pagination")
        void shouldGetMessagesWithPagination() {
            // Given
            List<MessageEntity> messages = createTestMessages(50);
            Page<MessageEntity> page = new PageImpl<>(messages);
            when(messageRepository.findByRoomIdOrderByTimestampAsc(eq(ROOM_ID), any(PageRequest.class))).thenReturn(page);

            // When
            Page<MessageEntity> result = messageService.getAllRoomMessages(ROOM_ID, 0, 50);

            // Then
            assertEquals(50, result.getContent().size());
        }

        @Test
        @DisplayName("Should limit page size to MAX_PAGE_SIZE")
        void shouldLimitPageSizeToMax() {
            // Given
            List<MessageEntity> messages = createTestMessages(10);
            Page<MessageEntity> page = new PageImpl<>(messages);
            when(messageRepository.findByRoomIdOrderByTimestampAsc(eq(ROOM_ID), any(PageRequest.class))).thenReturn(page);

            // When
            messageService.getAllRoomMessages(ROOM_ID, 0, 1000);

            // Then
            verify(messageRepository).findByRoomIdOrderByTimestampAsc(eq(ROOM_ID), argThat(pageable ->
                pageable.getPageSize() <= 500
            ));
        }
    }

    @Nested
    @DisplayName("markMessageAsRead Tests")
    class MarkMessageAsReadTests {

        @Test
        @DisplayName("Should mark message as read when not already read")
        void shouldMarkMessageAsReadWhenNotAlreadyRead() {
            // Given
            when(readReceiptRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());
            MessageReadReceipt newReceipt = new MessageReadReceipt(MESSAGE_ID, USER_ID);
            when(readReceiptRepository.save(any(MessageReadReceipt.class))).thenReturn(newReceipt);

            // When
            MessageReadReceipt result = messageService.markMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            assertNotNull(result);
            assertEquals(MESSAGE_ID, result.getMessageId());
            assertEquals(USER_ID, result.getUserId());
            verify(readReceiptRepository).save(any(MessageReadReceipt.class));
        }

        @Test
        @DisplayName("Should return existing receipt when already read")
        void shouldReturnExistingReceiptWhenAlreadyRead() {
            // Given
            MessageReadReceipt existingReceipt = new MessageReadReceipt(MESSAGE_ID, USER_ID);
            when(readReceiptRepository.findByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(Optional.of(existingReceipt));

            // When
            MessageReadReceipt result = messageService.markMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            assertNotNull(result);
            verify(readReceiptRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markRoomMessagesAsRead Tests")
    class MarkRoomMessagesAsReadTests {

        @Test
        @DisplayName("Should mark all unread messages as read")
        void shouldMarkAllUnreadMessagesAsRead() {
            // Given
            List<Long> unreadMessageIds = List.of(1L, 2L, 3L);
            when(messageRepository.findUnreadMessageIds(ROOM_ID, USER_ID, USERNAME)).thenReturn(unreadMessageIds);

            // When
            messageService.markRoomMessagesAsRead(ROOM_ID, USER_ID, USERNAME);

            // Then
            verify(readReceiptRepository).saveAll(argThat(receipts -> {
                List<MessageReadReceipt> list = new ArrayList<>();
                receipts.forEach(list::add);
                return list.size() == 3;
            }));
        }

        @Test
        @DisplayName("Should do nothing when no unread messages")
        void shouldDoNothingWhenNoUnreadMessages() {
            // Given
            when(messageRepository.findUnreadMessageIds(ROOM_ID, USER_ID, USERNAME)).thenReturn(new ArrayList<>());

            // When
            messageService.markRoomMessagesAsRead(ROOM_ID, USER_ID, USERNAME);

            // Then
            verify(readReceiptRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("getUnreadMessageCount Tests")
    class GetUnreadMessageCountTests {

        @Test
        @DisplayName("Should return unread message count")
        void shouldReturnUnreadMessageCount() {
            // Given
            when(readReceiptRepository.countUnreadMessagesInRoom(ROOM_ID, USER_ID, USERNAME)).thenReturn(5L);

            // When
            long result = messageService.getUnreadMessageCount(ROOM_ID, USER_ID, USERNAME);

            // Then
            assertEquals(5L, result);
        }
    }

    @Nested
    @DisplayName("getReadCount Tests")
    class GetReadCountTests {

        @Test
        @DisplayName("Should return read count for message")
        void shouldReturnReadCountForMessage() {
            // Given
            when(readReceiptRepository.countByMessageId(MESSAGE_ID)).thenReturn(10L);

            // When
            long result = messageService.getReadCount(MESSAGE_ID);

            // Then
            assertEquals(10L, result);
        }
    }

    @Nested
    @DisplayName("isMessageRead Tests")
    class IsMessageReadTests {

        @Test
        @DisplayName("Should return true when message is read")
        void shouldReturnTrueWhenMessageIsRead() {
            // Given
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(true);

            // When
            boolean result = messageService.isMessageRead(MESSAGE_ID, USER_ID);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when message is not read")
        void shouldReturnFalseWhenMessageIsNotRead() {
            // Given
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);

            // When
            boolean result = messageService.isMessageRead(MESSAGE_ID, USER_ID);

            // Then
            assertFalse(result);
        }
    }

    // Helper method
    private List<MessageEntity> createTestMessages(int count) {
        List<MessageEntity> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MessageEntity msg = new MessageEntity("user" + i, "Content " + i, ROOM_ID, "message");
            msg.setId((long) i);
            msg.setTimestamp(LocalDateTime.now().minusMinutes(i));
            messages.add(msg);
        }
        return messages;
    }
}
