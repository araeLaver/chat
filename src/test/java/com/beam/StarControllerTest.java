package com.beam;

import com.beam.StarredMessageEntity.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
@DisplayName("StarController Tests")
class StarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StarService starService;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final Long USER_ID = 1L;
    private static final Long MESSAGE_ID = 100L;

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(USER_ID);
    }

    private StarredMessageEntity createStarredMessage(Long id, Long messageId, MessageType type) {
        StarredMessageEntity entity = StarredMessageEntity.builder()
                .userId(USER_ID)
                .messageId(messageId)
                .messageType(type)
                .roomId("room-1")
                .note("Test note")
                .createdAt(LocalDateTime.now())
                .build();
        entity.setId(id);
        return entity;
    }

    @Nested
    @DisplayName("POST /api/stars Tests")
    class StarMessageTests {

        @Test
        @DisplayName("Should star message successfully")
        void shouldStarMessageSuccessfully() throws Exception {
            StarredMessageEntity starred = createStarredMessage(1L, MESSAGE_ID, MessageType.ROOM);
            when(starService.starMessage(eq(USER_ID), eq(MESSAGE_ID), eq(MessageType.ROOM), anyString(), isNull(), anyString()))
                    .thenReturn(starred);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "ROOM");
            request.put("roomId", "room-1");
            request.put("note", "Test note");

            mockMvc.perform(post("/api/v1/stars")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.starredMessage.messageId").value(MESSAGE_ID));
        }

        @Test
        @DisplayName("Should return bad request when already starred")
        void shouldReturnBadRequestWhenAlreadyStarred() throws Exception {
            when(starService.starMessage(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Message already starred"));

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "ROOM");
            request.put("roomId", "room-1");

            mockMvc.perform(post("/api/v1/stars")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Message already starred"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/stars/{messageType}/{messageId} Tests")
    class UnstarMessageTests {

        @Test
        @DisplayName("Should unstar message successfully")
        void shouldUnstarMessageSuccessfully() throws Exception {
            doNothing().when(starService).unstarMessage(USER_ID, MESSAGE_ID, MessageType.ROOM);

            mockMvc.perform(delete("/api/v1/stars/ROOM/{messageId}", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(starService).unstarMessage(USER_ID, MESSAGE_ID, MessageType.ROOM);
        }
    }

    @Nested
    @DisplayName("POST /api/stars/toggle Tests")
    class ToggleStarTests {

        @Test
        @DisplayName("Should toggle star on (star message)")
        void shouldToggleStarOn() throws Exception {
            StarredMessageEntity starred = createStarredMessage(1L, MESSAGE_ID, MessageType.DIRECT);
            when(starService.toggleStar(eq(USER_ID), eq(MESSAGE_ID), eq(MessageType.DIRECT), isNull(), anyString(), isNull()))
                    .thenReturn(starred);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "DIRECT");
            request.put("conversationId", "conv-1");

            mockMvc.perform(post("/api/v1/stars/toggle")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.starred").value(true))
                    .andExpect(jsonPath("$.starredMessage").exists());
        }

        @Test
        @DisplayName("Should toggle star off (unstar message)")
        void shouldToggleStarOff() throws Exception {
            when(starService.toggleStar(eq(USER_ID), eq(MESSAGE_ID), eq(MessageType.DIRECT), isNull(), anyString(), isNull()))
                    .thenReturn(null);

            Map<String, Object> request = new HashMap<>();
            request.put("messageId", MESSAGE_ID);
            request.put("messageType", "DIRECT");
            request.put("conversationId", "conv-1");

            mockMvc.perform(post("/api/v1/stars/toggle")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.starred").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/stars/my Tests")
    class GetMyStarredMessagesTests {

        @Test
        @DisplayName("Should get starred messages with pagination")
        void shouldGetStarredMessagesWithPagination() throws Exception {
            List<StarredMessageEntity> content = Arrays.asList(
                    createStarredMessage(1L, 100L, MessageType.ROOM),
                    createStarredMessage(2L, 101L, MessageType.DIRECT)
            );
            Page<StarredMessageEntity> page = new PageImpl<>(content, PageRequest.of(0, 20), 2);
            when(starService.getStarredMessages(USER_ID, 0, 20)).thenReturn(page);

            mockMvc.perform(get("/api/v1/stars/my")
                            .header("Authorization", VALID_TOKEN)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.starredMessages").isArray())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.currentPage").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/stars/room/{roomId} Tests")
    class GetStarredMessagesInRoomTests {

        @Test
        @DisplayName("Should get starred messages in room")
        void shouldGetStarredMessagesInRoom() throws Exception {
            List<StarredMessageEntity> messages = Arrays.asList(
                    createStarredMessage(1L, 100L, MessageType.ROOM)
            );
            when(starService.getStarredMessagesInRoom(USER_ID, "room-1")).thenReturn(messages);

            mockMvc.perform(get("/api/v1/stars/room/{roomId}", "room-1")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].messageId").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/stars/check/{messageType}/{messageId} Tests")
    class CheckStarredTests {

        @Test
        @DisplayName("Should check if message is starred")
        void shouldCheckIfMessageIsStarred() throws Exception {
            when(starService.isStarred(USER_ID, MESSAGE_ID, MessageType.ROOM)).thenReturn(true);

            mockMvc.perform(get("/api/v1/stars/check/ROOM/{messageId}", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isStarred").value(true));
        }

        @Test
        @DisplayName("Should return false when not starred")
        void shouldReturnFalseWhenNotStarred() throws Exception {
            when(starService.isStarred(USER_ID, MESSAGE_ID, MessageType.ROOM)).thenReturn(false);

            mockMvc.perform(get("/api/v1/stars/check/ROOM/{messageId}", MESSAGE_ID)
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isStarred").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/stars/count Tests")
    class GetStarredCountTests {

        @Test
        @DisplayName("Should get starred count")
        void shouldGetStarredCount() throws Exception {
            when(starService.getStarredCount(USER_ID)).thenReturn(5);

            mockMvc.perform(get("/api/v1/stars/count")
                            .header("Authorization", VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5));
        }
    }

    @Nested
    @DisplayName("PUT /api/stars/{starId}/note Tests")
    class UpdateStarNoteTests {

        @Test
        @DisplayName("Should update star note")
        void shouldUpdateStarNote() throws Exception {
            doNothing().when(starService).updateStarNote(1L, USER_ID, "Updated note");

            Map<String, Object> request = new HashMap<>();
            request.put("note", "Updated note");

            mockMvc.perform(put("/api/v1/stars/{starId}/note", 1L)
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(starService).updateStarNote(1L, USER_ID, "Updated note");
        }
    }
}
