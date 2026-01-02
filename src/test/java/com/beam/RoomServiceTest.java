package com.beam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Unit Tests")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private GroupMessageRepository groupMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private RoomService roomService;

    private UserEntity testUser;
    private RoomEntity testRoom;
    private RoomMemberEntity ownerMember;
    private RoomMemberEntity regularMember;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .build();

        testRoom = RoomEntity.builder()
                .id(1L)
                .roomName("Test Room")
                .description("Test Description")
                .roomType(RoomEntity.RoomType.PUBLIC)
                .createdBy(1L)
                .maxMembers(100)
                .currentMembers(2)
                .isActive(true)
                .build();

        ownerMember = RoomMemberEntity.builder()
                .id(1L)
                .roomId(1L)
                .userId(1L)
                .role(RoomMemberEntity.MemberRole.OWNER)
                .isActive(true)
                .isMuted(false)
                .build();

        regularMember = RoomMemberEntity.builder()
                .id(2L)
                .roomId(1L)
                .userId(2L)
                .role(RoomMemberEntity.MemberRole.MEMBER)
                .isActive(true)
                .isMuted(false)
                .build();
    }

    @Nested
    @DisplayName("Create Room Tests")
    class CreateRoomTests {

        @Test
        @DisplayName("Should create room successfully")
        void shouldCreateRoomSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> {
                RoomEntity saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(ownerMember);

            // When
            RoomEntity result = roomService.createRoom(1L, "New Room", "Description",
                    RoomEntity.RoomType.PUBLIC, 50);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRoomName()).isEqualTo("New Room");
            assertThat(result.getMaxMembers()).isEqualTo(50);
            assertThat(result.getCurrentMembers()).isEqualTo(1);

            verify(roomMemberRepository).save(argThat(member ->
                member.getRole() == RoomMemberEntity.MemberRole.OWNER
            ));
        }

        @Test
        @DisplayName("Should use default values when not provided")
        void shouldUseDefaultValues() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(roomRepository.save(any(RoomEntity.class))).thenAnswer(inv -> {
                RoomEntity saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(ownerMember);

            // When
            RoomEntity result = roomService.createRoom(1L, "New Room", "Desc", null, null);

            // Then
            assertThat(result.getRoomType()).isEqualTo(RoomEntity.RoomType.PUBLIC);
            assertThat(result.getMaxMembers()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(999L, "Room", "Desc", null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("Update Room Tests")
    class UpdateRoomTests {

        @Test
        @DisplayName("Should update room successfully as owner")
        void shouldUpdateRoomAsOwner() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When
            RoomEntity result = roomService.updateRoom(1L, 1L, "Updated Name", "New Desc", 200);

            // Then
            assertThat(result).isNotNull();
            verify(roomRepository).save(argThat(room ->
                "Updated Name".equals(room.getRoomName()) &&
                "New Desc".equals(room.getDescription()) &&
                room.getMaxMembers() == 200
            ));
        }

        @Test
        @DisplayName("Should update room successfully as admin")
        void shouldUpdateRoomAsAdmin() {
            // Given
            RoomMemberEntity adminMember = RoomMemberEntity.builder()
                    .id(3L)
                    .roomId(1L)
                    .userId(3L)
                    .role(RoomMemberEntity.MemberRole.ADMIN)
                    .isActive(true)
                    .build();

            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 3L))
                    .thenReturn(Optional.of(adminMember));
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When
            RoomEntity result = roomService.updateRoom(1L, 3L, "Updated", null, null);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should fail when regular member tries to update")
        void shouldFailWhenRegularMemberUpdates() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 2L))
                    .thenReturn(Optional.of(regularMember));

            // When & Then
            assertThatThrownBy(() -> roomService.updateRoom(1L, 2L, "Name", null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No permission to update room");
        }

        @Test
        @DisplayName("Should fail when room not found")
        void shouldFailWhenRoomNotFound() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.updateRoom(999L, 1L, "Name", null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Room not found");
        }
    }

    @Nested
    @DisplayName("Delete Room Tests")
    class DeleteRoomTests {

        @Test
        @DisplayName("Should delete room successfully as owner")
        void shouldDeleteRoomAsOwner() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomMemberRepository.findByRoomIdAndIsActiveTrue(1L))
                    .thenReturn(List.of(ownerMember, regularMember));
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When
            roomService.deleteRoom(1L, 1L);

            // Then
            verify(roomRepository).save(argThat(room -> !room.getIsActive()));
            verify(roomMemberRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should fail when non-owner tries to delete")
        void shouldFailWhenNonOwnerDeletes() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 2L))
                    .thenReturn(Optional.of(regularMember));

            // When & Then
            assertThatThrownBy(() -> roomService.deleteRoom(1L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Only owner can delete room");
        }
    }

    @Nested
    @DisplayName("Add Member Tests")
    class AddMemberTests {

        @Test
        @DisplayName("Should add member successfully")
        void shouldAddMemberSuccessfully() {
            // Given
            UserEntity newUser = UserEntity.builder().id(3L).username("newuser").build();

            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(userRepository.findById(3L)).thenReturn(Optional.of(newUser));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(1L, 3L)).thenReturn(false);
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(regularMember);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When & Then
            assertThatCode(() -> roomService.addMember(1L, 3L, 1L)).doesNotThrowAnyException();

            verify(roomMemberRepository).save(argThat(member ->
                member.getRole() == RoomMemberEntity.MemberRole.MEMBER
            ));
        }

        @Test
        @DisplayName("Should fail when room is full")
        void shouldFailWhenRoomIsFull() {
            // Given
            testRoom.setCurrentMembers(100);
            testRoom.setMaxMembers(100);

            UserEntity newUser = UserEntity.builder().id(3L).build();

            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(userRepository.findById(3L)).thenReturn(Optional.of(newUser));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(1L, 3L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> roomService.addMember(1L, 3L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Room is full");
        }

        @Test
        @DisplayName("Should fail when user already in room")
        void shouldFailWhenUserAlreadyInRoom() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(1L, 2L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> roomService.addMember(1L, 2L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User already in room");
        }
    }

    @Nested
    @DisplayName("Remove Member Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member successfully")
        void shouldRemoveMemberSuccessfully() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 2L))
                    .thenReturn(Optional.of(regularMember));
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(regularMember);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When
            roomService.removeMember(1L, 2L, 1L);

            // Then
            verify(roomMemberRepository).save(argThat(member ->
                !member.getIsActive() && member.getLeftAt() != null
            ));
        }

        @Test
        @DisplayName("Should fail when trying to remove owner")
        void shouldFailWhenRemovingOwner() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));

            // When & Then
            assertThatThrownBy(() -> roomService.removeMember(1L, 1L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cannot remove room owner");
        }

        @Test
        @DisplayName("Should fail when regular member tries to remove")
        void shouldFailWhenRegularMemberRemoves() {
            // Given
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 2L))
                    .thenReturn(Optional.of(regularMember));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 3L))
                    .thenReturn(Optional.of(RoomMemberEntity.builder()
                            .userId(3L).role(RoomMemberEntity.MemberRole.MEMBER).build()));

            // When & Then
            assertThatThrownBy(() -> roomService.removeMember(1L, 3L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No permission to remove members");
        }
    }

    @Nested
    @DisplayName("Leave Room Tests")
    class LeaveRoomTests {

        @Test
        @DisplayName("Should leave room successfully")
        void shouldLeaveRoomSuccessfully() {
            // Given
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 2L))
                    .thenReturn(Optional.of(regularMember));
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(regularMember);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When
            roomService.leaveRoom(1L, 2L);

            // Then
            verify(roomMemberRepository).save(argThat(member ->
                !member.getIsActive() && member.getLeftAt() != null
            ));
        }

        @Test
        @DisplayName("Should transfer ownership when owner leaves")
        void shouldTransferOwnershipWhenOwnerLeaves() {
            // Given
            RoomMemberEntity adminMember = RoomMemberEntity.builder()
                    .id(3L)
                    .roomId(1L)
                    .userId(3L)
                    .role(RoomMemberEntity.MemberRole.ADMIN)
                    .isActive(true)
                    .build();

            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndRole(1L, RoomMemberEntity.MemberRole.ADMIN))
                    .thenReturn(List.of(adminMember));
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);

            // When
            roomService.leaveRoom(1L, 1L);

            // Then
            verify(roomMemberRepository, times(2)).save(any(RoomMemberEntity.class));
        }
    }

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            // Given
            GroupMessageEntity message = GroupMessageEntity.builder()
                    .id(1L)
                    .roomId(1L)
                    .senderId(1L)
                    .content("Hello")
                    .build();

            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMessageRepository.save(any(GroupMessageEntity.class))).thenReturn(message);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);
            when(roomMemberRepository.findByRoomIdAndIsActiveTrue(1L))
                    .thenReturn(List.of(ownerMember, regularMember));

            // When
            GroupMessageEntity result = roomService.sendMessage(1L, 1L, "Hello", null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello");
            verify(roomMemberRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should fail when user is muted")
        void shouldFailWhenUserIsMuted() {
            // Given
            ownerMember.setIsMuted(true);
            ownerMember.setMutedUntil(LocalDateTime.now().plusHours(1));

            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));

            // When & Then
            assertThatThrownBy(() -> roomService.sendMessage(1L, 1L, "Hello", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("You are muted");
        }

        @Test
        @DisplayName("Should unmute when mute period expired")
        void shouldUnmuteWhenExpired() {
            // Given
            ownerMember.setIsMuted(true);
            ownerMember.setMutedUntil(LocalDateTime.now().minusMinutes(1));

            GroupMessageEntity message = GroupMessageEntity.builder()
                    .id(1L).roomId(1L).senderId(1L).content("Hello").build();

            when(roomRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(testRoom));
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(ownerMember);
            when(groupMessageRepository.save(any(GroupMessageEntity.class))).thenReturn(message);
            when(roomRepository.save(any(RoomEntity.class))).thenReturn(testRoom);
            when(roomMemberRepository.findByRoomIdAndIsActiveTrue(1L))
                    .thenReturn(List.of(ownerMember));

            // When
            GroupMessageEntity result = roomService.sendMessage(1L, 1L, "Hello", null);

            // Then
            assertThat(result).isNotNull();
            verify(roomMemberRepository).save(argThat(member -> !member.getIsMuted()));
        }
    }

    @Nested
    @DisplayName("Get Room Messages Tests")
    class GetRoomMessagesTests {

        @Test
        @DisplayName("Should get room messages successfully")
        void shouldGetRoomMessagesSuccessfully() {
            // Given
            List<GroupMessageEntity> messages = List.of(
                    GroupMessageEntity.builder().id(1L).content("Hi").build(),
                    GroupMessageEntity.builder().id(2L).content("Hello").build()
            );

            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 1L))
                    .thenReturn(Optional.of(ownerMember));
            when(groupMessageRepository.findTop100ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(1L))
                    .thenReturn(messages);

            // When
            List<GroupMessageEntity> result = roomService.getRoomMessages(1L, 1L);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should fail when not a member")
        void shouldFailWhenNotMember() {
            // Given
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.getRoomMessages(1L, 999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not a member of this room");
        }
    }

    @Nested
    @DisplayName("Mark As Read Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark as read successfully")
        void shouldMarkAsReadSuccessfully() {
            // Given
            regularMember.setUnreadCount(5);
            when(roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(1L, 2L))
                    .thenReturn(Optional.of(regularMember));
            when(roomMemberRepository.save(any(RoomMemberEntity.class))).thenReturn(regularMember);

            // When
            roomService.markAsRead(1L, 2L);

            // Then
            verify(roomMemberRepository).save(argThat(member ->
                member.getUnreadCount() == 0
            ));
        }
    }

    @Nested
    @DisplayName("Get User Rooms Tests")
    class GetUserRoomsTests {

        @Test
        @DisplayName("Should get user rooms successfully")
        void shouldGetUserRoomsSuccessfully() {
            // Given
            when(roomMemberRepository.findByUserIdAndIsActiveTrue(1L))
                    .thenReturn(List.of(ownerMember));
            when(roomRepository.findByIdAndIsActiveTrue(1L))
                    .thenReturn(Optional.of(testRoom));

            // When
            List<RoomEntity> result = roomService.getUserRooms(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoomName()).isEqualTo("Test Room");
        }
    }

    @Nested
    @DisplayName("Search Rooms Tests")
    class SearchRoomsTests {

        @Test
        @DisplayName("Should search rooms successfully")
        void shouldSearchRoomsSuccessfully() {
            // Given
            when(roomRepository.searchRooms("test")).thenReturn(List.of(testRoom));

            // When
            List<RoomEntity> result = roomService.searchRooms("test");

            // Then
            assertThat(result).hasSize(1);
        }
    }
}
