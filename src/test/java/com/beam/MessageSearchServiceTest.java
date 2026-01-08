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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageSearchService Unit Tests")
class MessageSearchServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private GroupMessageRepository groupMessageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageSearchService messageSearchService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ROOM_ID = 10L;
    private static final String CONVERSATION_ID = "1_2";
    private static final String KEYWORD = "hello";

    @Nested
    @DisplayName("searchDirectMessages Tests")
    class SearchDirectMessagesTests {

        @Test
        @DisplayName("Should find direct messages containing keyword")
        void shouldFindDirectMessagesContainingKeyword() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity message = createDirectMessage(1L, USER_ID, OTHER_USER_ID, "hello world");
            UserEntity otherUser = createUser(OTHER_USER_ID, "Other User");

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(message));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            // When
            List<Map<String, Object>> result = messageSearchService.searchDirectMessages(USER_ID, KEYWORD);

            // Then
            assertEquals(1, result.size());
            assertEquals("DM", result.get(0).get("type"));
            assertEquals("hello world", result.get(0).get("content"));
            assertEquals("Other User", result.get(0).get("otherUserName"));
        }

        @Test
        @DisplayName("Should return empty when no matching messages")
        void shouldReturnEmptyWhenNoMatchingMessages() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity message = createDirectMessage(1L, USER_ID, OTHER_USER_ID, "goodbye world");

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(message));

            // When
            List<Map<String, Object>> result = messageSearchService.searchDirectMessages(USER_ID, KEYWORD);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should be case insensitive search")
        void shouldBeCaseInsensitiveSearch() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity message = createDirectMessage(1L, USER_ID, OTHER_USER_ID, "HELLO WORLD");
            UserEntity otherUser = createUser(OTHER_USER_ID, "Other User");

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(message));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            // When
            List<Map<String, Object>> result = messageSearchService.searchDirectMessages(USER_ID, KEYWORD);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should mark isMine correctly for sent messages")
        void shouldMarkIsMineCorrectlyForSentMessages() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity message = createDirectMessage(1L, USER_ID, OTHER_USER_ID, "hello");
            UserEntity otherUser = createUser(OTHER_USER_ID, "Other User");

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(message));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            // When
            List<Map<String, Object>> result = messageSearchService.searchDirectMessages(USER_ID, KEYWORD);

            // Then
            assertTrue((Boolean) result.get(0).get("isMine"));
        }

        @Test
        @DisplayName("Should mark isMine correctly for received messages")
        void shouldMarkIsMineCorrectlyForReceivedMessages() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity message = createDirectMessage(1L, OTHER_USER_ID, USER_ID, "hello");
            UserEntity otherUser = createUser(OTHER_USER_ID, "Other User");

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(message));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            // When
            List<Map<String, Object>> result = messageSearchService.searchDirectMessages(USER_ID, KEYWORD);

            // Then
            assertFalse((Boolean) result.get(0).get("isMine"));
        }

        @Test
        @DisplayName("Should return empty when no conversations")
        void shouldReturnEmptyWhenNoConversations() {
            // Given
            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(new ArrayList<>());

            // When
            List<Map<String, Object>> result = messageSearchService.searchDirectMessages(USER_ID, KEYWORD);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("searchRoomMessages Tests")
    class SearchRoomMessagesTests {

        @Test
        @DisplayName("Should find room messages containing keyword")
        void shouldFindRoomMessagesContainingKeyword() {
            // Given
            RoomMemberEntity membership = createRoomMembership(USER_ID, ROOM_ID);
            GroupMessageEntity message = createGroupMessage(1L, OTHER_USER_ID, ROOM_ID, "hello room");
            UserEntity sender = createUser(OTHER_USER_ID, "Sender");

            when(roomMemberRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of(membership));
            when(groupMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampAsc(ROOM_ID))
                    .thenReturn(List.of(message));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(sender));

            // When
            List<Map<String, Object>> result = messageSearchService.searchRoomMessages(USER_ID, KEYWORD);

            // Then
            assertEquals(1, result.size());
            assertEquals("ROOM", result.get(0).get("type"));
            assertEquals("hello room", result.get(0).get("content"));
            assertEquals("Sender", result.get(0).get("senderName"));
        }

        @Test
        @DisplayName("Should return empty when no matching room messages")
        void shouldReturnEmptyWhenNoMatchingRoomMessages() {
            // Given
            RoomMemberEntity membership = createRoomMembership(USER_ID, ROOM_ID);
            GroupMessageEntity message = createGroupMessage(1L, OTHER_USER_ID, ROOM_ID, "goodbye room");

            when(roomMemberRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of(membership));
            when(groupMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampAsc(ROOM_ID))
                    .thenReturn(List.of(message));

            // When
            List<Map<String, Object>> result = messageSearchService.searchRoomMessages(USER_ID, KEYWORD);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when no room memberships")
        void shouldReturnEmptyWhenNoRoomMemberships() {
            // Given
            when(roomMemberRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(new ArrayList<>());

            // When
            List<Map<String, Object>> result = messageSearchService.searchRoomMessages(USER_ID, KEYWORD);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("searchAllMessages Tests")
    class SearchAllMessagesTests {

        @Test
        @DisplayName("Should combine DM and room results")
        void shouldCombineDMAndRoomResults() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity dmMessage = createDirectMessage(1L, USER_ID, OTHER_USER_ID, "hello dm");
            UserEntity otherUser = createUser(OTHER_USER_ID, "Other User");

            RoomMemberEntity membership = createRoomMembership(USER_ID, ROOM_ID);
            GroupMessageEntity roomMessage = createGroupMessage(2L, OTHER_USER_ID, ROOM_ID, "hello room");

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(dmMessage));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
            when(roomMemberRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of(membership));
            when(groupMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampAsc(ROOM_ID))
                    .thenReturn(List.of(roomMessage));

            // When
            List<Map<String, Object>> result = messageSearchService.searchAllMessages(USER_ID, KEYWORD);

            // Then
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should sort results by timestamp descending")
        void shouldSortResultsByTimestampDescending() {
            // Given
            ConversationEntity conversation = createConversation(CONVERSATION_ID, USER_ID, OTHER_USER_ID);
            DirectMessageEntity dmMessage = createDirectMessage(1L, USER_ID, OTHER_USER_ID, "hello dm");
            dmMessage.setTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0));
            UserEntity otherUser = createUser(OTHER_USER_ID, "Other User");

            RoomMemberEntity membership = createRoomMembership(USER_ID, ROOM_ID);
            GroupMessageEntity roomMessage = createGroupMessage(2L, OTHER_USER_ID, ROOM_ID, "hello room");
            roomMessage.setTimestamp(LocalDateTime.of(2024, 1, 1, 12, 0));

            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(List.of(conversation));
            when(directMessageRepository.findByConversationIdOrderByTimestampAsc(CONVERSATION_ID))
                    .thenReturn(List.of(dmMessage));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
            when(roomMemberRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(List.of(membership));
            when(groupMessageRepository.findByRoomIdAndIsDeletedFalseOrderByTimestampAsc(ROOM_ID))
                    .thenReturn(List.of(roomMessage));

            // When
            List<Map<String, Object>> result = messageSearchService.searchAllMessages(USER_ID, KEYWORD);

            // Then
            assertEquals(2, result.size());
            assertEquals("ROOM", result.get(0).get("type"));
            assertEquals("DM", result.get(1).get("type"));
        }

        @Test
        @DisplayName("Should return empty when no matches in both")
        void shouldReturnEmptyWhenNoMatchesInBoth() {
            // Given
            when(conversationRepository.findUserConversations(USER_ID)).thenReturn(new ArrayList<>());
            when(roomMemberRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(new ArrayList<>());

            // When
            List<Map<String, Object>> result = messageSearchService.searchAllMessages(USER_ID, KEYWORD);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    // Helper methods
    private ConversationEntity createConversation(String conversationId, Long user1Id, Long user2Id) {
        return ConversationEntity.builder()
                .conversationId(conversationId)
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
    }

    private DirectMessageEntity createDirectMessage(Long id, Long senderId, Long receiverId, String content) {
        DirectMessageEntity message = DirectMessageEntity.builder()
                .conversationId(CONVERSATION_ID)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .messageType(DirectMessageEntity.MessageType.TEXT)
                .isRead(false)
                .build();
        message.setId(id);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private GroupMessageEntity createGroupMessage(Long id, Long senderId, Long roomId, String content) {
        GroupMessageEntity message = GroupMessageEntity.builder()
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .messageType(GroupMessageEntity.MessageType.TEXT)
                .readCount(0)
                .isDeleted(false)
                .build();
        message.setId(id);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private RoomMemberEntity createRoomMembership(Long userId, Long roomId) {
        return RoomMemberEntity.builder()
                .userId(userId)
                .roomId(roomId)
                .role(RoomMemberEntity.MemberRole.MEMBER)
                .isActive(true)
                .unreadCount(0)
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
