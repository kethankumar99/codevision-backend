package com.sprintlite.sprintlite_backend.controller;

import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sprintlite.sprintlite_backend.domain.dto.request.InviteMemberRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.InviteResponse;
import com.sprintlite.sprintlite_backend.domain.dto.response.UserResponse;
import com.sprintlite.sprintlite_backend.service.ProjectMemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project Members", description = "Project member management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectMemberController {
    
    private final ProjectMemberService projectMemberService;
    
    @PostMapping("/{projectId}/invite")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Invite members to project")
    public ResponseEntity<InviteResponse> inviteMembers(
            @PathVariable UUID projectId,
            @Valid @RequestBody InviteMemberRequest request) {
        InviteResponse response = projectMemberService.inviteMembers(projectId, request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{projectId}/members")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all project members")
    public ResponseEntity<List<UserResponse>> getProjectMembers(@PathVariable UUID projectId) {
        List<UserResponse> members = projectMemberService.getProjectMembers(projectId);
        return ResponseEntity.ok(members);
    }
    
    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Remove member from project")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        projectMemberService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{projectId}/members/{userId}/role")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Update member role in project")
    public ResponseEntity<UserResponse> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestParam String role) {
        UserResponse response = projectMemberService.updateMemberRole(projectId, userId, role);
        return ResponseEntity.ok(response);
    }
}