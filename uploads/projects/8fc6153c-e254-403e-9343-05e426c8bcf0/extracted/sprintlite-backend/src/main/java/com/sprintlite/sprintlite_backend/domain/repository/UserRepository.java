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

import com.sprintlite.sprintlite_backend.domain.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByEmailAndCompanyId(String email, UUID companyId);
    
    Page<User> findByCompanyId(UUID companyId, Pageable pageable);
    
    List<User> findByCompanyIdAndRole(UUID companyId, String role);
    
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.isActive = true")
    List<User> findActiveUsersByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId AND u.isActive = true")
    long countActiveUsersByCompanyId(@Param("companyId") UUID companyId);
}