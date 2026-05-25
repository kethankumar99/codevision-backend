package com.sprintlite.sprintlite_backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sprintlite.sprintlite_backend.domain.entity.Sprint;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByProjectId(UUID projectId);
    List<Sprint> findByProjectIdAndStatus(UUID projectId, String status);
}