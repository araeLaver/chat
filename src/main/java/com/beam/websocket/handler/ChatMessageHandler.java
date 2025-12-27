package com.beam.websocket.handler;

import com.beam.ChatMessage;
import com.beam.ChatRoom;
import com.beam.MessageEntity;
import com.beam.MessageSecurityType;
import com.beam.MessageService;
import com.beam.websocket.ChatRoomManager;
import com.beam.websocket.WebSocketMessageSender;
import com.beam.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 채팅 메시지 처리
 * - 일반 메시지
 * - 파일 메시지
 * - 메시지 히스토리
 * - 읽음 처리
 */
@Component
public class ChatMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageHandler.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatRoomManager roomManager;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private WebSocketMessageSender messageSender;

    public void handleTextMessage(WebSocketSession session, ChatMessage chatMessage) throws Exception {
        String roomId = sessionManager.getSessionRoom(session.getId());
        if (roomId != null) {
            chatMessage.setType("message");
            chatMessage.setRoomId(roomId);
            chatMessage.setSecurityType(MessageSecurityType.NORMAL);

            ChatRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                messageService.saveMessage(chatMessage);
                messageSender.broadcastToRoom(roomId, chatMessage);
            }
        }
    }

    public void handleFileMessage(WebSocketSession session, ChatMessage chatMessage) throws Exception {
        String roomId = sessionManager.getSessionRoom(session.getId());
        if (roomId != null) {
            chatMessage.setRoomId(roomId);
            messageService.saveMessage(chatMessage);
            messageSender.broadcastToRoom(roomId, chatMessage);
        }
    }

    public void handleGetHistory(WebSocketSession session, ChatMessage chatMessage) throws Exception {
        String roomId = chatMessage.getRoomId();
        if (roomId != null) {
            sendMessageHistory(session, roomId);

            if (chatMessage.getUserId() != null) {
                messageService.markRoomMessagesAsRead(roomId, chatMessage.getUserId(), chatMessage.getSender());
            }
        }
    }

    public void handleMarkAsRead(WebSocketSession session, ChatMessage chatMessage) throws Exception {
        String roomId = chatMessage.getRoomId();
        Long userId = chatMessage.getUserId();
        String username = chatMessage.getSender();

        if (roomId != null && userId != null) {
            messageService.markRoomMessagesAsRead(roomId, userId, username);

            ChatMessage readUpdate = new ChatMessage("시스템",
                username + "님이 메시지를 읽었습니다.",
                messageSender.getCurrentTimestamp(),
                "readUpdate");
            readUpdate.setRoomId(roomId);
            messageSender.broadcastToRoom(roomId, readUpdate);
        }
    }

    public void sendMessageHistory(WebSocketSession session, String roomId) throws Exception {
        List<MessageEntity> messages = messageService.getRecentMessages(roomId);

        for (MessageEntity msg : messages) {
            ChatMessage historyMessage = new ChatMessage(
                msg.getSender(),
                msg.getContent(),
                msg.getTimestamp().format(TIME_FORMATTER),
                msg.getMessageType()
            );
            historyMessage.setRoomId(roomId);
            historyMessage.setSecurityType(msg.getSecurityType());

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(historyMessage)));
            }
        }
    }
}
