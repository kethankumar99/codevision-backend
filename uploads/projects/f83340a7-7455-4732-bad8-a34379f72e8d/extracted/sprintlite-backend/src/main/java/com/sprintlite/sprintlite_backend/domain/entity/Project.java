package com.sprintlite.sprintlite_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.JdbcTypeCode; // కొత్తగా యాడ్ చేసాం
import org.hibernate.type.SqlTypes;          // కొత్తగా యాడ్ చేసాం

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(name = "key_code", nullable = false)
    private String keyCode;
    
    @Column(name = "icon_emoji")
    private String iconEmoji;
    
    private String color;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    private LocalDate deadline;
    
    @Builder.Default
    private String status = "active"; // డిఫాల్ట్ వాల్యూ
    
    private String priority;
    
    // --- ఇక్కడ మార్పు చేసాం (JSONB Error Fix) ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    @Builder.Default
    private String settings = "{}"; 
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false; // డిఫాల్ట్ వాల్యూ
    
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