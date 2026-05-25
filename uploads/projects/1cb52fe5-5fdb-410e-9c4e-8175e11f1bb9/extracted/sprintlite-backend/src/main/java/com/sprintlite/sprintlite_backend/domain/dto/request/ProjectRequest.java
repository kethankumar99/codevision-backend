package com.sprintlite.sprintlite_backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Project key is required")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "Project key must be 2-10 uppercase letters")
    private String keyCode;
    
    private String iconEmoji;
    private String color;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate deadline;
    private String priority;
}