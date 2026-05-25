package com.sprintlite.sprintlite_backend.domain.mapper;

import org.springframework.stereotype.Component;

import com.sprintlite.sprintlite_backend.domain.dto.request.TaskRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.TaskResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Company;
import com.sprintlite.sprintlite_backend.domain.entity.Project;
import com.sprintlite.sprintlite_backend.domain.entity.Sprint;
import com.sprintlite.sprintlite_backend.domain.entity.Task;
import com.sprintlite.sprintlite_backend.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TaskMapper {
    
    private final UserMapper userMapper;
    
    public Task toEntity(TaskRequest request, Company company, Project project, 
                         Sprint sprint, User assignee, User reporter, User createdBy) {
        if (request == null) return null;
        
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .taskType(request.getTaskType() != null ? request.getTaskType() : "task")
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .status(request.getStatus() != null ? request.getStatus() : "todo")
                .storyPoints(request.getStoryPoints())
                .dueDate(request.getDueDate())
                .timeEstimate(request.getTimeEstimate())
                .labels(request.getLabels())
                .company(company)
                .project(project)
                .sprint(sprint)
                .assignee(assignee)
                .reporter(reporter)
                .createdBy(createdBy)
                .attachmentsCount(0)
                .commentsCount(0)
                .watchersCount(0)
                .timeSpent(0)
                .build();
    }
    
    public TaskResponse toResponse(Task task) {
        if (task == null) return null;
        
        return TaskResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .taskType(task.getTaskType())
                .assignee(userMapper.toResponse(task.getAssignee()))
                .reporter(userMapper.toResponse(task.getReporter()))
                .dueDate(task.getDueDate())
                .startDate(task.getStartDate())
                .storyPoints(task.getStoryPoints())
                .timeEstimate(task.getTimeEstimate())
                .timeSpent(task.getTimeSpent())
                .labels(task.getLabels() != null ? java.util.Arrays.asList(task.getLabels()) : null)
                .commentsCount(task.getCommentsCount())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
    
    public String generateTaskKey(String projectKey, Long taskCount) {
        return projectKey + "-" + (taskCount + 1);
    }
}