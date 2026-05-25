package com.sprintlite.sprintlite_backend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    
    private final SimpMessageSendingOperations messagingTemplate;
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("New WebSocket connection: {}", sessionId);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = activeSessions.remove(sessionId);
        
        if (userId != null) {
            log.info("User {} disconnected", userId);
            messagingTemplate.convertAndSend("/topic/presence", Map.of("userId", userId, "status", "offline"));
        }
    }
    
    public void registerSession(String sessionId, String userId) {
        activeSessions.put(sessionId, userId);
        messagingTemplate.convertAndSend("/topic/presence", Map.of("userId", userId, "status", "online"));
    }
}