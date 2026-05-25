package com.sprintlite.sprintlite_backend.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private UUID id;
    private String content;
    private UserResponse user;
    private LocalDateTime createdAt;
    private Boolean isEdited;
    private LocalDateTime editedAt;
}