package com.sprintlite.sprintlite_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlite.sprintlite_backend.domain.dto.request.ProjectRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.ProjectResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Project;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.mapper.ProjectMapper;
import com.sprintlite.sprintlite_backend.domain.repository.ProjectRepository;
import com.sprintlite.sprintlite_backend.domain.repository.TaskRepository;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.exception.BadRequestException;
import com.sprintlite.sprintlite_backend.exception.ResourceNotFoundException;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final SecurityUtils securityUtils;
    
    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        
        // Check if key code exists
        if (projectRepository.existsByKeyCodeAndCompanyId(request.getKeyCode(), currentUser.getCompany().getId())) {
            throw new BadRequestException("Project key code already exists");
        }
        
        Project project = projectMapper.toEntity(request, currentUser.getCompany(), currentUser);
        project = projectRepository.save(project);
        
        return getProjectResponse(project);
    }
    
    public Page<ProjectResponse> getProjects(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<Project> projects = projectRepository.findByCompanyId(currentUser.getCompany().getId(), pageable);
        return projects.map(this::getProjectResponse);
    }
    
    public ProjectResponse getProject(UUID projectId) {
        Project project = getProjectById(projectId);
        return getProjectResponse(project);
    }
    
    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest request) {
        Project project = getProjectById(projectId);
        
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setDeadline(request.getDeadline());
        project.setPriority(request.getPriority());
        
        project = projectRepository.save(project);
        return getProjectResponse(project);
    }
    
    @Transactional
    public void deleteProject(UUID projectId) {
        Project project = getProjectById(projectId);
        project.setDeletedAt(java.time.LocalDateTime.now());
        projectRepository.save(project);
    }
    
    private Project getProjectById(UUID projectId) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (!project.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new ResourceNotFoundException("Project not found");
        }
        return project;
    }
    
    private ProjectResponse getProjectResponse(Project project) {
        long totalTasks = taskRepository.countByProjectId(project.getId());
        long completedTasks = taskRepository.countByProjectIdAndStatus(project.getId(), "completed");
        long inProgressTasks = taskRepository.countByProjectIdAndStatus(project.getId(), "in_progress");
        long todoTasks = taskRepository.countByProjectIdAndStatus(project.getId(), "todo");
        
        return projectMapper.toResponse(project, 
                (int) totalTasks, 
                (int) completedTasks, 
                (int) inProgressTasks, 
                (int) todoTasks);
    }
}