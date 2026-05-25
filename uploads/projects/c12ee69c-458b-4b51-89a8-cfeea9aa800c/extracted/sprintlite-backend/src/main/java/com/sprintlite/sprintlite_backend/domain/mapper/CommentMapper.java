package com.sprintlite.sprintlite_backend.domain.mapper;

import org.springframework.stereotype.Component;

import com.sprintlite.sprintlite_backend.domain.dto.request.CommentRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.CommentResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Comment;
import com.sprintlite.sprintlite_backend.domain.entity.Task;
import com.sprintlite.sprintlite_backend.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    
    private final UserMapper userMapper;
    
    public Comment toEntity(CommentRequest request, Task task, User user) {
        return Comment.builder()
                .content(request.getContent())
                .task(task)
                .user(user)
                .isEdited(false)
                .attachments("[]")
                .build();
    }
    
    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(userMapper.toResponse(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .isEdited(comment.getIsEdited())
                .editedAt(comment.getEditedAt())
                .build();
    }
}