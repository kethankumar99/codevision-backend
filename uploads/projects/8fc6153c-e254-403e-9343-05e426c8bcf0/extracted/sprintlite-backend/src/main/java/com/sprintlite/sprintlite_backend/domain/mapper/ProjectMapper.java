package com.sprintlite.sprintlite_backend.domain.mapper;

import org.springframework.stereotype.Component;

import com.sprintlite.sprintlite_backend.domain.dto.request.ProjectRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.ProjectResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Company;
import com.sprintlite.sprintlite_backend.domain.entity.Project;
import com.sprintlite.sprintlite_backend.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMapper {
    
    private final UserMapper userMapper;
    
    public Project toEntity(ProjectRequest request, Company company, User createdBy) {
        if (request == null) return null;
        
        return Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .keyCode(request.getKeyCode().toUpperCase())
                .iconEmoji(request.getIconEmoji())
                .color(request.getColor())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .deadline(request.getDeadline())
                .priority(request.getPriority())
                .company(company)
                .createdBy(createdBy)
                .status("active")
                .isPublic(false)
                .build();
    }
    
    public ProjectResponse toResponse(Project project, Integer totalTasks, 
                                       Integer completedTasks, Integer inProgressTasks, 
                                       Integer todoTasks) {
        if (project == null) return null;
        
        int progress = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
        
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .keyCode(project.getKeyCode())
                .iconEmoji(project.getIconEmoji())
                .color(project.getColor())
                .status(project.getStatus())
                .priority(project.getPriority())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .deadline(project.getDeadline())
                .createdBy(userMapper.toResponse(project.getCreatedBy()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .progressPercentage(progress)
                .build();
    }
}