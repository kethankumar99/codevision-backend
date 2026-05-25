package com.codevision.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.codevision.dto.ProjectUploadResponse;
import com.codevision.model.LanguageDetectionResult;
import com.codevision.model.Project;
import com.codevision.model.User;
import com.codevision.repository.ProjectRepository;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final LanguageDetectionService languageDetectionService;

    public ProjectService(ProjectRepository projectRepository,
                          FileStorageService fileStorageService,
                          LanguageDetectionService languageDetectionService) {
        this.projectRepository = projectRepository;
        this.fileStorageService = fileStorageService;
        this.languageDetectionService = languageDetectionService;
        log.info("✅ ProjectService initialized");
    }

    /**
     * Upload and process a ZIP project
     */
    @Transactional
    public ProjectUploadResponse uploadProject(MultipartFile file, User user) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        log.info("📤 [{}] Project upload started - User: {}, File: {}, Size: {} bytes",
                requestId, user.getEmail(), file.getOriginalFilename(), file.getSize());

        try {
            // Create project entity
            Project project = Project.builder()
                    .user(user)
                    .projectName(extractProjectName(file.getOriginalFilename()))
                    .uploadType("ZIP")
                    .originalFilename(file.getOriginalFilename())
                    .status("UPLOADED")
                    .build();
            
            project = projectRepository.save(project);
            log.debug("[{}] Project record created: {}", requestId, project.getId());

            // Update status to EXTRACTING
            project.setStatus("EXTRACTING");
            project = projectRepository.save(project);
            
            // Save ZIP file
            log.debug("[{}] Saving ZIP file...", requestId);
            Path zipPath = fileStorageService.saveZipFile(file, project.getId());
            
            // Extract ZIP
            log.debug("[{}] Extracting ZIP file...", requestId);
            Path extractedPath = fileStorageService.extractZip(zipPath, project.getId());
            
            // Update project
            project.setExtractedPath(extractedPath.toString());
            project.setStatus("EXTRACTED");
            
            // Count files
            long fileCount = countFiles(extractedPath);
            project.setTotalFiles((int) fileCount);
            
            project = projectRepository.save(project);
            
            log.info("✅ [{}] Project uploaded successfully - ProjectId: {}, Files: {}",
                    requestId, project.getId(), fileCount);

            return ProjectUploadResponse.builder()
                    .projectId(project.getId().toString())
                    .projectName(project.getProjectName())
                    .status(project.getStatus())
                    .totalFiles(project.getTotalFiles())
                    .message("Project uploaded and extracted successfully")
                    .requestId(requestId)
                    .build();

        } catch (Exception e) {
            log.error("❌ [{}] Project upload failed: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Failed to upload project: " + e.getMessage());
        }
    }

    /**
     * Get all projects for a user
     */
    public List<ProjectUploadResponse> getUserProjects(User user) {
        log.debug("Fetching projects for user: {}", user.getEmail());
        
        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        return projects.stream()
                .map(this::toProjectResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get single project details
     */
    public ProjectUploadResponse getProject(UUID projectId, User user) {
        log.debug("Fetching project: {} for user: {}", projectId, user.getEmail());
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // Check ownership
        if (!project.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized access attempt to project: {} by user: {}", projectId, user.getId());
            throw new RuntimeException("Access denied");
        }
        
        return toProjectResponse(project);
    }

    /**
     * Delete a project
     */
    @Transactional
    public void deleteProject(UUID projectId, User user) {
        log.info("🗑️ Deleting project: {} for user: {}", projectId, user.getEmail());
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        // Delete files
        fileStorageService.deleteProjectFiles(projectId);
        
        // Delete from database (cascade will delete related records)
        projectRepository.delete(project);
        
        log.info("✅ Project deleted: {}", projectId);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String extractProjectName(String filename) {
        if (filename == null) return "Unnamed Project";
        // Remove extension
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    private long countFiles(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .count();
        } catch (Exception e) {
            log.warn("Failed to count files: {}", e.getMessage());
            return 0;
        }
    }

    private ProjectUploadResponse toProjectResponse(Project project) {
        return ProjectUploadResponse.builder()
                .projectId(project.getId().toString())
                .projectName(project.getProjectName())
                .originalFilename(project.getOriginalFilename())
                .status(project.getStatus())
                .totalFiles(project.getTotalFiles() != null ? project.getTotalFiles() : 0)
                .createdAt(project.getCreatedAt() != null ? project.getCreatedAt().toString() : null)
                .analyzedAt(project.getAnalyzedAt() != null ? project.getAnalyzedAt().toString() : null)
                .build();
    }
    // Update constructor


// Add this method
/**
 * Detect project language and framework
 */
public ProjectUploadResponse detectProjectLanguage(UUID projectId, User user) {
    String requestId = UUID.randomUUID().toString().substring(0, 8);
    
    log.info("🔍 [{}] Detecting language for project: {}", requestId, projectId);
    
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
    
    if (!project.getUser().getId().equals(user.getId())) {
        throw new RuntimeException("Access denied");
    }
    
    if (project.getExtractedPath() == null) {
        throw new RuntimeException("Project not extracted yet");
    }
    
    Path projectPath = Path.of(project.getExtractedPath());
    LanguageDetectionResult result = languageDetectionService.detectProjectLanguage(projectPath);
    
    // Update project
    project.setDetectedLanguage(result.getPrimaryLanguage());
    project.setDetectedFramework(result.getDetectedFramework());
    project.setStatus("LANGUAGE_DETECTED");
    projectRepository.save(project);
    
    log.info("✅ [{}] Language detected: {} {} ({}% confidence)", 
            requestId, 
            result.getLanguageEmoji(),
            result.getPrimaryLanguage(),
            result.getConfidencePercent());
    
    return ProjectUploadResponse.builder()
            .projectId(project.getId().toString())
            .projectName(project.getProjectName())
            .status(project.getStatus())
            .message(String.format("Detected: %s %s", result.getLanguageEmoji(), result.getPrimaryLanguage()))
            .build();
}
}