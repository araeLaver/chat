package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Room Service
 *
 * <p>Manages group chat rooms with caching for improved performance.
 * Cache eviction occurs automatically on create/update/delete operations.
 *
 * @since 1.0.0
 */
@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    @CacheEvict(value = "chatRooms", key = "'userRooms:' + #creatorId")
    public RoomEntity createRoom(Long creatorId, String roomName, String description,
                                  RoomEntity.RoomType roomType, Integer maxMembers) {

        // Verify user exists
        userRepository.findById(creatorId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        RoomEntity room = RoomEntity.builder()
            .roomName(roomName)
            .description(description)
            .roomType(roomType != null ? roomType : RoomEntity.RoomType.PUBLIC)
            .createdBy(creatorId)
            .maxMembers(maxMembers != null ? maxMembers : 100)
            .currentMembers(1)
            .isActive(true)
            .build();

        room = roomRepository.save(room);

        RoomMemberEntity creator = RoomMemberEntity.builder()
            .roomId(room.getId())
            .userId(creatorId)
            .role(RoomMemberEntity.MemberRole.OWNER)
            .isActive(true)
            .build();

        roomMemberRepository.save(creator);

        return room;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "chatRooms", key = "'members:' + #roomId"),
        @CacheEvict(value = "chatRooms", key = "'userRooms:' + #userId")
    })
    public RoomEntity updateRoom(Long roomId, Long userId, String roomName,
                                  String description, Integer maxMembers) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        if (member.getRole() != RoomMemberEntity.MemberRole.OWNER &&
            member.getRole() != RoomMemberEntity.MemberRole.ADMIN) {
            throw new RuntimeException("No permission to update room");
        }

        if (roomName != null) room.setRoomName(roomName);
        if (description != null) room.setDescription(description);
        if (maxMembers != null) room.setMaxMembers(maxMembers);

        return roomRepository.save(room);
    }

    @Transactional
    @CacheEvict(value = "chatRooms", allEntries = true)
    public void deleteRoom(Long roomId, Long userId) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        if (member.getRole() != RoomMemberEntity.MemberRole.OWNER) {
            throw new RuntimeException("Only owner can delete room");
        }

        room.setIsActive(false);
        roomRepository.save(room);

        List<RoomMemberEntity> members = roomMemberRepository.findByRoomIdAndIsActiveTrue(roomId);
        members.forEach(m -> {
            m.setIsActive(false);
            m.setLeftAt(LocalDateTime.now());
        });
        roomMemberRepository.saveAll(members);
    }

    @Transactional
    public void addMember(Long roomId, Long userId, Long inviterId) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        RoomMemberEntity inviter = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, inviterId)
            .orElseThrow(() -> new RuntimeException("Inviter not in room"));

        if (roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)) {
            throw new RuntimeException("User already in room");
        }

        if (room.getCurrentMembers() >= room.getMaxMembers()) {
            throw new RuntimeException("Room is full");
        }

        RoomMemberEntity newMember = RoomMemberEntity.builder()
            .roomId(roomId)
            .userId(userId)
            .role(RoomMemberEntity.MemberRole.MEMBER)
            .isActive(true)
            .build();

        roomMemberRepository.save(newMember);

        room.incrementMemberCount();
        roomRepository.save(room);
    }

    @Transactional
    public void removeMember(Long roomId, Long userId, Long removerId) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomMemberEntity remover = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, removerId)
            .orElseThrow(() -> new RuntimeException("Remover not in room"));

        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getRole() == RoomMemberEntity.MemberRole.OWNER) {
            throw new RuntimeException("Cannot remove room owner");
        }

        if (remover.getRole() != RoomMemberEntity.MemberRole.OWNER &&
            remover.getRole() != RoomMemberEntity.MemberRole.ADMIN) {
            throw new RuntimeException("No permission to remove members");
        }

        member.setIsActive(false);
        member.setLeftAt(LocalDateTime.now());
        roomMemberRepository.save(member);

        room.decrementMemberCount();
        roomRepository.save(room);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        if (member.getRole() == RoomMemberEntity.MemberRole.OWNER) {
            List<RoomMemberEntity> admins = roomMemberRepository.findByRoomIdAndRole(
                roomId, RoomMemberEntity.MemberRole.ADMIN);
            if (!admins.isEmpty()) {
                admins.get(0).setRole(RoomMemberEntity.MemberRole.OWNER);
                roomMemberRepository.save(admins.get(0));
            }
        }

        member.setIsActive(false);
        member.setLeftAt(LocalDateTime.now());
        roomMemberRepository.save(member);

        room.decrementMemberCount();
        roomRepository.save(room);
    }

    @Transactional
    @CacheEvict(value = "messages", key = "#roomId")
    public GroupMessageEntity sendMessage(Long roomId, Long senderId, String content,
                                          GroupMessageEntity.MessageType messageType) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomMemberEntity sender = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, senderId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        if (sender.getIsMuted()) {
            if (sender.getMutedUntil() != null && sender.getMutedUntil().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("You are muted");
            } else {
                sender.setIsMuted(false);
                sender.setMutedUntil(null);
                roomMemberRepository.save(sender);
            }
        }

        GroupMessageEntity message = GroupMessageEntity.builder()
            .roomId(roomId)
            .senderId(senderId)
            .content(content)
            .messageType(messageType != null ? messageType : GroupMessageEntity.MessageType.TEXT)
            .timestamp(LocalDateTime.now())
            .readCount(0)
            .build();

        message = groupMessageRepository.save(message);

        room.setLastMessage(content);
        room.setLastMessageTime(message.getTimestamp());
        room.setLastMessageSenderId(senderId);
        roomRepository.save(room);

        List<RoomMemberEntity> members = roomMemberRepository.findByRoomIdAndIsActiveTrue(roomId);
        for (RoomMemberEntity member : members) {
            if (!member.getUserId().equals(senderId)) {
                member.incrementUnreadCount();
            }
        }
        roomMemberRepository.saveAll(members);

        return message;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "messages", key = "#roomId")
    public List<GroupMessageEntity> getRoomMessages(Long roomId, Long userId) {
        roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        return groupMessageRepository.findTop100ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId) {
        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        member.resetUnreadCount();
        roomMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "chatRooms", key = "'userRooms:' + #userId")
    public List<RoomEntity> getUserRooms(Long userId) {
        List<RoomMemberEntity> memberships = roomMemberRepository.findByUserIdAndIsActiveTrue(userId);
        return memberships.stream()
            .map(m -> roomRepository.findByIdAndIsActiveTrue(m.getRoomId()).orElse(null))
            .filter(r -> r != null)
            .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "chatRooms", key = "'members:' + #roomId")
    public List<RoomMemberEntity> getRoomMembers(Long roomId, Long userId) {
        roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> new RuntimeException("Not a member of this room"));

        return roomMemberRepository.findByRoomIdAndIsActiveTrue(roomId);
    }

    @Transactional(readOnly = true)
    public List<RoomEntity> searchRooms(String keyword) {
        return roomRepository.searchRooms(keyword);
    }
}