package com.sprintlite.sprintlite_backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlite.sprintlite_backend.domain.dto.request.TaskRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.UpdateTaskStatusRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.TaskResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Project;
import com.sprintlite.sprintlite_backend.domain.entity.Sprint;
import com.sprintlite.sprintlite_backend.domain.entity.Task;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.mapper.TaskMapper;
import com.sprintlite.sprintlite_backend.domain.repository.ProjectRepository;
import com.sprintlite.sprintlite_backend.domain.repository.SprintRepository;
import com.sprintlite.sprintlite_backend.domain.repository.TaskRepository;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.exception.ResourceNotFoundException;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final TaskMapper taskMapper;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    
    @Transactional
    public TaskResponse createTask(UUID projectId, TaskRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        // Verify company access
        if (!project.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new ResourceNotFoundException("Project not found");
        }
        
        // Get assignee if provided
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }
        
        // Get sprint if provided
        Sprint sprint = null;
        if (request.getSprintId() != null) {
            sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
        }
        
        // Create task
        Task task = taskMapper.toEntity(request, currentUser.getCompany(), project, 
                sprint, assignee, currentUser, currentUser);
        
        // Generate task key
        long taskCount = taskRepository.countByProjectId(projectId);
        String taskKey = taskMapper.generateTaskKey(project.getKeyCode(), taskCount);
        task.setTaskKey(taskKey);
        
        task = taskRepository.save(task);
        
        // Send notification to assignee
        if (assignee != null && !assignee.getId().equals(currentUser.getId())) {
            notificationService.sendTaskAssignedNotification(task, assignee, currentUser);
        }
        
        return taskMapper.toResponse(task);
    }
    
    public Page<TaskResponse> getTasksByProject(UUID projectId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        
        if (!project.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new ResourceNotFoundException("Project not found");
        }
        
        Page<Task> tasks = taskRepository.findByProjectId(projectId, pageable);
        return tasks.map(taskMapper::toResponse);
    }
    
    public TaskResponse getTask(UUID taskId) {
        Task task = getTaskById(taskId);
        return taskMapper.toResponse(task);
    }
    
    @Transactional
    public TaskResponse updateTask(UUID taskId, TaskRequest request) {
        Task task = getTaskById(taskId);
        
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStoryPoints(request.getStoryPoints());
        task.setDueDate(request.getDueDate());
        task.setTimeEstimate(request.getTimeEstimate());
        task.setLabels(request.getLabels());
        
        // Update assignee if changed
        if (request.getAssigneeId() != null && 
            (task.getAssignee() == null || !task.getAssignee().getId().equals(request.getAssigneeId()))) {
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssignee(newAssignee);
            
            // Send notification
            User currentUser = securityUtils.getCurrentUser();
            notificationService.sendTaskAssignedNotification(task, newAssignee, currentUser);
        }
        
        // Update sprint if changed
        if (request.getSprintId() != null && 
            (task.getSprint() == null || !task.getSprint().getId().equals(request.getSprintId()))) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found"));
            task.setSprint(sprint);
        }
        
        task = taskRepository.save(task);
        return taskMapper.toResponse(task);
    }
    
    @Transactional
    public TaskResponse updateTaskStatus(UUID taskId, UpdateTaskStatusRequest request) {
        Task task = getTaskById(taskId);
        String oldStatus = task.getStatus();
        String newStatus = request.getStatus();
        
        task.setStatus(newStatus);
        
        if ("completed".equals(newStatus) && oldStatus != null && !"completed".equals(oldStatus)) {
            task.setCompletedAt(LocalDateTime.now());
        }
        
        task = taskRepository.save(task);
        
        // Create status history
        createStatusHistory(task, oldStatus, newStatus);
        
        // Send notification
        if (!oldStatus.equals(newStatus)) {
            User currentUser = securityUtils.getCurrentUser();
            notificationService.sendTaskStatusChangedNotification(task, oldStatus, newStatus, currentUser);
        }
        
        return taskMapper.toResponse(task);
    }
    
    @Transactional
    public void deleteTask(UUID taskId) {
        Task task = getTaskById(taskId);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
    
    private Task getTaskById(UUID taskId) {
        User currentUser = securityUtils.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        if (!task.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new ResourceNotFoundException("Task not found");
        }
        return task;
    }
    
    private void createStatusHistory(Task task, String fromStatus, String toStatus) {
        // This would save to task_status_history table
        // Implementation depends on your TaskStatusHistory entity
    }
}