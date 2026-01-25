package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.beam.exception.MessageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadReceiptService Unit Tests")
class ReadReceiptServiceTest {

    @Mock
    private ReadReceiptRepository readReceiptRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private GroupMessageRepository groupMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    private ReadReceiptService readReceiptService;

    private static final Long MESSAGE_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long SENDER_ID = 2L;
    private static final Long ROOM_ID = 10L;

    @BeforeEach
    void setUp() {
        readReceiptService = new ReadReceiptService(
            readReceiptRepository,
            directMessageRepository,
            groupMessageRepository,
            userRepository
        );
        ReflectionTestUtils.setField(readReceiptService, "messagingTemplate", messagingTemplate);
    }

    @Nested
    @DisplayName("markDirectMessageAsRead Tests")
    class MarkDirectMessageAsReadTests {

        @Test
        @DisplayName("Should mark direct message as read")
        void shouldMarkDirectMessageAsRead() {
            // Given
            DirectMessageEntity message = createDirectMessage(MESSAGE_ID, SENDER_ID, USER_ID);
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);
            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(readReceiptRepository.save(any(ReadReceiptEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(directMessageRepository.save(any(DirectMessageEntity.class))).thenReturn(message);

            // When
            readReceiptService.markDirectMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            verify(readReceiptRepository).save(any(ReadReceiptEntity.class));
            verify(directMessageRepository).save(any(DirectMessageEntity.class));
            assertTrue(message.getIsRead());
        }

        @Test
        @DisplayName("Should skip if already read")
        void shouldSkipIfAlreadyRead() {
            // Given
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(true);

            // When
            readReceiptService.markDirectMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            verify(directMessageRepository, never()).findById(any());
            verify(readReceiptRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when message not found")
        void shouldThrowExceptionWhenMessageNotFound() {
            // Given
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);
            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> readReceiptService.markDirectMessageAsRead(MESSAGE_ID, USER_ID))
                    .isInstanceOf(MessageException.class);
        }

        @Test
        @DisplayName("Should throw exception when not authorized")
        void shouldThrowExceptionWhenNotAuthorized() {
            // Given
            Long anotherUserId = 999L;
            DirectMessageEntity message = createDirectMessage(MESSAGE_ID, SENDER_ID, USER_ID);
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, anotherUserId)).thenReturn(false);
            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            // When & Then
            assertThatThrownBy(() -> readReceiptService.markDirectMessageAsRead(MESSAGE_ID, anotherUserId))
                    .isInstanceOf(MessageException.class);
        }

