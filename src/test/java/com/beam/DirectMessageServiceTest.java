package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectMessageService Unit Tests")
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DirectMessageService directMessageService;

    private UserEntity sender;
    private UserEntity receiver;
    private ConversationEntity conversation;
    private DirectMessageEntity message;

    @BeforeEach
    void setUp() {
        sender = UserEntity.builder()
                .id(1L)
                .username("sender")
                .build();

        receiver = UserEntity.builder()
                .id(2L)
                .username("receiver")
                .build();

        conversation = ConversationEntity.builder()
                .id(1L)
                .conversationId("1_2")
                .user1Id(1L)
                .user2Id(2L)
                .build();

        message = DirectMessageEntity.builder()
                .id(1L)
                .conversationId("1_2")
                .senderId(1L)
                .receiverId(2L)
                .content("Hello")
                .isRead(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message successfully with existing conversation")
        void shouldSendMessageWithExistingConversation() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(conversationRepository.findByConversationId(anyString()))
                    .thenReturn(Optional.of(conversation));
            when(directMessageRepository.save(any(DirectMessageEntity.class))).thenReturn(message);
            when(conversationRepository.save(any(ConversationEntity.class))).thenReturn(conversation);

            // When
            DirectMessageEntity result = directMessageService.sendMessage(1L, 2L, "Hello");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello");
            verify(conversationRepository).save(any(ConversationEntity.class));
        }

        @Test
        @DisplayName("Should create new conversation when not exists")
        void shouldCreateNewConversation() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
            when(conversationRepository.findByConversationId(anyString())).thenReturn(Optional.empty());
            when(conversationRepository.save(any(ConversationEntity.class))).thenReturn(conversation);
            when(directMessageRepository.save(any(DirectMessageEntity.class))).thenReturn(message);

            // When
            DirectMessageEntity result = directMessageService.sendMessage(1L, 2L, "Hello");

            // Then
            assertThat(result).isNotNull();
            verify(conversationRepository, times(2)).save(any(ConversationEntity.class));
        }

        @Test
        @DisplayName("Should fail when sender not found")
        void shouldFailWhenSenderNotFound() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> directMessageService.sendMessage(1L, 2L, "Hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Sender not found");
        }

        @Test
        @DisplayName("Should fail when receiver not found")
        void shouldFailWhenReceiverNotFound() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> directMessageService.sendMessage(1L, 2L, "Hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Receiver not found");
        }
    }

    @Nested
    @DisplayName("Get Conversation Messages Tests")
    class GetConversationMessagesTests {

        @Test
        @DisplayName("Should get messages and mark as read")
        void shouldGetMessagesAndMarkAsRead() {
            // Given
            List<DirectMessageEntity> messages = List.of(message);
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc("1_2"))
                    .thenReturn(messages);
            when(directMessageRepository.findUnreadMessages("1_2", 2L))
                    .thenReturn(List.of(message));
            when(conversationRepository.findByConversationId("1_2"))
                    .thenReturn(Optional.of(conversation));

            // When
            List<DirectMessageEntity> result = directMessageService.getConversationMessages("1_2", 2L);

            // Then
            assertThat(result).hasSize(1);
            verify(directMessageRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should return messages without marking when no unread")
        void shouldReturnMessagesWithoutMarking() {
            // Given
            message.setIsRead(true);
            List<DirectMessageEntity> messages = List.of(message);
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc("1_2"))
                    .thenReturn(messages);
            when(directMessageRepository.findUnreadMessages("1_2", 2L))
                    .thenReturn(List.of());

            // When
            List<DirectMessageEntity> result = directMessageService.getConversationMessages("1_2", 2L);

            // Then
            assertThat(result).hasSize(1);
            verify(directMessageRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Mark Messages As Read Tests")
    class MarkMessagesAsReadTests {

        @Test
        @DisplayName("Should mark messages as read successfully")
        void shouldMarkMessagesAsRead() {
            // Given
            List<DirectMessageEntity> unreadMessages = List.of(message);
            when(directMessageRepository.findUnreadMessages("1_2", 2L))
                    .thenReturn(unreadMessages);
            when(conversationRepository.findByConversationId("1_2"))
                    .thenReturn(Optional.of(conversation));

            // When
            directMessageService.markMessagesAsRead("1_2", 2L);

            // Then
            verify(directMessageRepository).saveAll(argThat(list -> {
                List<DirectMessageEntity> msgs = (List<DirectMessageEntity>) list;
                return msgs.get(0).getIsRead();
            }));
            verify(conversationRepository).save(any(ConversationEntity.class));
        }

        @Test
        @DisplayName("Should do nothing when no unread messages")
        void shouldDoNothingWhenNoUnread() {
            // Given
            when(directMessageRepository.findUnreadMessages("1_2", 2L))
                    .thenReturn(List.of());

            // When
            directMessageService.markMessagesAsRead("1_2", 2L);

            // Then
            verify(directMessageRepository, never()).saveAll(anyList());
            verify(conversationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get User Conversations Tests")
    class GetUserConversationsTests {

        @Test
        @DisplayName("Should get user conversations successfully")
        void shouldGetUserConversationsSuccessfully() {
            // Given
            when(conversationRepository.findUserConversations(1L))
                    .thenReturn(List.of(conversation));

            // When
            List<ConversationEntity> result = directMessageService.getUserConversations(1L);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Unread Count Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should get unread count successfully")
        void shouldGetUnreadCountSuccessfully() {
            // Given
            when(directMessageRepository.countUnreadMessages("1_2", 2L)).thenReturn(5);

            // When
            Integer result = directMessageService.getUnreadCount("1_2", 2L);

            // Then
            assertThat(result).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Get Or Create Conversation Tests")
    class GetOrCreateConversationTests {

        @Test
        @DisplayName("Should return existing conversation")
        void shouldReturnExistingConversation() {
            // Given
            when(conversationRepository.findByUsers(1L, 2L))
                    .thenReturn(Optional.of(conversation));

            // When
            ConversationEntity result = directMessageService.getOrCreateConversation(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getConversationId()).isEqualTo("1_2");
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new conversation when not exists")
        void shouldCreateNewConversationWhenNotExists() {
            // Given
            when(conversationRepository.findByUsers(1L, 2L)).thenReturn(Optional.empty());
            when(conversationRepository.save(any(ConversationEntity.class))).thenReturn(conversation);

            // When
            ConversationEntity result = directMessageService.getOrCreateConversation(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            verify(conversationRepository).save(any(ConversationEntity.class));
        }
    }
}
