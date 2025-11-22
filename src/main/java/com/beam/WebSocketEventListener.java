package com.beam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    @Autowired(required = false)
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (userId != null) {
            sessionUserMap.put(sessionId, userId);

            userRepository.findById(userId).ifPresent(user -> {
                user.setIsOnline(true);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);

                if (messagingTemplate != null) {
                    Map<String, Object> statusUpdate = Map.of(
                        "userId", userId,
                        "isOnline", true,
                        "timestamp", LocalDateTime.now().toString()
                    );
                    messagingTemplate.convertAndSend("/topic/user-status", statusUpdate);
                }
            });
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setIsOnline(false);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);

                if (messagingTemplate != null) {
                    Map<String, Object> statusUpdate = Map.of(
                        "userId", userId,
                        "isOnline", false,
                        "lastSeen", LocalDateTime.now().toString()
                    );
                    messagingTemplate.convertAndSend("/topic/user-status", statusUpdate);
                }
            });
        }
    }
}