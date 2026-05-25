package com.sprintlite.sprintlite_backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sprintlite.sprintlite_backend.domain.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    Optional<Company> findBySubdomain(String subdomain);
    
    boolean existsBySubdomain(String subdomain);
    
    Optional<Company> findByName(String name);
}