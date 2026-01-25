package com.beam;

import com.beam.MessageReactionEntity.MessageType;
import com.beam.MessageReactionService.ReactionSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MessageReactionController Tests")
class MessageReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageReactionService reactionService;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;
    private static final String EMOJI = "\uD83D\uDC4D"; // Thumbs up

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(USER_ID);
    }

    private MessageReactionEntity createReaction(Long id, Long messageId, MessageType type, String emoji) {
        MessageReactionEntity entity = MessageReactionEntity.builder()
                .messageId(messageId)
                .messageType(type)
                .userId(USER_ID)
                .emoji(emoji)
                .createdAt(LocalDateTime.now())
                .build();
        entity.setId(id);
        return entity;
    }

    @Nested
    @DisplayName("POST /api/reactions Tests")
    class AddReactionTests {

        @Test
        @DisplayName("Should add reaction successfully")
        void shouldAddReactionSuccessfully() throws Exception {
            MessageReactionEntity reaction = createReaction(1L, MESSAGE_ID, MessageType.ROOM, EMOJI);
            when(reactionService.addReaction(eq(MESSAGE_ID), eq(MessageType.ROOM), eq(USER_ID), eq(EMOJI)))
                    .thenReturn(reaction);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "ROOM");
            request.put("emoji", EMOJI);

            mockMvc.perform(post("/api/v1/reactions")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reactionId").value(1))
                    .andExpect(jsonPath("$.emoji").value(EMOJI));
        }

        @Test
        @DisplayName("Should return bad request for invalid message type")
        void shouldReturnBadRequestForInvalidMessageType() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "INVALID");
            request.put("emoji", EMOJI);

            mockMvc.perform(post("/api/v1/reactions")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid message type"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/reactions Tests")
    class RemoveReactionTests {

        @Test
        @DisplayName("Should remove reaction successfully")
        void shouldRemoveReactionSuccessfully() throws Exception {
            doNothing().when(reactionService).removeReaction(MESSAGE_ID, MessageType.ROOM, USER_ID, EMOJI);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "ROOM");
            request.put("emoji", EMOJI);

            mockMvc.perform(delete("/api/v1/reactions")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reactionService).removeReaction(MESSAGE_ID, MessageType.ROOM, USER_ID, EMOJI);
        }
    }

    @Nested
    @DisplayName("POST /api/reactions/toggle Tests")
    class ToggleReactionTests {

        @Test
        @DisplayName("Should toggle on (add reaction)")
        void shouldToggleOn() throws Exception {
            MessageReactionEntity reaction = createReaction(1L, MESSAGE_ID, MessageType.DIRECT, EMOJI);
            when(reactionService.toggleReaction(eq(MESSAGE_ID), eq(MessageType.DIRECT), eq(USER_ID), eq(EMOJI)))
                    .thenReturn(reaction);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "DIRECT");
            request.put("emoji", EMOJI);

            mockMvc.perform(post("/api/v1/reactions/toggle")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.added").value(true));
        }

        @Test
        @DisplayName("Should toggle off (remove reaction)")
        void shouldToggleOff() throws Exception {
            when(reactionService.toggleReaction(eq(MESSAGE_ID), eq(MessageType.DIRECT), eq(USER_ID), eq(EMOJI)))
                    .thenReturn(null);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "DIRECT");
            request.put("emoji", EMOJI);

            mockMvc.perform(post("/api/v1/reactions/toggle")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.added").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/reactions/{messageType}/{messageId} Tests")
    class GetReactionsTests {

        @Test
        @DisplayName("Should get reactions summary")
        void shouldGetReactionsSummary() throws Exception {
            List<ReactionSummary> summaries = Arrays.asList(
                    new ReactionSummary(EMOJI, 5, Arrays.asList(1L, 2L, 3L, 4L, 5L))
            );
            when(reactionService.getReactionSummary(MESSAGE_ID, MessageType.ROOM)).thenReturn(summaries);

            mockMvc.perform(get("/api/v1/reactions/ROOM/{messageId}", MESSAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messageId").value(MESSAGE_ID))
                    .andExpect(jsonPath("$.reactions").isArray());
        }

        @Test
        @DisplayName("Should return bad request for invalid message type")
        void shouldReturnBadRequestForInvalidType() throws Exception {
            mockMvc.perform(get("/api/v1/reactions/INVALID/{messageId}", MESSAGE_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid message type"));
        }
    }

    @Nested
    @DisplayName("GET /api/reactions/{messageType}/{messageId}/check Tests")
    class CheckReactionTests {

        @Test
        @DisplayName("Should check if user has reacted")
        void shouldCheckIfUserHasReacted() throws Exception {
            when(reactionService.hasUserReacted(MESSAGE_ID, MessageType.ROOM, USER_ID, EMOJI)).thenReturn(true);

            mockMvc.perform(get("/api/v1/reactions/ROOM/{messageId}/check", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN)
                            .param("emoji", EMOJI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasReacted").value(true))
                    .andExpect(jsonPath("$.emoji").value(EMOJI));
        }

        @Test
        @DisplayName("Should return false when user has not reacted")
        void shouldReturnFalseWhenNotReacted() throws Exception {
            when(reactionService.hasUserReacted(MESSAGE_ID, MessageType.ROOM, USER_ID, EMOJI)).thenReturn(false);

            mockMvc.perform(get("/api/v1/reactions/ROOM/{messageId}/check", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN)
                            .param("emoji", EMOJI))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasReacted").value(false));
        }
    }
}
