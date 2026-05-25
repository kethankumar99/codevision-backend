package com.sprintlite.sprintlite_backend.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private String taskKey;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String taskType;
    private UserResponse assignee;
    private UserResponse reporter;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Integer storyPoints;
    private Integer timeEstimate;
    private Integer timeSpent;
    private List<String> labels;
    private List<CommentResponse> comments;
    private Integer commentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}