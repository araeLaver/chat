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
@DisplayName("FriendController Integration Tests")
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitService rateLimitService;

    private UserEntity testUser;
    private UserEntity otherUser;
    private UserEntity thirdUser;
    private String token;

    @BeforeEach
    void setUp() {
        // Mock rate limit service
        when(rateLimitService.isApiRequestAllowed(anyString())).thenReturn(true);
        when(rateLimitService.getApiRemainingTokens(anyString())).thenReturn(100L);

        // Create test users
        testUser = UserEntity.builder()
                .username("friendtestuser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("friendtest@example.com")
                .displayName("Friend Test User")
                .phoneNumber("010-1234-5678")
                .isActive(true)
                .isOnline(true)
                .build();
        testUser = userRepository.save(testUser);

        otherUser = UserEntity.builder()
                .username("friendother")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("friendother@example.com")
                .displayName("Friend Other User")
                .phoneNumber("010-8765-4321")
                .isActive(true)
                .isOnline(false)
                .build();
        otherUser = userRepository.save(otherUser);

        thirdUser = UserEntity.builder()
                .username("thirduser")
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMye")
                .email("third@example.com")
                .displayName("Third User")
                .phoneNumber("010-1111-2222")
                .isActive(true)
                .isOnline(true)
                .build();
        thirdUser = userRepository.save(thirdUser);

        token = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());
    }

    @Nested
    @DisplayName("Friend Request Endpoint Tests")
    class FriendRequestTests {

        @Test
        @DisplayName("Should send friend request successfully")
        void shouldSendFriendRequestSuccessfully() throws Exception {
            String requestBody = "{\"friendId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/v1/friends/request")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Friend request sent"))
                    .andExpect(jsonPath("$.requestId").exists())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should accept friend request successfully")
        void shouldAcceptFriendRequestSuccessfully() throws Exception {
            // Create pending friend request from otherUser to testUser
            FriendEntity request = FriendEntity.builder()
                    .userId(otherUser.getId())
                    .friendId(testUser.getId())
                    .status(FriendEntity.FriendStatus.PENDING)
                    .build();
            friendRepository.save(request);

            String requestBody = "{\"requesterId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/v1/friends/accept")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Friend request accepted"))
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("Should reject friend request successfully")
        void shouldRejectFriendRequestSuccessfully() throws Exception {
            // Create pending friend request from otherUser to testUser
            FriendEntity request = FriendEntity.builder()
                    .userId(otherUser.getId())
                    .friendId(testUser.getId())
                    .status(FriendEntity.FriendStatus.PENDING)
                    .build();
            friendRepository.save(request);

            String requestBody = "{\"requesterId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/v1/friends/reject")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Friend request rejected"));
        }
    }

    @Nested
    @DisplayName("Block User Endpoint Tests")
    class BlockUserTests {

        @Test
        @DisplayName("Should block user successfully")
        void shouldBlockUserSuccessfully() throws Exception {
            String requestBody = "{\"userId\": " + otherUser.getId() + "}";

            mockMvc.perform(post("/api/v1/friends/block")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User blocked"));
        }
    }

    @Nested
    @DisplayName("Unfriend Endpoint Tests")
    class UnfriendTests {

        @Test
        @DisplayName("Should unfriend successfully")
        void shouldUnfriendSuccessfully() throws Exception {
            // Create accepted friendship
            FriendEntity friendship = FriendEntity.builder()
                    .userId(testUser.getId())
                    .friendId(otherUser.getId())
                    .status(FriendEntity.FriendStatus.ACCEPTED)
                    .build();
            friendRepository.save(friendship);

            mockMvc.perform(delete("/api/v1/friends/unfriend/" + otherUser.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Friend removed"));
        }
    }

    @Nested
    @DisplayName("Friend List Endpoint Tests")
    class FriendListTests {

        @Test
        @DisplayName("Should get friend list successfully")
        void shouldGetFriendListSuccessfully() throws Exception {
            // Create accepted friendship
            FriendEntity friendship = FriendEntity.builder()
                    .userId(testUser.getId())
                    .friendId(otherUser.getId())
                    .status(FriendEntity.FriendStatus.ACCEPTED)
                    .build();
            friendRepository.save(friendship);

            mockMvc.perform(get("/api/v1/friends/list")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].friendId").value(otherUser.getId()))
                    .andExpect(jsonPath("$[0].displayName").value("Friend Other User"));
        }

        @Test
        @DisplayName("Should return empty list when no friends")
        void shouldReturnEmptyListWhenNoFriends() throws Exception {
            mockMvc.perform(get("/api/v1/friends/list")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("Pending Requests Endpoint Tests")
    class PendingRequestsTests {

        @Test
        @DisplayName("Should get received friend requests")
        void shouldGetReceivedFriendRequests() throws Exception {
            // Create pending friend request from otherUser to testUser
            FriendEntity request = FriendEntity.builder()
                    .userId(otherUser.getId())
                    .friendId(testUser.getId())
                    .status(FriendEntity.FriendStatus.PENDING)
                    .build();
            friendRepository.save(request);

            mockMvc.perform(get("/api/v1/friends/requests/received")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].requesterId").value(otherUser.getId()))
                    .andExpect(jsonPath("$[0].displayName").value("Friend Other User"));
        }

        @Test
        @DisplayName("Should get sent friend requests")
        void shouldGetSentFriendRequests() throws Exception {
            // Create pending friend request from testUser to otherUser
            FriendEntity request = FriendEntity.builder()
                    .userId(testUser.getId())
                    .friendId(otherUser.getId())
                    .status(FriendEntity.FriendStatus.PENDING)
                    .build();
            friendRepository.save(request);

            mockMvc.perform(get("/api/v1/friends/requests/sent")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].friendId").value(otherUser.getId()))
                    .andExpect(jsonPath("$[0].displayName").value("Friend Other User"));
        }

        @Test
        @DisplayName("Should get pending request count")
        void shouldGetPendingRequestCount() throws Exception {
            // Create pending friend requests from other users
            FriendEntity request1 = FriendEntity.builder()
                    .userId(otherUser.getId())
                    .friendId(testUser.getId())
                    .status(FriendEntity.FriendStatus.PENDING)
                    .build();
            FriendEntity request2 = FriendEntity.builder()
                    .userId(thirdUser.getId())
                    .friendId(testUser.getId())
                    .status(FriendEntity.FriendStatus.PENDING)
                    .build();
            friendRepository.save(request1);
            friendRepository.save(request2);

            mockMvc.perform(get("/api/v1/friends/requests/count")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2));
        }
    }

    @Nested
    @DisplayName("Search Users Endpoint Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users by username")
        void shouldSearchUsersByUsername() throws Exception {
            mockMvc.perform(get("/api/v1/friends/search")
                            .header("Authorization", "Bearer " + token)
                            .param("query", "friendother"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].username").value("friendother"));
        }

        @Test
        @DisplayName("Should exclude self from search results")
        void shouldExcludeSelfFromSearchResults() throws Exception {
            mockMvc.perform(get("/api/v1/friends/search")
                            .header("Authorization", "Bearer " + token)
                            .param("query", "friendtest"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return empty when no matches")
        void shouldReturnEmptyWhenNoMatches() throws Exception {
            mockMvc.perform(get("/api/v1/friends/search")
                            .header("Authorization", "Bearer " + token)
                            .param("query", "nonexistentuser123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
