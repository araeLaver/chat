package com.beam.websocket.handler;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.RoomType;
import com.beam.User;
import com.beam.websocket.ChatRoomManager;
import com.beam.websocket.WebSocketMessageSender;
import com.beam.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;

/**
 * 채팅방 관련 메시지 처리
 * - 방 생성/삭제
 * - 입장/퇴장
 * - DM 생성
 */
@Component
public class RoomMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(RoomMessageHandler.class);

    @Autowired
    private ChatRoomManager roomManager;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private WebSocketMessageSender messageSender;

    public void handleJoinRoom(WebSocketSession session, ChatMessage message) throws Exception {
        String roomId = message.getRoomId();
        if (roomId == null) roomId = "general";

        leaveCurrentRoom(session);
        joinRoom(session, roomId, message.getSender());
    }

    public void handleCreateRoom(WebSocketSession session, ChatMessage message) throws Exception {
        try {
            String roomName = message.getRoomName();
            String creator = message.getCreator();
            String description = message.getDescription();

            if (roomName == null || roomName.trim().isEmpty()) {
                messageSender.sendErrorMessage(session, "방 이름을 입력하세요.");
                return;
            }

            if (roomManager.isRoomNameDuplicate(roomName.trim())) {
                messageSender.sendErrorMessage(session, "이미 존재하는 방 이름입니다. 다른 이름을 사용해주세요.");
                return;
            }

            String roomId = roomManager.generateGroupRoomId();
            roomManager.createRoom(roomId, roomName.trim(), RoomType.GROUP, creator, description);

            logger.info("New room created: {} (GROUP) by {}", roomName, creator);

            messageSender.broadcastRoomListToAll();
            joinRoom(session, roomId, creator);
            messageSender.sendSuccessMessage(session, "방 '" + roomName + "'이 성공적으로 생성되었습니다!");

        } catch (Exception e) {
            logger.error("Room creation error: {}", e.getMessage(), e);
            messageSender.sendErrorMessage(session, "방 생성 중 오류가 발생했습니다.");
        }
    }

    public void handleCreateDirectMessage(WebSocketSession session, ChatMessage message) throws Exception {
        try {
            Long userId = message.getUserId();
            Long friendId = message.getFriendId();
            String username = message.getSender();

            if (userId == null || friendId == null) {
                messageSender.sendErrorMessage(session, "사용자 ID가 필요합니다.");
                return;
            }

            String roomId = roomManager.generateDirectMessageRoomId(userId, friendId);
            ChatRoom existingRoom = roomManager.getRoom(roomId);

            if (existingRoom == null) {
                String roomName = "DM: " + username + " ↔ " + message.getFriendName();
                roomManager.createRoom(roomId, roomName, RoomType.DIRECT);
                logger.info("New DM room created: {}", roomName);
            }

            messageSender.broadcastRoomListToAll();
            leaveCurrentRoom(session);
            joinRoom(session, roomId, username);

            messageSender.sendSuccessMessage(session, "1:1 채팅방에 입장했습니다.",
                "directMessageCreated", roomId);

        } catch (Exception e) {
            logger.error("DM room creation error: {}", e.getMessage(), e);
            messageSender.sendErrorMessage(session, "1:1 채팅방 생성 중 오류가 발생했습니다.");
        }
    }

    public void handleDeleteRoom(WebSocketSession session, ChatMessage message) throws Exception {
        try {
            String roomId = message.getRoomId();
            String requestUser = message.getSender();

            if (roomId == null || roomId.trim().isEmpty()) {
                messageSender.sendErrorMessage(session, "삭제할 방을 지정하세요.");
                return;
            }

            ChatRoom room = roomManager.getRoom(roomId);
            if (room == null) {
                messageSender.sendErrorMessage(session, "존재하지 않는 방입니다.");
                return;
            }

            if (roomManager.isDefaultRoom(roomId)) {
                messageSender.sendErrorMessage(session, "기본 방은 삭제할 수 없습니다.");
                return;
            }

            if (!requestUser.equals(room.getCreator())) {
                messageSender.sendErrorMessage(session, "방장만 방을 삭제할 수 있습니다.");
                return;
            }

            // 방에 있는 모든 사용자를 내보냄
            for (User user : new ArrayList<>(room.getUsers().values())) {
                WebSocketSession userSession = sessionManager.findSessionById(user.getSessionId());
                if (userSession != null) {
                    sessionManager.removeSessionRoom(userSession.getId());
                    messageSender.sendSuccessMessage(userSession,
                        "방이 삭제되었습니다. 로비로 이동합니다.", "roomDeleted", roomId);
                }
            }

            roomManager.deleteRoom(roomId);
            logger.info("Room deleted: {} by {}", room.getRoomName(), requestUser);

            messageSender.broadcastRoomListToAll();
            messageSender.sendSuccessMessage(session, "방이 성공적으로 삭제되었습니다.");

        } catch (Exception e) {
            logger.error("Room deletion error: {}", e.getMessage(), e);
            messageSender.sendErrorMessage(session, "방 삭제 중 오류가 발생했습니다.");
        }
    }

    public void joinRoom(WebSocketSession session, String roomId, String username) throws Exception {
        ChatRoom room = roomManager.getRoom(roomId);
        if (room == null) return;

        User user = new User(session.getId(), username, session.getId());
        sessionManager.addUser(session.getId(), user);
        roomManager.addUserToRoom(roomId, user);
        sessionManager.setSessionRoom(session.getId(), roomId);

        messageSender.sendSystemMessage(roomId,
            username + "님이 " + room.getRoomName() + "에 입장하셨습니다.", "system");
        messageSender.sendRoomUserList(roomId);
    }

    public void leaveCurrentRoom(WebSocketSession session) throws Exception {
        String currentRoomId = sessionManager.getSessionRoom(session.getId());
        if (currentRoomId != null) {
            ChatRoom room = roomManager.getRoom(currentRoomId);
            if (room != null) {
                User user = roomManager.removeUserFromRoom(currentRoomId, session.getId());
                sessionManager.removeSessionRoom(session.getId());

                if (user != null) {
                    messageSender.sendSystemMessage(currentRoomId,
                        user.getUsername() + "님이 " + room.getRoomName() + "에서 퇴장하셨습니다.", "system");
                    messageSender.sendRoomUserList(currentRoomId);
                }
            }
        }
    }
}
