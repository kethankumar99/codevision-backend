package com.codevision.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "User login request payload")
public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Registered email", example = "john@example.com")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "Account password", example = "Test@1234")
    private String password;
}