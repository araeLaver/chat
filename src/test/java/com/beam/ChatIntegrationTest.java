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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 채팅 애플리케이션 E2E 통합 테스트
 *
 * <p>주요 사용자 시나리오를 테스트합니다:
 * <ul>
 *   <li>회원가입 → 로그인 → 친구 추가 → DM 전송</li>
 *   <li>채팅방 생성 → 메시지 전송 → 메시지 조회</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Chat Application Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RateLimitService rateLimitService;

    private static final String TEST_USER1 = "integrationuser1";
    private static final String TEST_USER2 = "integrationuser2";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_EMAIL1 = "integration1@example.com";
    private static final String TEST_EMAIL2 = "integration2@example.com";

    @BeforeEach
    void setUp() {
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);
        when(emailService.sendVerificationEmail(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(emailService.sendWelcomeEmail(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    @Order(1)
    @DisplayName("회원가입 → 로그인 플로우")
    void testRegistrationAndLoginFlow() throws Exception {
        // 1. 회원가입
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setUsername(TEST_USER1);
        registerRequest.setPassword(TEST_PASSWORD);
        registerRequest.setEmail(TEST_EMAIL1);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(TEST_USER1));

        // 2. 활성화
        UserEntity user = userRepository.findByUsername(TEST_USER1).orElseThrow();
        user.setIsActive(true);
        userRepository.save(user);

        // 3. 로그인
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername(TEST_USER1);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.username").value(TEST_USER1))
            .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        assertThat(response).contains("token");
    }

    @Test
    @Order(2)
    @DisplayName("친구 추가 플로우")
    void testFriendRequestFlow() throws Exception {
        UserEntity user1 = createTestUser(TEST_USER1, TEST_EMAIL1);
        UserEntity user2 = createTestUser(TEST_USER2, TEST_EMAIL2);

        String token1 = jwtUtil.generateToken(user1.getUsername(), user1.getId());
        String token2 = jwtUtil.generateToken(user2.getUsername(), user2.getId());

        // 1. 친구 요청 전송 (POST /api/friends/request with body)
        Map<String, Object> friendRequest = Map.of("friendId", user2.getId());
        mockMvc.perform(post("/api/v1/friends/request")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 2. 받은 친구 요청 조회
        mockMvc.perform(get("/api/v1/friends/requests/received")
                .header("Authorization", "Bearer " + token2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // 3. 친구 요청 수락
        Map<String, Object> acceptRequest = Map.of("requesterId", user1.getId());
        mockMvc.perform(post("/api/v1/friends/accept")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(acceptRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 4. 친구 목록 확인
        mockMvc.perform(get("/api/v1/friends/list")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("DM 메시지 전송 및 조회 플로우")
    void testDirectMessageFlow() throws Exception {
        UserEntity user1 = createTestUser(TEST_USER1, TEST_EMAIL1);
        UserEntity user2 = createTestUser(TEST_USER2, TEST_EMAIL2);

        String token1 = jwtUtil.generateToken(user1.getUsername(), user1.getId());
        String token2 = jwtUtil.generateToken(user2.getUsername(), user2.getId());

        // 1. DM 전송
        Map<String, Object> dmRequest = Map.of(
            "receiverId", user2.getId(),
            "content", "Hello, this is a test message!"
        );

        mockMvc.perform(post("/api/v1/dm/send")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dmRequest)))
            .andExpect(status().isOk());

        // 2. 대화 목록 조회
        mockMvc.perform(get("/api/v1/dm/conversations")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // 3. 메시지 조회 (GET /api/dm/conversation/{conversationId})
        String conversationId = DirectMessageEntity.generateConversationId(user1.getId(), user2.getId());
        mockMvc.perform(get("/api/v1/dm/conversation/" + conversationId)
                .header("Authorization", "Bearer " + token2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("채팅방 생성 플로우")
    void testChatRoomFlow() throws Exception {
        UserEntity user1 = createTestUser(TEST_USER1, TEST_EMAIL1);
        String token1 = jwtUtil.generateToken(user1.getUsername(), user1.getId());

        // 1. 채팅방 생성 (POST /api/rooms)
        Map<String, Object> roomRequest = Map.of(
            "roomName", "Integration Test Room",
            "description", "Test room description",
            "roomType", "PUBLIC",
            "maxMembers", 100
        );

        mockMvc.perform(post("/api/v1/rooms")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roomRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roomName").value("Integration Test Room"));

        // 2. 내 채팅방 목록 조회
        mockMvc.perform(get("/api/v1/rooms/my-rooms")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("게스트 로그인 플로우")
    void testGuestLoginFlow() throws Exception {
        MvcResult guestResult = mockMvc.perform(post("/api/v1/auth/guest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.user").exists())
            .andReturn();

        String response = guestResult.getResponse().getContentAsString();
        assertThat(response).contains("token");
    }

    @Test
    @Order(6)
    @DisplayName("Health Check 엔드포인트 테스트")
    void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/health/live"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/health/ready"))
            .andExpect(status().isOk());
    }

    private UserEntity createTestUser(String username, String email) {
        UserEntity existing = userRepository.findByUsername(username).orElse(null);
        if (existing != null) {
            return existing;
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(TEST_PASSWORD);
        user.setEmail(email);
        user.setPhoneNumber("010" + String.format("%08d", (int)(Math.random() * 100000000)));
        user.setIsActive(true);
        user.setCreatedAt(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }
}
