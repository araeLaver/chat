package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SearchController Tests")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageSearchService messageSearchService;

    @MockBean
    private JwtUtil jwtUtil;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";

    @BeforeEach
    void setUp() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Nested
    @DisplayName("GET /api/search/messages Tests")
    class SearchMessagesTests {

        @Test
        @DisplayName("Should search all messages when no type specified")
        void shouldSearchAllMessagesWhenNoTypeSpecified() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("content", "Hello world");
            result.put("sender", "user1");
            List<Map<String, Object>> results = Arrays.asList(result);

            when(messageSearchService.searchAllMessages(eq(1L), eq("hello"))).thenReturn(results);

            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", "hello"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keyword").value("hello"))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.results").isArray());
        }

        @Test
        @DisplayName("Should search direct messages when type is DM")
        void shouldSearchDirectMessagesWhenTypeIsDm() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("content", "Direct message");
            List<Map<String, Object>> results = Arrays.asList(result);

            when(messageSearchService.searchDirectMessages(eq(1L), eq("test"))).thenReturn(results);

            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", "test")
                            .param("type", "DM"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keyword").value("test"))
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @DisplayName("Should search room messages when type is ROOM")
        void shouldSearchRoomMessagesWhenTypeIsRoom() throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("content", "Room message");
            List<Map<String, Object>> results = Arrays.asList(result);

            when(messageSearchService.searchRoomMessages(eq(1L), eq("test"))).thenReturn(results);

            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", "test")
                            .param("type", "ROOM"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keyword").value("test"))
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @DisplayName("Should return bad request when keyword is empty")
        void shouldReturnBadRequestWhenKeywordIsEmpty() throws Exception {
            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Keyword is required"));
        }

        @Test
        @DisplayName("Should return bad request when keyword is whitespace")
        void shouldReturnBadRequestWhenKeywordIsWhitespace() throws Exception {
            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Keyword is required"));
        }

        @Test
        @DisplayName("Should return empty results when no matches")
        void shouldReturnEmptyResultsWhenNoMatches() throws Exception {
            when(messageSearchService.searchAllMessages(eq(1L), anyString())).thenReturn(Arrays.asList());

            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.results").isEmpty());
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void shouldHandleExceptionGracefully() throws Exception {
            when(jwtUtil.getUserIdFromToken(anyString())).thenThrow(new RuntimeException("Invalid token"));

            mockMvc.perform(get("/api/v1/search/messages")
                            .header("Authorization", VALID_TOKEN)
                            .param("keyword", "test"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid token"));
        }
    }
}
