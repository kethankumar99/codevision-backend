package com.sprintlite.sprintlite_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String subdomain;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    private String timezone;
    
    @Column(name = "date_format")
    private String dateFormat;
    
    @Column(name = "week_start")
    private String weekStart;
    
    @Column(name = "plan_type")
    private String planType;
    
    // @Column(name = "subscription_status")
    // private String subscriptionStatus;
    
    // @Column(name = "subscription_end_date")
    // private LocalDateTime subscriptionEndDate;
    
    @Column(name = "max_users")
    private Integer maxUsers;
    
    @Column(name = "max_projects")
    private Integer maxProjects;
    
    @Column(name = "max_storage_gb")
    private Integer maxStorageGb;
    
    @Column(columnDefinition = "jsonb")
    private String settings;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}