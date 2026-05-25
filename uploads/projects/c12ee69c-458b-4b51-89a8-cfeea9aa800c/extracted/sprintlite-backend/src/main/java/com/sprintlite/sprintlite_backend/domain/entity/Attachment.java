package com.sprintlite.sprintlite_backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "original_name", nullable = false)
    private String originalName;
    
    @Column(name = "mime_type", nullable = false)
    private String mimeType;
    
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    
    @Column(name = "storage_path", nullable = false)
    private String storagePath;
    
    @Builder.Default
    private String storageProvider = "local";
    
    private String fileHash;
    
    private String thumbnailPath;
    
    @Builder.Default
    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime deletedAt;
}