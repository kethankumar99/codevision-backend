package com.sprintlite.sprintlite_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sprintlite.sprintlite_backend.domain.dto.request.ChangePasswordRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.LoginRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.RegisterRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.AuthResponse;
import com.sprintlite.sprintlite_backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user and company")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("📝 Register request received for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("✅ User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Login request received for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("✅ User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "Change user password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("🔑 Password change request received");
        authService.changePassword(request);
        log.info("✅ Password changed successfully");
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }
    
    @PostMapping("/accept-invitation")
    @Operation(summary = "Accept invitation and setup account")
    public ResponseEntity<AuthResponse> acceptInvitation(
            @RequestParam String token,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String password) {
        log.info("📧 Accepting invitation with token: {}", token);
        AuthResponse response = authService.acceptInvitation(token, firstName, lastName, password);
        log.info("✅ Invitation accepted successfully");
        return ResponseEntity.ok(response);
    }
}