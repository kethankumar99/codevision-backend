package com.codevision.controller;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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

import com.codevision.ai.GeminiService;
import com.codevision.model.Project;
import com.codevision.model.User;
import com.codevision.parser.SpringBootApiExtractor;
import com.codevision.repository.ProjectRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Analysis", description = "Gemini AI-powered code analysis")
@SecurityRequirement(name = "Bearer Authentication")
public class AiAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisController.class);
    private final ProjectRepository projectRepository;
    private final SpringBootApiExtractor apiExtractor;
    private final GeminiService geminiService;

    public AiAnalysisController(ProjectRepository projectRepository, GeminiService geminiService) {
        this.projectRepository = projectRepository;
        this.apiExtractor = new SpringBootApiExtractor();
        this.geminiService = geminiService;
        log.info("✅ AiAnalysisController ready");
    }

    @PostMapping("/analyze/{projectId}")
@Operation(summary = "Full AI analysis")
public ResponseEntity<?> analyzeProject(@PathVariable UUID projectId,
                                         @AuthenticationPrincipal User user) {
    try {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getUser().getId().equals(user.getId())) {
            return buildErrorResponse("Access denied", HttpStatus.FORBIDDEN);
        }

        Path projectPath = Path.of(project.getExtractedPath());
        java.io.File[] files = projectPath.toFile().listFiles();
        if (files != null && files.length == 1 && files[0].isDirectory()) {
            projectPath = files[0].toPath();
        }

        List<Map<String, Object>> apis = apiExtractor.extractApis(projectPath);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId.toString());
        result.put("apiCount", apis.size());
        result.put("apis", apis);

        // Try AI - but don't fail if it doesn't work
        String archSummary = null;
        try {
            archSummary = geminiService.generateArchitectureSummary(
                    apis, project.getDetectedLanguage(), project.getDetectedFramework());
        } catch (Exception aiEx) {
            log.warn("⚠️ AI analysis skipped (rate limited or unavailable)");
            archSummary = "AI analysis temporarily unavailable. API extraction completed successfully.";
        }
        
        result.put("architecture", archSummary);

        return buildSuccessResponse(result, "Analysis complete", HttpStatus.OK);

    } catch (Exception e) {
        log.error("Analysis failed: {}", e.getMessage());
        return buildErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Object data, String msg, HttpStatus status) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        r.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(r);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String err, HttpStatus status) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("error", err);
        r.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(r);
    }
}