package com.sprintlite.sprintlite_backend.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String role;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}