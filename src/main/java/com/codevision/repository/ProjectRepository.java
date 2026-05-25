package com.codevision.repository;

import com.codevision.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    List<Project> findByUserId(UUID userId);
    
    List<Project> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    long countByUserId(UUID userId);
}