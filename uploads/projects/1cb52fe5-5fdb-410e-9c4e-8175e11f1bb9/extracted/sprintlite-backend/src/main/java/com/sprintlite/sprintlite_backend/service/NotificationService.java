package com.sprintlite.sprintlite_backend.service;



import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlite.sprintlite_backend.domain.dto.response.NotificationResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Company;
import com.sprintlite.sprintlite_backend.domain.entity.Notification;
import com.sprintlite.sprintlite_backend.domain.entity.Project;
import com.sprintlite.sprintlite_backend.domain.entity.Task;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.mapper.NotificationMapper;
import com.sprintlite.sprintlite_backend.domain.repository.NotificationRepository;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityUtils securityUtils;
    
    // Existing methods...
    
    @Transactional
    public void sendProjectInvitationNotification(Project project, User invitedUser, User invitedBy) {
        Notification notification = Notification.builder()
                .user(invitedUser)
                .company(project.getCompany())
                .type("PROJECT_INVITATION")
                .title("You've been invited to a project")
                .content(invitedBy.getFullName() + " invited you to join project: " + project.getName())
                .isRead(false)
                .isArchived(false)
                .metadata(String.format("{\"projectId\":\"%s\", \"projectName\":\"%s\"}", 
                        project.getId(), project.getName()))
                .build();
        
        notification = notificationRepository.save(notification);
        
        // Send realtime notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                invitedUser.getId().toString(),
                "/queue/notifications",
                notificationMapper.toResponse(notification)
        );
    }
    
    @Transactional
    public void sendProjectJoinedNotification(Project project, User user) {
        Notification notification = Notification.builder()
                .user(user)
                .company(project.getCompany())
                .type("PROJECT_JOINED")
                .title("Welcome to the project!")
                .content("You have successfully joined project: " + project.getName())
                .isRead(false)
                .isArchived(false)
                .metadata(String.format("{\"projectId\":\"%s\", \"projectName\":\"%s\"}", 
                        project.getId(), project.getName()))
                .build();
        
        notification = notificationRepository.save(notification);
        
        // Send realtime notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                notificationMapper.toResponse(notification)
        );
    }
    
    @Transactional
    public void sendWelcomeNotification(User user, Company company) {
        Notification notification = Notification.builder()
                .user(user)
                .company(company)
                .type("WELCOME")
                .title("Welcome to SprintLite!")
                .content("Your account has been created successfully. Get started by creating your first project!")
                .isRead(false)
                .isArchived(false)
                .metadata("{}")
                .build();
        
        notification = notificationRepository.save(notification);
        
        // Send realtime notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                notificationMapper.toResponse(notification)
        );
    }
    
    @Transactional
    public void sendTaskAssignedNotification(Task task, User assignee, User assignedBy) {
        Notification notification = Notification.builder()
                .user(assignee)
                .company(task.getCompany())
                .type("TASK_ASSIGNED")
                .title("New Task Assigned")
                .content(assignedBy.getFullName() + " assigned you to task: " + task.getTitle())
                .isRead(false)
                .isArchived(false)
                .metadata(String.format("{\"taskId\":\"%s\", \"taskKey\":\"%s\"}", task.getId(), task.getTaskKey()))
                .build();
        
        notification = notificationRepository.save(notification);
        
        // Send realtime notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                assignee.getId().toString(),
                "/queue/notifications",
                notificationMapper.toResponse(notification)
        );
    }
    
    @Transactional
    public void sendTaskStatusChangedNotification(Task task, String oldStatus, String newStatus, User changedBy) {
        if (task.getAssignee() != null) {
            Notification notification = Notification.builder()
                    .user(task.getAssignee())
                    .company(task.getCompany())
                    .type("TASK_STATUS_CHANGED")
                    .title("Task Status Updated")
                    .content(changedBy.getFullName() + " changed task \"" + task.getTitle() + "\" from " + oldStatus + " to " + newStatus)
                    .isRead(false)
                    .isArchived(false)
                    .metadata(String.format("{\"taskId\":\"%s\", \"taskKey\":\"%s\", \"oldStatus\":\"%s\", \"newStatus\":\"%s\"}", 
                            task.getId(), task.getTaskKey(), oldStatus, newStatus))
                    .build();
            
            notification = notificationRepository.save(notification);
            
            messagingTemplate.convertAndSendToUser(
                    task.getAssignee().getId().toString(),
                    "/queue/notifications",
                    notificationMapper.toResponse(notification)
            );
        }
    }
    
    @Transactional
    public void sendCommentAddedNotification(Task task, com.sprintlite.sprintlite_backend.domain.entity.Comment comment, User commentedBy) {
        if (task.getAssignee() != null && !task.getAssignee().getId().equals(commentedBy.getId())) {
            Notification notification = Notification.builder()
                    .user(task.getAssignee())
                    .company(task.getCompany())
                    .type("COMMENT_ADDED")
                    .title("New Comment on Your Task")
                    .content(commentedBy.getFullName() + " commented on task: " + task.getTitle())
                    .isRead(false)
                    .isArchived(false)
                    .metadata(String.format("{\"taskId\":\"%s\", \"taskKey\":\"%s\", \"commentId\":\"%s\"}", 
                            task.getId(), task.getTaskKey(), comment.getId()))
                    .build();
            
            notification = notificationRepository.save(notification);
            
            messagingTemplate.convertAndSendToUser(
                    task.getAssignee().getId().toString(),
                    "/queue/notifications",
                    notificationMapper.toResponse(notification)
            );
        }
    }
    
    public Page<NotificationResponse> getUserNotifications(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(), pageable);
        return notifications.map(notificationMapper::toResponse);
    }
    
    public long getUnreadCount() {
        User currentUser = securityUtils.getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
    }
    
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void markAllAsRead() {
        User currentUser = securityUtils.getCurrentUser();
        notificationRepository.markAllAsRead(currentUser.getId());
    }
}