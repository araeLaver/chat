package com.beam;

import com.beam.PinnedMessageEntity.ContextType;
import com.beam.PinnedMessageEntity.MessageType;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("PinController Tests")
class PinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PinService pinService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;
    private static final String ROOM_ID = "room-1";

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(USER_ID);

        UserEntity user = UserEntity.builder()
                .username("testuser")
                .displayName("Test User")
                .password("password")
                .email("test@test.com")
                .phoneNumber("01012345678")
                .build();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private PinnedMessageEntity createPinnedMessage(Long id, Long messageId, ContextType contextType, String contextId) {
        PinnedMessageEntity entity = PinnedMessageEntity.builder()
                .messageId(messageId)
                .messageType(MessageType.ROOM)
                .contextType(contextType)
                .contextId(contextId)
                .pinnedByUserId(USER_ID)
                .pinOrder(1)
                .note("Test note")
                .createdAt(LocalDateTime.now())
                .build();
        entity.setId(id);
        return entity;
    }

    @Nested
    @DisplayName("POST /api/pins Tests")
    class PinMessageTests {

        @Test
        @DisplayName("Should pin message successfully")
        void shouldPinMessageSuccessfully() throws Exception {
            PinnedMessageEntity pinned = createPinnedMessage(1L, MESSAGE_ID, ContextType.ROOM, ROOM_ID);
            when(pinService.pinMessage(eq(MESSAGE_ID), eq(MessageType.ROOM), eq(ContextType.ROOM), eq(ROOM_ID), eq(USER_ID), anyString()))
                    .thenReturn(pinned);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "ROOM");
            request.put("contextType", "ROOM");
            request.put("contextId", ROOM_ID);
            request.put("note", "Important message");

            mockMvc.perform(post("/api/v1/pins")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.pinnedMessage.messageId").value(MESSAGE_ID));
        }

        @Test
        @DisplayName("Should return bad request when already pinned")
        void shouldReturnBadRequestWhenAlreadyPinned() throws Exception {
            when(pinService.pinMessage(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Message already pinned"));

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "ROOM");
            request.put("contextType", "ROOM");
            request.put("contextId", ROOM_ID);

            mockMvc.perform(post("/api/v1/pins")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Message already pinned"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/pins/{messageType}/{messageId} Tests")
    class UnpinMessageTests {

        @Test
        @DisplayName("Should unpin message successfully")
        void shouldUnpinMessageSuccessfully() throws Exception {
            doNothing().when(pinService).unpinMessage(MESSAGE_ID, MessageType.ROOM, ContextType.ROOM, ROOM_ID);

            mockMvc.perform(delete("/api/v1/pins/ROOM/{messageId}", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN)
                            .param("contextType", "ROOM")
                            .param("contextId", ROOM_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(pinService).unpinMessage(MESSAGE_ID, MessageType.ROOM, ContextType.ROOM, ROOM_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/pins/{contextType}/{contextId} Tests")
    class GetPinnedMessagesTests {

        @Test
        @DisplayName("Should get pinned messages for room")
        void shouldGetPinnedMessagesForRoom() throws Exception {
            List<PinnedMessageEntity> messages = Arrays.asList(
                    createPinnedMessage(1L, 100L, ContextType.ROOM, ROOM_ID),
                    createPinnedMessage(2L, 101L, ContextType.ROOM, ROOM_ID)
            );
            when(pinService.getPinnedMessages(ContextType.ROOM, ROOM_ID)).thenReturn(messages);

            mockMvc.perform(get("/api/v1/pins/ROOM/{contextId}", ROOM_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pinnedMessages").isArray())
                    .andExpect(jsonPath("$.count").value(2));
        }

        @Test
        @DisplayName("Should return empty list when no pinned messages")
        void shouldReturnEmptyListWhenNoPinnedMessages() throws Exception {
            when(pinService.getPinnedMessages(ContextType.ROOM, ROOM_ID)).thenReturn(Arrays.asList());

            mockMvc.perform(get("/api/v1/pins/ROOM/{contextId}", ROOM_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pinnedMessages").isEmpty())
                    .andExpect(jsonPath("$.count").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/pins/check/{messageType}/{messageId} Tests")
    class CheckPinnedTests {

        @Test
        @DisplayName("Should check if message is pinned")
        void shouldCheckIfMessageIsPinned() throws Exception {
            when(pinService.isPinned(MESSAGE_ID, MessageType.ROOM, ContextType.ROOM, ROOM_ID)).thenReturn(true);

            mockMvc.perform(get("/api/v1/pins/check/ROOM/{messageId}", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN)
                            .param("contextType", "ROOM")
                            .param("contextId", ROOM_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isPinned").value(true));
        }

        @Test
        @DisplayName("Should return false when not pinned")
        void shouldReturnFalseWhenNotPinned() throws Exception {
            when(pinService.isPinned(MESSAGE_ID, MessageType.ROOM, ContextType.ROOM, ROOM_ID)).thenReturn(false);

            mockMvc.perform(get("/api/v1/pins/check/ROOM/{messageId}", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN)
                            .param("contextType", "ROOM")
                            .param("contextId", ROOM_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isPinned").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/pins/{pinnedMessageId}/order Tests")
    class UpdatePinOrderTests {

        @Test
        @DisplayName("Should update pin order")
        void shouldUpdatePinOrder() throws Exception {
            doNothing().when(pinService).updatePinOrder(1L, 2, USER_ID);

            Map<String, Object> request = new HashMap<>();
            request.put("order", 2);

            mockMvc.perform(put("/api/v1/pins/{pinnedMessageId}/order", 1L)
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(pinService).updatePinOrder(1L, 2, USER_ID);
        }
    }
}
