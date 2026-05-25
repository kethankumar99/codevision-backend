package com.sprintlite.sprintlite_backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlite.sprintlite_backend.domain.dto.request.CommentRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.CommentResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Comment;
import com.sprintlite.sprintlite_backend.domain.entity.Task;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.mapper.CommentMapper;
import com.sprintlite.sprintlite_backend.domain.repository.CommentRepository;
import com.sprintlite.sprintlite_backend.domain.repository.TaskRepository;
import com.sprintlite.sprintlite_backend.exception.ResourceNotFoundException;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CommentMapper commentMapper;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    
    @Transactional
    public CommentResponse addComment(UUID taskId, CommentRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        Comment comment = commentMapper.toEntity(request, task, currentUser);
        comment = commentRepository.save(comment);
        
        // Increment comment count on task
        task.setCommentsCount(task.getCommentsCount() + 1);
        taskRepository.save(task);
        
        // Send notification to assignee
        if (task.getAssignee() != null && !task.getAssignee().getId().equals(currentUser.getId())) {
            notificationService.sendCommentAddedNotification(task, comment, currentUser);
        }
        
        return commentMapper.toResponse(comment);
    }
    
    @Transactional
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
        
        // Decrement comment count on task
        Task task = comment.getTask();
        task.setCommentsCount(Math.max(0, task.getCommentsCount() - 1));
        taskRepository.save(task);
    }
}