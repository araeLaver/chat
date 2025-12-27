package com.beam.websocket;

import com.beam.ChatRoom;
import com.beam.RoomType;
import com.beam.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅방 관리
 * 방 생성, 삭제, 입장, 퇴장 관리
 */
@Component
public class ChatRoomManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatRoomManager.class);

    private final Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 기본 그룹 채팅방들
        chatRooms.put("general", new ChatRoom("general", "일반 채팅방", RoomType.GROUP));
        chatRooms.put("tech", new ChatRoom("tech", "개발 이야기", RoomType.GROUP));
        chatRooms.put("casual", new ChatRoom("casual", "자유 토론", RoomType.GROUP));
        logger.info("Default chat rooms initialized");
    }

    public ChatRoom getRoom(String roomId) {
        return chatRooms.get(roomId);
    }

    public Collection<ChatRoom> getAllRooms() {
        return chatRooms.values();
    }

    public boolean roomExists(String roomId) {
        return chatRooms.containsKey(roomId);
    }

    public boolean isRoomNameDuplicate(String roomName) {
        return chatRooms.values().stream()
            .anyMatch(room -> room.getRoomName().equals(roomName));
    }

    public boolean isDefaultRoom(String roomId) {
        return "general".equals(roomId) || "tech".equals(roomId) || "casual".equals(roomId);
    }

    public ChatRoom createRoom(String roomId, String roomName, RoomType roomType,
                               String creator, String description) {
        ChatRoom newRoom = new ChatRoom(roomId, roomName, roomType, creator, description);
        chatRooms.put(roomId, newRoom);
        logger.info("Room created: {} ({}) by {}", roomName, roomType, creator);
        return newRoom;
    }

    public ChatRoom createRoom(String roomId, String roomName, RoomType roomType) {
        ChatRoom newRoom = new ChatRoom(roomId, roomName, roomType);
        chatRooms.put(roomId, newRoom);
        logger.info("Room created: {} ({})", roomName, roomType);
        return newRoom;
    }

    public void deleteRoom(String roomId) {
        ChatRoom removed = chatRooms.remove(roomId);
        if (removed != null) {
            logger.info("Room deleted: {}", removed.getRoomName());
        }
    }

    public void addUserToRoom(String roomId, User user) {
        ChatRoom room = chatRooms.get(roomId);
        if (room != null) {
            room.addUser(user);
        }
    }

    public User removeUserFromRoom(String roomId, String sessionId) {
        ChatRoom room = chatRooms.get(roomId);
        if (room != null) {
            return room.removeUser(sessionId);
        }
        return null;
    }

    public String generateGroupRoomId() {
        return "group_" + System.currentTimeMillis();
    }

    public String generateDirectMessageRoomId(Long userId1, Long userId2) {
        long smaller = Math.min(userId1, userId2);
        long larger = Math.max(userId1, userId2);
        return "dm_" + smaller + "_" + larger;
    }
}
