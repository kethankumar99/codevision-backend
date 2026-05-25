package com.sprintlite.sprintlite_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String content;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "is_archived")
    private Boolean isArchived;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}