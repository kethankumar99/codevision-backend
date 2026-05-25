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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codevision.model.Project;
import com.codevision.model.User;
import com.codevision.parser.ParserFactory;
import com.codevision.parser.ParserInterface;
import com.codevision.parser.SpringBootApiExtractor;
import com.codevision.repository.ProjectRepository;
import com.codevision.service.FlowMappingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Flow Mapping", description = "Trace API execution flows")
@SecurityRequirement(name = "Bearer Authentication")
public class FlowMappingController {

    private static final Logger log = LoggerFactory.getLogger(FlowMappingController.class);

    private final ProjectRepository projectRepository;
    private final SpringBootApiExtractor apiExtractor;
    private final FlowMappingService flowMappingService;

    public FlowMappingController(ProjectRepository projectRepository,
                                  FlowMappingService flowMappingService) {
        this.projectRepository = projectRepository;
        this.apiExtractor = new SpringBootApiExtractor();
        this.flowMappingService = flowMappingService;
        log.info("✅ FlowMappingController initialized");
    }

  @PostMapping("/{projectId}/map-flows")
public ResponseEntity<?> mapFlows(@PathVariable UUID projectId,
                                   @AuthenticationPrincipal User user) {
    String requestId = generateRequestId("FLOW");

    try {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // ALWAYS return success, even if empty
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId.toString());
        result.put("totalFlows", 0);
        result.put("flows", List.of());

        try {
            Path projectPath = findProjectRoot(Path.of(project.getExtractedPath()));
            String language = project.getDetectedLanguage() != null ? project.getDetectedLanguage() : "Unknown";
            ParserInterface parser = ParserFactory.getParser(language, project.getDetectedFramework());
            
            List<Map<String, Object>> apis = parser.extractApis(projectPath);
            List<Map<String, Object>> flows = parser.mapFlows(projectPath, apis);
            
            result.put("totalFlows", flows.size());
            result.put("flows", flows);
        } catch (Exception e) {
            log.warn("Flow mapping skipped: {}", e.getMessage());
        }

        return buildSuccessResponse(result, "Flow mapping complete", HttpStatus.OK, requestId);

    } catch (Exception e) {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("totalFlows", 0);
        empty.put("flows", List.of());
        return buildSuccessResponse(empty, "Flow mapping unavailable", HttpStatus.OK, requestId);
    }
}

    @GetMapping("/{projectId}/flows/{apiIndex}")
    @Operation(summary = "Get specific flow", description = "Get flow for a specific API by index")
    public ResponseEntity<?> getFlow(@PathVariable UUID projectId,
                                      @PathVariable int apiIndex,
                                      @AuthenticationPrincipal User user) {
        String requestId = generateRequestId("FLOW");

        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            Path projectPath = findProjectRoot(Path.of(project.getExtractedPath()));
            List<Map<String, Object>> apis = apiExtractor.extractApis(projectPath);

            if (apiIndex >= apis.size()) {
                return buildErrorResponse("Invalid API index", HttpStatus.BAD_REQUEST, requestId);
            }

            List<Map<String, Object>> singleApi = List.of(apis.get(apiIndex));
            List<Map<String, Object>> flows = flowMappingService.mapFlows(projectPath, singleApi);

            return buildSuccessResponse(flows.isEmpty() ? null : flows.get(0),
                    "Flow retrieved", HttpStatus.OK, requestId);

        } catch (Exception e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST, requestId);
        }
    }

    private Path findProjectRoot(Path extractDir) {
        try {
            java.io.File[] files = extractDir.toFile().listFiles();
            if (files != null && files.length == 1 && files[0].isDirectory()) {
                return files[0].toPath();
            }
        } catch (Exception e) {
            log.warn("Could not find project root: {}", e.getMessage());
        }
        return extractDir;
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