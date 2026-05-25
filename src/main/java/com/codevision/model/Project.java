package com.codevision.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "project_name", nullable = false)
    private String projectName;
    
    private String description;
    
    @Column(name = "upload_type", nullable = false)
    private String uploadType; // ZIP, GIT_URL
    
    @Column(name = "git_url")
    private String gitUrl;
    
    @Column(name = "extracted_path")
    private String extractedPath;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "detected_language")
    private String detectedLanguage;
    
    @Column(name = "detected_framework")
    private String detectedFramework;
    
    @Column(name = "total_files")
    private Integer totalFiles = 0;
    
    @Column(name = "total_lines")
    private Integer totalLines = 0;
    
    @Column(columnDefinition = "TEXT")
    private String status = "UPLOADED";
    
    @Column(name = "total_apis")
    private Integer totalApis = 0;
    
    @Column(name = "total_issues")
    private Integer totalIssues = 0;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}