package com.beam;

import com.beam.exception.RoomException;
import com.beam.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final MessageEncryptionService encryptionService;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = "chatRooms", key = "'userRooms:' + #creatorId")
    public RoomEntity createRoom(Long creatorId, String roomName, String description,
                                  RoomEntity.RoomType roomType, Integer maxMembers) {

        // Verify user exists
        userRepository.findById(creatorId)
            .orElseThrow(() -> UserException.notFound(creatorId));

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
            .orElseThrow(() -> RoomException.notFound(roomId));

        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        if (member.getRole() != RoomMemberEntity.MemberRole.OWNER &&
            member.getRole() != RoomMemberEntity.MemberRole.ADMIN) {
            throw RoomException.permissionDenied(roomId);
        }

        if (roomName != null) room.setRoomName(roomName);
        if (description != null) room.setDescription(description);
        if (maxMembers != null) room.setMaxMembers(maxMembers);

        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> RoomException.notFound(roomId));

        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        if (member.getRole() != RoomMemberEntity.MemberRole.OWNER) {
            throw RoomException.ownerOnly(roomId);
        }

        room.setIsActive(false);
        roomRepository.save(room);

        // 캐시 무효화를 위해 먼저 영향받는 사용자 ID 조회
        List<Long> affectedUserIds = roomMemberRepository.findActiveUserIdsByRoomId(roomId);

        // 한 번의 쿼리로 모든 멤버 비활성화 (N+1 방지)
        roomMemberRepository.deactivateAllMembersByRoomId(roomId, LocalDateTime.now());

        // 영향받는 사용자의 캐시만 선택적으로 삭제 (thundering herd 방지)
        evictRoomCaches(roomId, affectedUserIds);
    }

    /**
     * 방 관련 캐시를 선택적으로 삭제 (allEntries=true 대신 사용)
     * Cache stampede (thundering herd) 방지를 위해 영향받는 키만 삭제
     */
    private void evictRoomCaches(Long roomId, List<Long> userIds) {
        if (cacheManager == null) {
            return;
        }

        // chatRooms 캐시에서 관련 키만 삭제
        Cache chatRoomsCache = cacheManager.getCache("chatRooms");
        if (chatRoomsCache != null) {
            // 방 멤버 캐시 삭제
            chatRoomsCache.evict("members:" + roomId);

            // 영향받는 사용자의 방 목록 캐시만 삭제
            for (Long userId : userIds) {
                chatRoomsCache.evict("userRooms:" + userId);
            }
        }

        // messages 캐시에서 해당 방 메시지 캐시 삭제
        Cache messagesCache = cacheManager.getCache("messages");
        if (messagesCache != null) {
            messagesCache.evict(roomId);
        }
    }

    @Transactional
    public void addMember(Long roomId, Long userId, Long inviterId) {
        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> RoomException.notFound(roomId));

        userRepository.findById(userId)
            .orElseThrow(() -> UserException.notFound(userId));

        RoomMemberEntity inviter = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, inviterId)
            .orElseThrow(() -> RoomException.notMember(roomId, inviterId));

        if (roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)) {
            throw RoomException.alreadyMember(roomId, userId);
        }

        if (room.getCurrentMembers() >= room.getMaxMembers()) {
            throw RoomException.roomFull(roomId);
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
            .orElseThrow(() -> RoomException.notFound(roomId));

        RoomMemberEntity remover = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, removerId)
            .orElseThrow(() -> RoomException.notMember(roomId, removerId));

        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        if (member.getRole() == RoomMemberEntity.MemberRole.OWNER) {
            throw RoomException.cannotRemoveOwner(roomId);
        }

        if (remover.getRole() != RoomMemberEntity.MemberRole.OWNER &&
            remover.getRole() != RoomMemberEntity.MemberRole.ADMIN) {
            throw RoomException.permissionDenied(roomId);
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
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        RoomEntity room = roomRepository.findByIdAndIsActiveTrue(roomId)
            .orElseThrow(() -> RoomException.notFound(roomId));

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
            .orElseThrow(() -> RoomException.notFound(roomId));

        RoomMemberEntity sender = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, senderId)
            .orElseThrow(() -> RoomException.notMember(roomId, senderId));

        if (sender.getIsMuted()) {
            if (sender.getMutedUntil() != null && sender.getMutedUntil().isAfter(LocalDateTime.now())) {
                throw RoomException.userMuted(roomId, senderId);
            } else {
                sender.setIsMuted(false);
                sender.setMutedUntil(null);
                roomMemberRepository.save(sender);
            }
        }

        // 암호화 적용
        String messageContent = content;
        boolean isEncrypted = false;
        if (encryptionService.isEncryptionEnabled() && content != null) {
            messageContent = encryptionService.encryptGroupMessage(content, roomId);
            isEncrypted = true;
        }

        GroupMessageEntity message = GroupMessageEntity.builder()
            .roomId(roomId)
            .senderId(senderId)
            .content(messageContent)
            .messageType(messageType != null ? messageType : GroupMessageEntity.MessageType.TEXT)
            .timestamp(LocalDateTime.now())
            .readCount(0)
            .isEncrypted(isEncrypted)
            .securityType(MessageSecurityType.NORMAL)
            .build();

        message = groupMessageRepository.save(message);

        // 미리보기는 원본 저장 (UI 표시용)
        String preview = content != null && content.length() > 50
            ? content.substring(0, 50) + "..."
            : content;
        room.setLastMessage(preview);
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
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        List<GroupMessageEntity> messages = groupMessageRepository
            .findTop100ByRoomIdAndIsDeletedFalseOrderByTimestampDesc(roomId);

        // 메시지 복호화
        decryptGroupMessages(messages, roomId);

        return messages;
    }

    private void decryptGroupMessages(List<GroupMessageEntity> messages, Long roomId) {
        for (GroupMessageEntity msg : messages) {
            if (Boolean.TRUE.equals(msg.getIsEncrypted())) {
                String decrypted = encryptionService.decryptGroupMessage(
                    msg.getContent(), roomId, true);
                msg.setContent(decrypted);
            }
        }
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId) {
        RoomMemberEntity member = roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        member.resetUnreadCount();
        roomMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "chatRooms", key = "'userRooms:' + #userId")
    public List<RoomEntity> getUserRooms(Long userId) {
        // N+1 쿼리 최적화: 서브쿼리로 한 번에 조회
        return roomRepository.findUserRooms(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "chatRooms", key = "'members:' + #roomId")
    public List<RoomMemberEntity> getRoomMembers(Long roomId, Long userId) {
        roomMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)
            .orElseThrow(() -> RoomException.notMember(roomId, userId));

        return roomMemberRepository.findByRoomIdAndIsActiveTrue(roomId);
    }

    @Transactional(readOnly = true)
    public List<RoomEntity> searchRooms(String keyword) {
        return roomRepository.searchRooms(keyword);
    }
}