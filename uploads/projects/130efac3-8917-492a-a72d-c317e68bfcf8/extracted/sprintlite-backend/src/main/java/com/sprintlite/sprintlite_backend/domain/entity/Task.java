package com.sprintlite.sprintlite_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @Column(name = "task_key", nullable = false)
    private String taskKey;
    
    @Column(name = "task_type")
    private String taskType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;
    
    private String priority;
    
    private String status;
    
    @Column(name = "story_points")
    private Integer storyPoints;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "time_estimate")
    private Integer timeEstimate;
    
    @Column(name = "time_spent")
    private Integer timeSpent;
    
    @Column(columnDefinition = "text[]")
    private String[] labels;
    
    @Column(name = "attachments_count")
    private Integer attachmentsCount;
    
    @Column(name = "comments_count")
    private Integer commentsCount;
    
    @Column(name = "watchers_count")
    private Integer watchersCount;
    
    @Column(columnDefinition = "jsonb")
    private String customFields;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}