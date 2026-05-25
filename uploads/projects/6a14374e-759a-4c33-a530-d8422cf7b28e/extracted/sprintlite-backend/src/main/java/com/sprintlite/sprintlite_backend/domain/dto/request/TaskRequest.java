package com.sprintlite.sprintlite_backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TaskRequest {
    @NotBlank(message = "Task title is required")
    private String title;
    
    private String description;
    private UUID assigneeId;
    private UUID sprintId;
    private String priority;
    private String status;
    private Integer storyPoints;
    private LocalDate dueDate;
    private Integer timeEstimate;
    private String taskType;
    private String[] labels;
}