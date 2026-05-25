package com.sprintlite.sprintlite_backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sprintlite.sprintlite_backend.domain.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    Page<Project> findByCompanyId(UUID companyId, Pageable pageable);
    
    List<Project> findByCompanyIdAndStatus(UUID companyId, String status);
    
    Optional<Project> findByKeyCodeAndCompanyId(String keyCode, UUID companyId);
    
    boolean existsByKeyCodeAndCompanyId(String keyCode, UUID companyId);
    
    @Query("SELECT p FROM Project p WHERE p.company.id = :companyId AND p.status = 'active'")
    List<Project> findActiveProjectsByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(p) FROM Project p WHERE p.company.id = :companyId")
    long countByCompanyId(@Param("companyId") UUID companyId);
}