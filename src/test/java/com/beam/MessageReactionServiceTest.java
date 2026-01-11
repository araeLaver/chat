package com.beam;

import com.beam.MessageReactionEntity.MessageType;
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
@DisplayName("MessageReactionService Unit Tests")
class MessageReactionServiceTest {

    @Mock
    private MessageReactionRepository reactionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @InjectMocks
    private MessageReactionService reactionService;

    private static final Long MESSAGE_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final String EMOJI = "👍";
    private static final MessageType MESSAGE_TYPE = MessageType.ROOM;

    @Nested
    @DisplayName("addReaction tests")
    class AddReactionTests {

        @Test
        @DisplayName("should add reaction successfully when not exists")
        void addReaction_Success() {
            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(false);
            when(reactionRepository.save(any(MessageReactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MessageReactionEntity result = reactionService.addReaction(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI);

            assertNotNull(result);
            assertEquals(MESSAGE_ID, result.getMessageId());
            assertEquals(MESSAGE_TYPE, result.getMessageType());
            assertEquals(USER_ID, result.getUserId());
            assertEquals(EMOJI, result.getEmoji());
            verify(reactionRepository).save(any(MessageReactionEntity.class));
        }

        @Test
        @DisplayName("should throw exception when reaction already exists")
        void addReaction_AlreadyExists() {
            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(true);

            assertThrows(RuntimeException.class, () ->
                reactionService.addReaction(MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI));

            verify(reactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeReaction tests")
    class RemoveReactionTests {

        @Test
        @DisplayName("should remove reaction successfully")
        void removeReaction_Success() {
            doNothing().when(reactionRepository)
                .deleteByMessageIdAndMessageTypeAndUserIdAndEmoji(
                    MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI);

            assertDoesNotThrow(() ->
                reactionService.removeReaction(MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI));

            verify(reactionRepository).deleteByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI);
        }
    }

    @Nested
    @DisplayName("toggleReaction tests")
    class ToggleReactionTests {

        @Test
        @DisplayName("should add reaction when not exists")
        void toggleReaction_Add() {
            when(reactionRepository.findByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(Optional.empty());
            when(messageRepository.existsById(MESSAGE_ID)).thenReturn(true);
            when(reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(false);
            when(reactionRepository.save(any(MessageReactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MessageReactionEntity result = reactionService.toggleReaction(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI);

            assertNotNull(result);
            assertEquals(EMOJI, result.getEmoji());
            verify(reactionRepository).save(any(MessageReactionEntity.class));
        }

        @Test
        @DisplayName("should remove reaction when exists")
        void toggleReaction_Remove() {
            MessageReactionEntity existing = MessageReactionEntity.builder()
                .id(1L)
                .messageId(MESSAGE_ID)
                .messageType(MESSAGE_TYPE)
                .userId(USER_ID)
                .emoji(EMOJI)
                .build();

            when(reactionRepository.findByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(Optional.of(existing));

            MessageReactionEntity result = reactionService.toggleReaction(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI);

            assertNull(result);
            verify(reactionRepository).delete(existing);
        }
    }

    @Nested
    @DisplayName("getReactions tests")
    class GetReactionsTests {

        @Test
        @DisplayName("should return all reactions for a message")
        void getReactions_Success() {
            List<MessageReactionEntity> reactions = Arrays.asList(
                MessageReactionEntity.builder()
                    .id(1L).messageId(MESSAGE_ID).messageType(MESSAGE_TYPE)
                    .userId(1L).emoji("👍").build(),
                MessageReactionEntity.builder()
                    .id(2L).messageId(MESSAGE_ID).messageType(MESSAGE_TYPE)
                    .userId(2L).emoji("❤️").build()
            );

            when(reactionRepository.findByMessageIdAndMessageType(MESSAGE_ID, MESSAGE_TYPE))
                .thenReturn(reactions);

            List<MessageReactionEntity> result = reactionService.getReactions(MESSAGE_ID, MESSAGE_TYPE);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should return empty list when no reactions")
        void getReactions_Empty() {
            when(reactionRepository.findByMessageIdAndMessageType(MESSAGE_ID, MESSAGE_TYPE))
                .thenReturn(Collections.emptyList());

            List<MessageReactionEntity> result = reactionService.getReactions(MESSAGE_ID, MESSAGE_TYPE);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getReactionSummary tests")
    class GetReactionSummaryTests {

        @Test
        @DisplayName("should return reaction summary with counts")
        void getReactionSummary_Success() {
            List<Object[]> countData = Arrays.asList(
                new Object[]{"👍", 5L},
                new Object[]{"❤️", 3L}
            );
            List<Object[]> userData = Arrays.asList(
                new Object[]{"👍", 1L},
                new Object[]{"👍", 2L},
                new Object[]{"❤️", 1L}
            );

            when(reactionRepository.countReactionsByEmoji(MESSAGE_ID, MESSAGE_TYPE))
                .thenReturn(countData);
            when(reactionRepository.findReactionsWithUsers(MESSAGE_ID, MESSAGE_TYPE))
                .thenReturn(userData);

            List<MessageReactionService.ReactionSummary> summaries =
                reactionService.getReactionSummary(MESSAGE_ID, MESSAGE_TYPE);

            assertNotNull(summaries);
            assertEquals(2, summaries.size());
            assertEquals("👍", summaries.get(0).getEmoji());
            assertEquals(5, summaries.get(0).getCount());
            assertEquals(2, summaries.get(0).getUserIds().size());
        }

        @Test
        @DisplayName("should return empty list when no reactions")
        void getReactionSummary_Empty() {
            when(reactionRepository.countReactionsByEmoji(MESSAGE_ID, MESSAGE_TYPE))
                .thenReturn(Collections.emptyList());
            when(reactionRepository.findReactionsWithUsers(MESSAGE_ID, MESSAGE_TYPE))
                .thenReturn(Collections.emptyList());

            List<MessageReactionService.ReactionSummary> summaries =
                reactionService.getReactionSummary(MESSAGE_ID, MESSAGE_TYPE);

            assertNotNull(summaries);
            assertTrue(summaries.isEmpty());
        }
    }

    @Nested
    @DisplayName("hasUserReacted tests")
    class HasUserReactedTests {

        @Test
        @DisplayName("should return true when user has reacted with emoji")
        void hasUserReacted_True() {
            when(reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(true);

            assertTrue(reactionService.hasUserReacted(MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI));
        }

        @Test
        @DisplayName("should return false when user has not reacted")
        void hasUserReacted_False() {
            when(reactionRepository.existsByMessageIdAndMessageTypeAndUserIdAndEmoji(
                MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI)).thenReturn(false);

            assertFalse(reactionService.hasUserReacted(MESSAGE_ID, MESSAGE_TYPE, USER_ID, EMOJI));
        }
    }
}
