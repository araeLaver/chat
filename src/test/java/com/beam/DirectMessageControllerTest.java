package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("DirectMessageController Integration Tests")
class DirectMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitService rateLimitService;

    private UserEntity testUser;
    private UserEntity otherUser;
    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() {
        // Mock rate limit service
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        // Create test users
        testUser = UserEntity.builder()
                .username("dmtestuser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("dmtest@example.com")
                .phoneNumber("01011112222")
                .displayName("DM Test User")
                .isActive(true)
                .isOnline(true)
                .build();
        testUser = userRepository.save(testUser);

        otherUser = UserEntity.builder()
                .username("dmotheruser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("dmother@example.com")
                .phoneNumber("01033334444")
                .displayName("DM Other User")
                .isActive(true)
                .isOnline(false)
                .build();
        otherUser = userRepository.save(otherUser);

        token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());
        otherToken = jwtUtil.generateToken(otherUser.getUsername(), otherUser.getId());
    }

    private ConversationEntity createConversation() {
        String conversationId = testUser.getId() < otherUser.getId()
                ? testUser.getId() + "_" + otherUser.getId()
                : otherUser.getId() + "_" + testUser.getId();

        ConversationEntity conversation = ConversationEntity.builder()
                .conversationId(conversationId)
                .user1Id(testUser.getId())
                .user2Id(otherUser.getId())
                .build();
        return conversationRepository.save(conversation);
    }

    @Nested
    @DisplayName("Send Message Endpoint Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() throws Exception {
            String requestBody = """
                {
                    "receiverId": %d,
                    "content": "Hello, how are you?"
                }
                """.formatted(otherUser.getId());

            mockMvc.perform(post("/api/dm/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.messageId").exists())
                    .andExpect(jsonPath("$.conversationId").exists())
                    .andExpect(jsonPath("$.content").value("Hello, how are you?"));
        }

        @Test
        @DisplayName("Should create conversation if not exists")
        void shouldCreateConversationIfNotExists() throws Exception {
            String requestBody = """
                {
                    "receiverId": %d,
                    "content": "First message"
                }
                """.formatted(otherUser.getId());

            mockMvc.perform(post("/api/dm/send")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.conversationId").exists());
        }
    }

    @Nested
    @DisplayName("Get Conversations Endpoint Tests")
    class GetConversationsTests {

        @Test
        @DisplayName("Should get conversations successfully")
        void shouldGetConversationsSuccessfully() throws Exception {
            ConversationEntity conversation = createConversation();

            mockMvc.perform(get("/api/dm/conversations")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].conversationId").value(conversation.getConversationId()))
                    .andExpect(jsonPath("$[0].otherUserId").value(otherUser.getId()))
                    .andExpect(jsonPath("$[0].otherUserName").value("DM Other User"));
        }

        @Test
        @DisplayName("Should return empty list when no conversations")
        void shouldReturnEmptyListWhenNoConversations() throws Exception {
            mockMvc.perform(get("/api/dm/conversations")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Conversation Messages Endpoint Tests")
    class GetConversationMessagesTests {

        @Test
        @DisplayName("Should get conversation messages successfully")
        void shouldGetConversationMessagesSuccessfully() throws Exception {
            ConversationEntity conversation = createConversation();

            // Create a message
            DirectMessageEntity message = DirectMessageEntity.builder()
                    .conversationId(conversation.getConversationId())
                    .senderId(testUser.getId())
                    .receiverId(otherUser.getId())
                    .content("Test message")
                    .messageType(DirectMessageEntity.MessageType.TEXT)
                    .isRead(false)
                    .build();
            directMessageRepository.save(message);

            mockMvc.perform(get("/api/dm/conversation/" + conversation.getConversationId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].content").value("Test message"))
                    .andExpect(jsonPath("$[0].isMine").value(true));
        }

        @Test
        @DisplayName("Should show isMine correctly for receiver")
        void shouldShowIsMineCorrectlyForReceiver() throws Exception {
            ConversationEntity conversation = createConversation();

            // Create a message from testUser
            DirectMessageEntity message = DirectMessageEntity.builder()
                    .conversationId(conversation.getConversationId())
                    .senderId(testUser.getId())
                    .receiverId(otherUser.getId())
                    .content("Test message")
                    .messageType(DirectMessageEntity.MessageType.TEXT)
                    .isRead(false)
                    .build();
            directMessageRepository.save(message);

            // Get messages as otherUser
            mockMvc.perform(get("/api/dm/conversation/" + conversation.getConversationId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].isMine").value(false));
        }
    }

    @Nested
    @DisplayName("Start Conversation Endpoint Tests")
    class StartConversationTests {

        @Test
        @DisplayName("Should start new conversation successfully")
        void shouldStartNewConversationSuccessfully() throws Exception {
            String requestBody = "{\"userId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/dm/conversation/start")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.conversationId").exists())
                    .andExpect(jsonPath("$.otherUserId").value(otherUser.getId()))
                    .andExpect(jsonPath("$.otherUserName").value("DM Other User"));
        }

        @Test
        @DisplayName("Should return existing conversation if exists")
        void shouldReturnExistingConversationIfExists() throws Exception {
            ConversationEntity existingConversation = createConversation();

            String requestBody = "{\"userId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/dm/conversation/start")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.conversationId").value(existingConversation.getConversationId()));
        }
    }

    @Nested
    @DisplayName("Mark As Read Endpoint Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark messages as read successfully")
        void shouldMarkMessagesAsReadSuccessfully() throws Exception {
            ConversationEntity conversation = createConversation();

            // Create unread messages
            DirectMessageEntity message = DirectMessageEntity.builder()
                    .conversationId(conversation.getConversationId())
                    .senderId(otherUser.getId())
                    .receiverId(testUser.getId())
                    .content("Unread message")
                    .messageType(DirectMessageEntity.MessageType.TEXT)
                    .isRead(false)
                    .build();
            directMessageRepository.save(message);

            mockMvc.perform(post("/api/dm/conversation/" + conversation.getConversationId() + "/read")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Messages marked as read"));
        }
    }
}
