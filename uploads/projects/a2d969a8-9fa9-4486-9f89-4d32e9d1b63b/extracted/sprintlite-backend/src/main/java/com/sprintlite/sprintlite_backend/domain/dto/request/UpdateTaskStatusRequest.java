package com.sprintlite.sprintlite_backend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
}