package com.codevision.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codevision.model.User;
import com.codevision.service.LanguageDetectionService;
import com.codevision.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Language Detection", description = "Detect programming languages & frameworks")
@SecurityRequirement(name = "Bearer Authentication")
public class LanguageDetectionController {

    private static final Logger log = LoggerFactory.getLogger(LanguageDetectionController.class);
    private final ProjectService projectService;
    private final LanguageDetectionService languageDetectionService;

    public LanguageDetectionController(ProjectService projectService,
                                       LanguageDetectionService languageDetectionService) {
        this.projectService = projectService;
        this.languageDetectionService = languageDetectionService;
        log.info("✅ LanguageDetectionController initialized");
    }

    @PostMapping("/{projectId}/detect-language")
    @Operation(
        summary = "Detect project language",
        description = "Analyzes the project files to detect programming language, framework, and platform"
    )
    public ResponseEntity<?> detectLanguage(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User user) {
        
        String requestId = generateRequestId("LANG");
        
        log.info("🔍 [{}] Language detection requested for project: {}", requestId, projectId);
        
        try {
            var response = projectService.detectProjectLanguage(projectId, user);
            
            log.info("✅ [{}] Language detection completed: {}", requestId, response.getMessage());
            
            return buildSuccessResponse(response, response.getMessage(), HttpStatus.OK, requestId);

        } catch (RuntimeException e) {
            log.error("❌ [{}] Language detection failed: {}", requestId, e.getMessage());
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST, requestId);
        }
    }

    // Helper methods (same pattern as other controllers)
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