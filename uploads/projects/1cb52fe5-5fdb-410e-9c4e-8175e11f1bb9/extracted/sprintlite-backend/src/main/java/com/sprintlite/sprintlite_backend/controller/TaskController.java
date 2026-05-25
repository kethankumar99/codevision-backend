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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintlite.sprintlite_backend.domain.dto.request.CommentRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.TaskRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.UpdateTaskStatusRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.CommentResponse;
import com.sprintlite.sprintlite_backend.domain.dto.response.TaskResponse;
import com.sprintlite.sprintlite_backend.service.CommentService;
import com.sprintlite.sprintlite_backend.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskController {
    
    private final TaskService taskService;
    private final CommentService commentService;
    
    // Task CRUD Operations
    
    @PostMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new task in project")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID projectId,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all tasks in project")
    public ResponseEntity<Page<TaskResponse>> getTasksByProject(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TaskResponse> tasks = taskService.getTasksByProject(projectId, pageable);
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
        TaskResponse task = taskService.getTask(taskId);
        return ResponseEntity.ok(task);
    }
    
    @PutMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update task")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{taskId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update task status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        TaskResponse response = taskService.updateTaskStatus(taskId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{taskId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete task")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
    
    // Comment Operations
    
    @PostMapping("/{taskId}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add comment to task")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody CommentRequest request) {
        CommentResponse response = commentService.addComment(taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete comment")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}