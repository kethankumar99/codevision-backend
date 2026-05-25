package com.sprintlite.sprintlite_backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintlite.sprintlite_backend.domain.dto.request.ProjectRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.ProjectResponse;
import com.sprintlite.sprintlite_backend.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {
    
    private final ProjectService projectService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Create a new project")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all projects")
    public ResponseEntity<Page<ProjectResponse>> getProjects(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProjectResponse> projects = projectService.getProjects(pageable);
        return ResponseEntity.ok(projects);
    }
    
    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID projectId) {
        ProjectResponse project = projectService.getProject(projectId);
        return ResponseEntity.ok(project);
    }
    
    @PutMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Update project")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Delete project")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}