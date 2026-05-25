package com.codevision.controller;

import com.codevision.dto.AuthResponse;
import com.codevision.dto.LoginRequest;
import com.codevision.dto.RegisterRequest;
import com.codevision.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, Login & Profile APIs")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
        log.info("✅ AuthController initialized successfully");
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account with FREE plan. Password must contain uppercase, lowercase, number, and special character."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Email already exists or validation failed"
        )
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String requestId = generateRequestId("REG");
        
        log.info("📝 [{}] Registration request received for email: {}", requestId, request.getEmail());
        
        try {
            AuthResponse response = authService.register(request);
            log.info("✅ [{}] User registered successfully - userId: {}", requestId, response.getUser().getId());
            
            return buildSuccessResponse(response, "User registered successfully", HttpStatus.CREATED, requestId);
            
        } catch (RuntimeException e) {
            log.error("❌ [{}] Registration failed: {}", requestId, e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST, requestId);
        }
    }

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates user with email and password. Returns JWT access and refresh tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String requestId = generateRequestId("LOG");
        
        log.info("🔑 [{}] Login attempt for email: {}", requestId, request.getEmail());
        
        try {
            AuthResponse response = authService.login(request);
            log.info("✅ [{}] Login successful - userId: {}", requestId, response.getUser().getId());
            
            return buildSuccessResponse(response, "Login successful", HttpStatus.OK, requestId);
            
        } catch (RuntimeException e) {
            log.warn("❌ [{}] Login failed: {}", requestId, e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED, requestId);
        }
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile of currently authenticated user. Requires JWT token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile fetched"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> getCurrentUser() {
        String requestId = generateRequestId("PRO");
        log.info("👤 [{}] Profile request received", requestId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Profile endpoint working",
            "requestId", requestId,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // Helper methods (same as before)
    private ResponseEntity<Map<String, Object>> buildSuccessResponse(
            Object data, String message, HttpStatus status, String requestId) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("requestId", requestId);
        
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            String errorMessage, HttpStatus status, String requestId) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("requestId", requestId);
        
        return ResponseEntity.status(status).body(response);
    }

    private String generateRequestId(String prefix) {
        return String.format("%s-%d-%04d", 
            prefix, 
            System.currentTimeMillis() % 100000, 
            (int)(Math.random() * 10000));
    }
}