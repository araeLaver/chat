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
@DisplayName("FriendService Unit Tests")
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendService friendService;

    private UserEntity user1;
    private UserEntity user2;
    private FriendEntity pendingRequest;
    private FriendEntity acceptedFriend;

    @BeforeEach
    void setUp() {
        user1 = UserEntity.builder()
                .id(1L)
                .username("user1")
                .build();

        user2 = UserEntity.builder()
                .id(2L)
                .username("user2")
                .build();

        pendingRequest = FriendEntity.builder()
                .id(1L)
                .userId(1L)
                .friendId(2L)
                .status(FriendEntity.FriendStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        acceptedFriend = FriendEntity.builder()
                .id(2L)
                .userId(1L)
                .friendId(2L)
                .status(FriendEntity.FriendStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Send Friend Request Tests")
    class SendFriendRequestTests {

        @Test
        @DisplayName("Should send friend request successfully")
        void shouldSendFriendRequestSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.empty());
            when(friendRepository.save(any(FriendEntity.class))).thenReturn(pendingRequest);

            // When
            FriendEntity result = friendService.sendFriendRequest(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(FriendEntity.FriendStatus.PENDING);
            verify(friendRepository).save(argThat(friend ->
                friend.getStatus() == FriendEntity.FriendStatus.PENDING
            ));
        }

        @Test
        @DisplayName("Should fail when adding yourself")
        void shouldFailWhenAddingYourself() {
            // When & Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot add yourself as a friend");
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should fail when friend not found")
        void shouldFailWhenFriendNotFound() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Friend user not found");
        }

        @Test
        @DisplayName("Should fail when user is blocked")
        void shouldFailWhenUserIsBlocked() {
            // Given
            FriendEntity blocked = FriendEntity.builder()
                    .status(FriendEntity.FriendStatus.BLOCKED)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(blocked));

            // When & Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot send friend request to blocked user");
        }

        @Test
        @DisplayName("Should fail when request already sent")
        void shouldFailWhenRequestAlreadySent() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(pendingRequest));

            // When & Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Friend request already sent");
        }

        @Test
        @DisplayName("Should fail when already friends")
        void shouldFailWhenAlreadyFriends() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(acceptedFriend));

            // When & Then
            assertThatThrownBy(() -> friendService.sendFriendRequest(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Already friends");
        }
    }

    @Nested
    @DisplayName("Accept Friend Request Tests")
    class AcceptFriendRequestTests {

        @Test
        @DisplayName("Should accept friend request successfully")
        void shouldAcceptFriendRequestSuccessfully() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(pendingRequest));
            when(friendRepository.save(any(FriendEntity.class))).thenReturn(acceptedFriend);

            // When
            FriendEntity result = friendService.acceptFriendRequest(2L, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(friendRepository).save(argThat(friend ->
                friend.getStatus() == FriendEntity.FriendStatus.ACCEPTED &&
                friend.getAcceptedAt() != null
            ));
        }

        @Test
        @DisplayName("Should fail when request not found")
        void shouldFailWhenRequestNotFound() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(2L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Friend request not found");
        }

        @Test
        @DisplayName("Should fail when trying to accept own request")
        void shouldFailWhenAcceptingOwnRequest() {
            // Given - user1 sent request to user2 (pendingRequest has userId=1, friendId=2)
            // When user1 tries to accept (should only be accepted by user2)
            // acceptFriendRequest(userId=1, requesterId=1) calls findFriendship(1, 1)
            when(friendRepository.findFriendship(1L, 1L)).thenReturn(Optional.of(pendingRequest));

            // When & Then (user1 tries to accept - but friendId=2, not 1)
            assertThatThrownBy(() -> friendService.acceptFriendRequest(1L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot accept this friend request");
        }

        @Test
        @DisplayName("Should fail when request not pending")
        void shouldFailWhenNotPending() {
            // Given
            acceptedFriend.setStatus(FriendEntity.FriendStatus.ACCEPTED);
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(acceptedFriend));

            // When & Then
            assertThatThrownBy(() -> friendService.acceptFriendRequest(2L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Friend request is not pending");
        }
    }

    @Nested
    @DisplayName("Reject Friend Request Tests")
    class RejectFriendRequestTests {

        @Test
        @DisplayName("Should reject friend request successfully")
        void shouldRejectFriendRequestSuccessfully() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(pendingRequest));

            // When
            friendService.rejectFriendRequest(2L, 1L);

            // Then
            verify(friendRepository).save(argThat(friend ->
                friend.getStatus() == FriendEntity.FriendStatus.REJECTED
            ));
        }

        @Test
        @DisplayName("Should fail when request not found")
        void shouldFailWhenRequestNotFound() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> friendService.rejectFriendRequest(2L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Friend request not found");
        }
    }

    @Nested
    @DisplayName("Block User Tests")
    class BlockUserTests {

        @Test
        @DisplayName("Should block existing friendship")
        void shouldBlockExistingFriendship() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(acceptedFriend));

            // When
            friendService.blockUser(1L, 2L);

            // Then
            verify(friendRepository).save(argThat(friend ->
                friend.getStatus() == FriendEntity.FriendStatus.BLOCKED &&
                friend.getBlockedAt() != null
            ));
        }

        @Test
        @DisplayName("Should create new block relation when no friendship exists")
        void shouldCreateNewBlockRelation() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.empty());

            // When
            friendService.blockUser(1L, 2L);

            // Then
            verify(friendRepository).save(argThat(friend ->
                friend.getStatus() == FriendEntity.FriendStatus.BLOCKED
            ));
        }
    }

    @Nested
    @DisplayName("Unfriend Tests")
    class UnfriendTests {

        @Test
        @DisplayName("Should unfriend successfully")
        void shouldUnfriendSuccessfully() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.of(acceptedFriend));

            // When
            friendService.unfriend(1L, 2L);

            // Then
            verify(friendRepository).delete(acceptedFriend);
        }

        @Test
        @DisplayName("Should fail when friendship not found")
        void shouldFailWhenFriendshipNotFound() {
            // Given
            when(friendRepository.findFriendship(1L, 2L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> friendService.unfriend(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Friendship not found");
        }
    }

    @Nested
    @DisplayName("Get Friend List Tests")
    class GetFriendListTests {

        @Test
        @DisplayName("Should get friend list successfully")
        void shouldGetFriendListSuccessfully() {
            // Given
            when(friendRepository.findAcceptedFriends(1L)).thenReturn(List.of(acceptedFriend));

            // When
            List<FriendEntity> result = friendService.getFriendList(1L);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Pending Requests Tests")
    class GetPendingRequestsTests {

        @Test
        @DisplayName("Should get pending requests received")
        void shouldGetPendingRequestsReceived() {
            // Given
            when(friendRepository.findPendingRequestsReceived(2L)).thenReturn(List.of(pendingRequest));

            // When
            List<FriendEntity> result = friendService.getPendingRequestsReceived(2L);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get pending requests sent")
        void shouldGetPendingRequestsSent() {
            // Given
            when(friendRepository.findByUserIdAndStatus(1L, FriendEntity.FriendStatus.PENDING))
                    .thenReturn(List.of(pendingRequest));

            // When
            List<FriendEntity> result = friendService.getPendingRequestsSent(1L);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get pending request count")
        void shouldGetPendingRequestCount() {
            // Given
            when(friendRepository.countPendingRequests(2L)).thenReturn(3);

            // When
            Integer result = friendService.getPendingRequestCount(2L);

            // Then
            assertThat(result).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Search Users Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUserByUsername() {
            // Given
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
            when(userRepository.findByPhoneNumber("user1")).thenReturn(Optional.empty());

            // When
            List<UserEntity> result = friendService.searchUsers("user1");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("user1");
        }

        @Test
        @DisplayName("Should find user by phone number")
        void shouldFindUserByPhoneNumber() {
            // Given
            user1.setPhoneNumber("010-1234-5678");
            when(userRepository.findByUsername("010-1234-5678")).thenReturn(Optional.empty());
            when(userRepository.findByPhoneNumber("010-1234-5678")).thenReturn(Optional.of(user1));

            // When
            List<UserEntity> result = friendService.searchUsers("010-1234-5678");

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should not duplicate when same user found by both")
        void shouldNotDuplicateResults() {
            // Given
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
            when(userRepository.findByPhoneNumber("user1")).thenReturn(Optional.of(user1));

            // When
            List<UserEntity> result = friendService.searchUsers("user1");

            // Then
            assertThat(result).hasSize(1);
        }
    }
}