        @Test
        @DisplayName("Should send notification when messaging template is available")
        void shouldSendNotificationWhenMessagingTemplateAvailable() {
            // Given
            DirectMessageEntity message = createDirectMessage(MESSAGE_ID, SENDER_ID, USER_ID);
            when(readReceiptRepository.existsByMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);
            when(directMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(readReceiptRepository.save(any(ReadReceiptEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(directMessageRepository.save(any(DirectMessageEntity.class))).thenReturn(message);

            // When
            readReceiptService.markDirectMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                eq(SENDER_ID.toString()),
                eq("/queue/read-receipts"),
                any(Map.class)
            );
        }
    }

    @Nested
    @DisplayName("markGroupMessageAsRead Tests")
    class MarkGroupMessageAsReadTests {

        @Test
        @DisplayName("Should mark group message as read")
        void shouldMarkGroupMessageAsRead() {
            // Given
            GroupMessageEntity message = createGroupMessage(MESSAGE_ID, SENDER_ID, ROOM_ID);
            when(readReceiptRepository.existsByGroupMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);
            when(groupMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(readReceiptRepository.save(any(ReadReceiptEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(groupMessageRepository.save(any(GroupMessageEntity.class))).thenReturn(message);

            // When
            readReceiptService.markGroupMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            verify(readReceiptRepository).save(any(ReadReceiptEntity.class));
            verify(groupMessageRepository).save(any(GroupMessageEntity.class));
        }

        @Test
        @DisplayName("Should skip if already read")
        void shouldSkipIfAlreadyRead() {
            // Given
            when(readReceiptRepository.existsByGroupMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(true);

            // When
            readReceiptService.markGroupMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            verify(groupMessageRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should skip if user is the sender")
        void shouldSkipIfUserIsSender() {
            // Given
            GroupMessageEntity message = createGroupMessage(MESSAGE_ID, USER_ID, ROOM_ID);
            when(readReceiptRepository.existsByGroupMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);
            when(groupMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));

            // When
            readReceiptService.markGroupMessageAsRead(MESSAGE_ID, USER_ID);

            // Then
            verify(readReceiptRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when message not found")
        void shouldThrowExceptionWhenMessageNotFound() {
            // Given
            when(readReceiptRepository.existsByGroupMessageIdAndUserId(MESSAGE_ID, USER_ID)).thenReturn(false);
            when(groupMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> readReceiptService.markGroupMessageAsRead(MESSAGE_ID, USER_ID))
                    .isInstanceOf(MessageException.class);
        }
    }

    @Nested
    @DisplayName("getMessageReadReceipts Tests")
    class GetMessageReadReceiptsTests {

        @Test
        @DisplayName("Should get direct message read receipts")
        void shouldGetDirectMessageReadReceipts() {
            // Given
            ReadReceiptEntity receipt = createReadReceipt(MESSAGE_ID, USER_ID);
            UserEntity user = createUser(USER_ID, "Test User");

            when(readReceiptRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(receipt));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            List<Map<String, Object>> result = readReceiptService.getMessageReadReceipts(MESSAGE_ID, false);

            // Then
            assertEquals(1, result.size());
            assertEquals(USER_ID, result.get(0).get("userId"));
            assertEquals("Test User", result.get(0).get("userName"));
        }

        @Test
        @DisplayName("Should get group message read receipts")
        void shouldGetGroupMessageReadReceipts() {
            // Given
            ReadReceiptEntity receipt = createReadReceipt(MESSAGE_ID, USER_ID);
            receipt.setGroupMessageId(MESSAGE_ID);
            UserEntity user = createUser(USER_ID, "Test User");

            when(readReceiptRepository.findByGroupMessageId(MESSAGE_ID)).thenReturn(List.of(receipt));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            List<Map<String, Object>> result = readReceiptService.getMessageReadReceipts(MESSAGE_ID, true);

            // Then
            assertEquals(1, result.size());
            verify(readReceiptRepository).findByGroupMessageId(MESSAGE_ID);
        }

        @Test
        @DisplayName("Should return 'Unknown' when user not found")
        void shouldReturnUnknownWhenUserNotFound() {
            // Given
            ReadReceiptEntity receipt = createReadReceipt(MESSAGE_ID, USER_ID);

            when(readReceiptRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(receipt));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // When
            List<Map<String, Object>> result = readReceiptService.getMessageReadReceipts(MESSAGE_ID, false);

            // Then
            assertEquals("Unknown", result.get(0).get("userName"));
        }

        @Test
        @DisplayName("Should return empty list when no receipts")
        void shouldReturnEmptyListWhenNoReceipts() {
            // Given
            when(readReceiptRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of());

            // When
            List<Map<String, Object>> result = readReceiptService.getMessageReadReceipts(MESSAGE_ID, false);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    // Helper methods
    private DirectMessageEntity createDirectMessage(Long id, Long senderId, Long receiverId) {
        DirectMessageEntity message = DirectMessageEntity.builder()
                .conversationId("conv-123")
                .senderId(senderId)
                .receiverId(receiverId)
                .content("Test message")
                .messageType(DirectMessageEntity.MessageType.TEXT)
                .isRead(false)
                .build();
        message.setId(id);
        return message;
    }

    private GroupMessageEntity createGroupMessage(Long id, Long senderId, Long roomId) {
        GroupMessageEntity message = GroupMessageEntity.builder()
                .roomId(roomId)
                .senderId(senderId)
                .content("Test group message")
                .messageType(GroupMessageEntity.MessageType.TEXT)
                .readCount(0)
                .isDeleted(false)
                .build();
        message.setId(id);
        return message;
    }

    private ReadReceiptEntity createReadReceipt(Long messageId, Long userId) {
        return ReadReceiptEntity.builder()
                .messageId(messageId)
                .userId(userId)
                .readAt(LocalDateTime.now())
                .build();
    }

    private UserEntity createUser(Long id, String displayName) {
        UserEntity user = UserEntity.builder()
                .username("user" + id)
                .password("password")
                .email("user" + id + "@example.com")
                .phoneNumber("0101234567" + id)
                .displayName(displayName)
                .isActive(true)
                .build();
        user.setId(id);
        return user;
    }
}
