package com.codevision.controller;

import com.codevision.model.Project;
import com.codevision.model.User;
import com.codevision.parser.ParserFactory;
import com.codevision.parser.ParserInterface;
import com.codevision.repository.ProjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/analyze")
@Tag(name = "Universal Analysis", description = "Works for ALL languages")
@SecurityRequirement(name = "Bearer Authentication")
public class UnifiedApiController {

    private static final Logger log = LoggerFactory.getLogger(UnifiedApiController.class);
    private final ProjectRepository projectRepository;

    public UnifiedApiController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
        log.info("✅ Universal Analysis Controller ready");
    }

    @PostMapping("/{projectId}")
    @Operation(summary = "Analyze project (Auto-detect language & parser)")
    public ResponseEntity<?> analyzeProject(@PathVariable UUID projectId,
                                             @AuthenticationPrincipal User user) {
        String requestId = generateRequestId("ANALYZE");

        log.info("🔍 [{}] Universal analysis for project: {}", requestId, projectId);

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            if (!project.getUser().getId().equals(user.getId())) {
                return buildErrorResponse("Access denied", HttpStatus.FORBIDDEN, requestId);
            }

            String language = project.getDetectedLanguage();
            String framework = project.getDetectedFramework();

            if (language == null) {
                return buildErrorResponse("Language not detected. Run language detection first.",
                        HttpStatus.BAD_REQUEST, requestId);
            }

            // Get parser for this language
            ParserInterface parser = ParserFactory.getParser(language, framework);
            log.info("📦 Using parser: {} for {}", parser.getClass().getSimpleName(), language);

            Path projectPath = Path.of(project.getExtractedPath());

            // Extract APIs
            List<Map<String, Object>> apis = parser.extractApis(projectPath);

            // Map flows
            List<Map<String, Object>> flows = parser.mapFlows(projectPath, apis);

            // Update project
            project.setTotalApis(apis.size());
            project.setStatus("ANALYZED");
            projectRepository.save(project);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("projectId", projectId.toString());
            response.put("language", language);
            response.put("framework", framework != null ? framework : "None");
            response.put("parser", parser.getClass().getSimpleName());
            response.put("totalApis", apis.size());
            response.put("totalFlows", flows.size());
            response.put("apis", apis);
            response.put("flows", flows);

            log.info("✅ [{}] Analysis complete - {} APIs, {} flows", requestId, apis.size(), flows.size());

            return buildSuccessResponse(response,
                    String.format("Analyzed %s project: %d APIs, %d flows",
                            language, apis.size(), flows.size()),
                    HttpStatus.OK, requestId);

        } catch (Exception e) {
            log.error("❌ [{}] Analysis failed: {}", requestId, e.getMessage(), e);
            return buildErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, requestId);
        }
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(
            Object data, String message, HttpStatus status, String requestId) {
        Map<String, Object> response = new LinkedHashMap<>();
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
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("requestId", requestId);
        return ResponseEntity.status(status).body(response);
    }

    private String generateRequestId(String prefix) {
        return String.format("%s-%d-%04d",
                prefix, System.currentTimeMillis() % 100000, (int) (Math.random() * 10000));
    }
}