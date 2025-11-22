package com.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private MessageService messageService;

    @Autowired
    private FriendService friendService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitService rateLimitService;

    public ChatWebSocketHandler() {
        // 기본 그룹 채팅방들
        chatRooms.put("general", new ChatRoom("general", "일반 채팅방", RoomType.GROUP));
        chatRooms.put("tech", new ChatRoom("tech", "개발 이야기", RoomType.GROUP));
        chatRooms.put("casual", new ChatRoom("casual", "자유 토론", RoomType.GROUP));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // WebSocket 인증: Query parameter에서 토큰 추출 및 검증
        String token = extractTokenFromSession(session);

        // 게스트 모드 지원: 토큰이 없거나 "guest"인 경우 허용
        if (token != null && !"guest".equals(token)) {
            if (!jwtUtil.validateToken(token)) {
                System.err.println("Invalid JWT token for session: " + session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
                return;
            }
            // 유효한 토큰이면 사용자 정보를 세션에 저장
            String username = jwtUtil.getUsernameFromToken(token);
            session.getAttributes().put("username", username);
            session.getAttributes().put("userId", jwtUtil.getUserIdFromToken(token));
            System.out.println("Authenticated user connected: " + username);
        } else {
            // 게스트 모드
            System.out.println("Guest user connected: " + session.getId());
        }

        sessions.add(session);
        sendRoomList(session);
        System.out.println("새로운 연결: " + session.getId());
    }

    /**
     * WebSocket 세션에서 JWT 토큰 추출
     * Query parameter 'token' 또는 Sec-WebSocket-Protocol 헤더에서 추출
     */
    private String extractTokenFromSession(WebSocketSession session) {
        // 1. Query parameter에서 추출 시도
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6); // "token=" 제거
                }
            }
        }

        // 2. Handshake headers에서 추출 시도
        List<String> protocols = session.getHandshakeHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            return protocols.get(0);
        }

        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // WebSocket Rate Limiting
            if (!rateLimitService.isWebSocketMessageAllowed(session.getId())) {
                // Send rate limit error message
                ChatMessage errorMessage = new ChatMessage();
                errorMessage.setType("error");
                errorMessage.setContent("Rate limit exceeded. Please slow down.");
                errorMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMessage)));
                System.err.println("Rate limit exceeded for session: " + session.getId());
                return;
            }

            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            if ("joinRoom".equals(chatMessage.getType())) {
                String roomId = chatMessage.getRoomId();
                if (roomId == null) roomId = "general";
                
                leaveCurrentRoom(session);
                joinRoom(session, roomId, chatMessage.getSender());
                
            } else if ("message".equals(chatMessage.getType()) || chatMessage.getType() == null) {
                String roomId = sessionToRoom.get(session.getId());
                if (roomId != null) {
                    chatMessage.setType("message");
                    chatMessage.setRoomId(roomId);
                    chatMessage.setSecurityType(MessageSecurityType.NORMAL);

                    ChatRoom room = chatRooms.get(roomId);
                    if (room != null) {
                        messageService.saveMessage(chatMessage);
                        broadcastToRoom(roomId, chatMessage);
                    }
                }
            } else if ("file".equals(chatMessage.getType())) {
                String roomId = sessionToRoom.get(session.getId());
                if (roomId != null) {
                    chatMessage.setRoomId(roomId);
                    
                    messageService.saveMessage(chatMessage);
                    broadcastToRoom(roomId, chatMessage);
                }
            } else if ("getHistory".equals(chatMessage.getType())) {
                String roomId = chatMessage.getRoomId();
                if (roomId != null) {
                    sendMessageHistory(session, roomId);
                    // 메시지 히스토리를 불러온 후 읽음 처리
                    if (chatMessage.getUserId() != null) {
                        messageService.markRoomMessagesAsRead(roomId, chatMessage.getUserId(), chatMessage.getSender());
                    }
                }
            } else if ("markAsRead".equals(chatMessage.getType())) {
                // 채팅방 입장 시 읽음 처리
                String roomId = chatMessage.getRoomId();
                Long userId = chatMessage.getUserId();
                String username = chatMessage.getSender();
                if (roomId != null && userId != null) {
                    messageService.markRoomMessagesAsRead(roomId, userId, username);
                    // 읽음 상태 업데이트를 전체에 브로드캐스트
                    ChatMessage readUpdate = new ChatMessage("시스템",
                        username + "님이 메시지를 읽었습니다.",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        "readUpdate");
                    readUpdate.setRoomId(roomId);
                    broadcastToRoom(roomId, readUpdate);
                }
            } else if ("createRoom".equals(chatMessage.getType())) {
                handleCreateRoom(session, chatMessage);
            } else if ("createDirectMessage".equals(chatMessage.getType())) {
                handleCreateDirectMessage(session, chatMessage);
            } else if ("deleteRoom".equals(chatMessage.getType())) {
                handleDeleteRoom(session, chatMessage);
            }
            
            System.out.println("메시지 처리: " + chatMessage.getSender() + " - " + chatMessage.getContent());
            
        } catch (Exception e) {
            System.err.println("메시지 처리 오류: " + e.getMessage());
        }
    }

    private void joinRoom(WebSocketSession session, String roomId, String username) throws Exception {
        ChatRoom room = chatRooms.get(roomId);
        if (room == null) return;
        
        User user = new User(session.getId(), username, session.getId());
        users.put(session.getId(), user);
        room.addUser(user);
        sessionToRoom.put(session.getId(), roomId);
        
        ChatMessage joinMessage = new ChatMessage("시스템", 
            username + "님이 " + room.getRoomName() + "에 입장하셨습니다.", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
            "system");
        joinMessage.setRoomId(roomId);
        
        broadcastToRoom(roomId, joinMessage);
        sendRoomUserList(roomId);
        sendMessageHistory(session, roomId);
    }

    private void leaveCurrentRoom(WebSocketSession session) throws Exception {
        String currentRoomId = sessionToRoom.get(session.getId());
        if (currentRoomId != null) {
            ChatRoom room = chatRooms.get(currentRoomId);
            User user = room.removeUser(session.getId());
            sessionToRoom.remove(session.getId());
            
            if (user != null) {
                ChatMessage leaveMessage = new ChatMessage("시스템", 
                    user.getUsername() + "님이 " + room.getRoomName() + "에서 퇴장하셨습니다.", 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                    "system");
                leaveMessage.setRoomId(currentRoomId);
                
                broadcastToRoom(currentRoomId, leaveMessage);
                sendRoomUserList(currentRoomId);
            }
        }
    }

    private void broadcastToRoom(String roomId, ChatMessage message) throws Exception {
        ChatRoom room = chatRooms.get(roomId);
        if (room == null) return;
        
        String messageJson = objectMapper.writeValueAsString(message);
        for (User user : room.getUsers().values()) {
            WebSocketSession userSession = findSessionById(user.getSessionId());
            if (userSession != null && userSession.isOpen()) {
                userSession.sendMessage(new TextMessage(messageJson));
            }
        }
    }

    private void sendRoomUserList(String roomId) throws Exception {
        ChatRoom room = chatRooms.get(roomId);
        if (room == null) return;
        
        ChatMessage userListMessage = new ChatMessage("시스템", 
            objectMapper.writeValueAsString(room.getUsers().values()), 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
            "userlist");
        userListMessage.setRoomId(roomId);
        
        broadcastToRoom(roomId, userListMessage);
    }

    private void sendRoomList(WebSocketSession session) throws Exception {
        // 방 정보를 더 자세히 포함하는 객체 생성
        List<Map<String, Object>> roomDetails = new ArrayList<>();
        
        for (ChatRoom room : chatRooms.values()) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", room.getRoomId());
            roomInfo.put("roomName", room.getRoomName());
            roomInfo.put("roomType", room.getRoomType().toString());
            roomInfo.put("userCount", room.getUserCount());
            roomInfo.put("creator", room.getCreator());
            roomInfo.put("description", room.getDescription());
            roomInfo.put("isDirectMessage", room.isDirectMessage());

            roomDetails.add(roomInfo);
        }
        
        ChatMessage roomListMessage = new ChatMessage("시스템", 
            objectMapper.writeValueAsString(roomDetails), 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
            "roomlist");
            
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(roomListMessage)));
        }
    }

    private void sendMessageHistory(WebSocketSession session, String roomId) throws Exception {
        List<MessageEntity> messages = messageService.getRecentMessages(roomId);
        ChatRoom room = chatRooms.get(roomId);
        
        for (MessageEntity msg : messages) {
            ChatMessage historyMessage = new ChatMessage(
                msg.getSender(), 
                msg.getContent(), 
                msg.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                msg.getMessageType()
            );
            historyMessage.setRoomId(roomId);
            historyMessage.setSecurityType(msg.getSecurityType());

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(historyMessage)));
            }
        }
    }

    private WebSocketSession findSessionById(String sessionId) {
        return sessions.stream()
            .filter(session -> session.getId().equals(sessionId))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        leaveCurrentRoom(session);
        users.remove(session.getId());

        // Clean up rate limiter for this session
        rateLimitService.removeWebSocketLimiter(session.getId());

        System.out.println("연결 종료: " + session.getId());
    }
    

    private void handleCreateRoom(WebSocketSession session, ChatMessage message) throws Exception {
        try {
            String roomName = message.getRoomName();
            String creator = message.getCreator();
            String description = message.getDescription();

            if (roomName == null || roomName.trim().isEmpty()) {
                sendErrorMessage(session, "방 이름을 입력하세요.");
                return;
            }

            // 방 이름 중복 체크
            if (isRoomNameDuplicate(roomName.trim())) {
                sendErrorMessage(session, "이미 존재하는 방 이름입니다. 다른 이름을 사용해주세요.");
                return;
            }

            // 고유한 방 ID 생성
            String roomId = "group_" + System.currentTimeMillis();

            // 그룹 채팅방 생성
            ChatRoom newRoom = new ChatRoom(roomId, roomName.trim(), RoomType.GROUP, creator, description);
            chatRooms.put(roomId, newRoom);
            
            System.out.println("새 방 생성: " + roomName + " (GROUP) by " + creator);
            
            // 전체 사용자에게 방 목록 업데이트 전송
            broadcastRoomListUpdate();
            
            // 생성자를 자동으로 방에 입장시킴
            joinRoom(session, roomId, creator);
            
            // 성공 메시지 전송
            ChatMessage successMessage = new ChatMessage("시스템", 
                "방 '" + roomName + "'이 성공적으로 생성되었습니다!", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                "success");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(successMessage)));
            
        } catch (Exception e) {
            System.err.println("방 생성 오류: " + e.getMessage());
            sendErrorMessage(session, "방 생성 중 오류가 발생했습니다.");
        }
    }


    private void broadcastRoomListUpdate() throws Exception {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendRoomList(session);
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws Exception {
        ChatMessage error = new ChatMessage("시스템", errorMessage, 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), "error");
        
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        }
    }
    
    private boolean isRoomNameDuplicate(String roomName) {
        return chatRooms.values().stream()
            .anyMatch(room -> room.getRoomName().equals(roomName));
    }

    /**
     * 1:1 다이렉트 메시지 채팅방 생성 또는 가져오기
     */
    private void handleCreateDirectMessage(WebSocketSession session, ChatMessage message) throws Exception {
        try {
            Long userId = message.getUserId();
            Long friendId = message.getFriendId();
            String username = message.getSender();

            if (userId == null || friendId == null) {
                sendErrorMessage(session, "사용자 ID가 필요합니다.");
                return;
            }

            // 친구 관계 확인 (선택적 - 친구가 아니어도 DM 가능하게 하려면 주석 처리)
            // if (!friendService.areFriends(userId, friendId)) {
            //     sendErrorMessage(session, "친구 관계가 아닙니다.");
            //     return;
            // }

            // 1:1 채팅방 ID 생성 (작은 ID가 앞에 오도록)
            String roomId = createDirectMessageRoomId(userId, friendId);

            // 이미 존재하는 채팅방인지 확인
            ChatRoom existingRoom = chatRooms.get(roomId);

            if (existingRoom == null) {
                // 새로운 1:1 채팅방 생성
                String roomName = "DM: " + username + " ↔ " + message.getFriendName();
                ChatRoom newRoom = new ChatRoom(roomId, roomName, RoomType.DIRECT);
                chatRooms.put(roomId, newRoom);

                System.out.println("새 1:1 채팅방 생성: " + roomName);
            }

            // 전체 사용자에게 방 목록 업데이트 전송
            broadcastRoomListUpdate();

            // 생성자를 자동으로 방에 입장시킴
            leaveCurrentRoom(session);
            joinRoom(session, roomId, username);

            // 성공 메시지 전송
            ChatMessage successMessage = new ChatMessage("시스템",
                "1:1 채팅방에 입장했습니다.",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                "directMessageCreated");
            successMessage.setRoomId(roomId);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(successMessage)));

        } catch (Exception e) {
            System.err.println("1:1 채팅방 생성 오류: " + e.getMessage());
            sendErrorMessage(session, "1:1 채팅방 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 두 사용자 ID로 고유한 1:1 채팅방 ID 생성
     * 작은 ID가 항상 앞에 오도록 정렬
     */
    private String createDirectMessageRoomId(Long userId1, Long userId2) {
        long smaller = Math.min(userId1, userId2);
        long larger = Math.max(userId1, userId2);
        return "dm_" + smaller + "_" + larger;
    }
    
    private void handleDeleteRoom(WebSocketSession session, ChatMessage message) throws Exception {
        try {
            String roomId = message.getRoomId();
            String requestUser = message.getSender();
            
            if (roomId == null || roomId.trim().isEmpty()) {
                sendErrorMessage(session, "삭제할 방을 지정하세요.");
                return;
            }
            
            ChatRoom room = chatRooms.get(roomId);
            if (room == null) {
                sendErrorMessage(session, "존재하지 않는 방입니다.");
                return;
            }
            
            // 기본 방은 삭제 불가
            if (roomId.equals("general") || roomId.equals("tech") || roomId.equals("casual")) {
                sendErrorMessage(session, "기본 방은 삭제할 수 없습니다.");
                return;
            }
            
            // 방장만 삭제 가능
            if (!requestUser.equals(room.getCreator())) {
                sendErrorMessage(session, "방장만 방을 삭제할 수 있습니다.");
                return;
            }
            
            // 방에 있는 모든 사용자를 내보냄
            for (User user : new ArrayList<>(room.getUsers().values())) {
                WebSocketSession userSession = findSessionById(user.getSessionId());
                if (userSession != null) {
                    sessionToRoom.remove(userSession.getId());
                    
                    // 삭제 알림 메시지 전송
                    ChatMessage deleteMessage = new ChatMessage("시스템", 
                        "방이 삭제되었습니다. 로비로 이동합니다.", 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                        "roomDeleted");
                    deleteMessage.setRoomId(roomId);
                    
                    if (userSession.isOpen()) {
                        userSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(deleteMessage)));
                    }
                }
            }
            
            // 방 삭제
            chatRooms.remove(roomId);
            
            System.out.println("방 삭제: " + room.getRoomName() + " by " + requestUser);
            
            // 모든 사용자에게 방 목록 업데이트
            broadcastRoomListUpdate();
            
            // 성공 메시지 전송
            ChatMessage successMessage = new ChatMessage("시스템", 
                "방이 성공적으로 삭제되었습니다.", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), 
                "success");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(successMessage)));
            
        } catch (Exception e) {
            System.err.println("방 삭제 오류: " + e.getMessage());
            sendErrorMessage(session, "방 삭제 중 오류가 발생했습니다.");
        }
    }
}