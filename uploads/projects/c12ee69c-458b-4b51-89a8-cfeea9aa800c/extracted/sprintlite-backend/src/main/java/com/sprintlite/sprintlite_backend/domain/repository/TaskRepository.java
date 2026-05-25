package com.sprintlite.sprintlite_backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlite.sprintlite_backend.domain.entity.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    Page<Task> findByProjectId(UUID projectId, Pageable pageable);
    
    List<Task> findByAssigneeId(UUID assigneeId);
    
    List<Task> findByProjectIdAndStatus(UUID projectId, String status);
    
    Optional<Task> findByTaskKey(String taskKey);
    
    Page<Task> findByProjectIdAndStatusIn(UUID projectId, List<String> statuses, Pageable pageable);
    
    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId AND t.status != 'completed' ORDER BY t.dueDate ASC")
    List<Task> findPendingTasksByAssignee(@Param("assigneeId") UUID assigneeId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") String status);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);
    
    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_DATE AND t.status != 'completed'")
    List<Task> findOverdueTasks();
    
    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = :status WHERE t.id = :taskId")
    void updateTaskStatus(@Param("taskId") UUID taskId, @Param("status") String status);
}