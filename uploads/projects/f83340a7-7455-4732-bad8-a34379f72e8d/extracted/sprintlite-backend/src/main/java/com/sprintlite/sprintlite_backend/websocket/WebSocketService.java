package com.sprintlite.sprintlite_backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendToUser(String userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
    }
    
    public void sendToTopic(String topic, Object payload) {
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
    }
    
    public void sendTaskUpdate(String taskId, Map<String, Object> update) {
        messagingTemplate.convertAndSend("/topic/tasks/" + taskId, update);
    }
    
    public void sendProjectUpdate(String projectId, Map<String, Object> update) {
        messagingTemplate.convertAndSend("/topic/projects/" + projectId, update);
    }
}