package com.sprintlite.sprintlite_backend.domain.repository;

import com.sprintlite.sprintlite_backend.domain.entity.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;

@Repository
public class ProjectMemberRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @SuppressWarnings("unchecked")
    public List<User> findUsersByProjectId(UUID projectId) {
        String sql = "SELECT u.* FROM users u " +
                     "INNER JOIN project_members pm ON u.id = pm.user_id " +
                     "WHERE pm.project_id = :projectId AND u.deleted_at IS NULL";
        
        Query query = entityManager.createNativeQuery(sql, User.class);
        query.setParameter("projectId", projectId);
        return query.getResultList();
    }
    
    @Transactional
    public void addMember(UUID projectId, UUID userId, String role) {
        String sql = "INSERT INTO project_members (id, project_id, user_id, role, joined_at) " +
                     "VALUES (gen_random_uuid(), :projectId, :userId, :role, CURRENT_TIMESTAMP)";
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("projectId", projectId);
        query.setParameter("userId", userId);
        query.setParameter("role", role);
        query.executeUpdate();
    }
    
    @Transactional
    public void deleteByProjectIdAndUserId(UUID projectId, UUID userId) {
        String sql = "DELETE FROM project_members WHERE project_id = :projectId AND user_id = :userId";
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("projectId", projectId);
        query.setParameter("userId", userId);
        query.executeUpdate();
    }
    
    // --- ఇక్కడ మార్పు చేసాం (ClassCastException Fix) ---
    public boolean existsByProjectIdAndUserId(UUID projectId, UUID userId) {
        // క్వెరీ లో కేవలం COUNT(*) మాత్రమే తీసుకుంటున్నాం (ఇది ఒక Number ని ఇస్తుంది)
        String sql = "SELECT COUNT(*) FROM project_members WHERE project_id = :projectId AND user_id = :userId";
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("projectId", projectId);
        query.setParameter("userId", userId);
        
        Object result = query.getSingleResult();
        
        // కౌంట్ 0 కంటే ఎక్కువ ఉంటే true, లేదంటే false రిటర్న్ అవుతుంది
        return ((Number) result).longValue() > 0;
    }
    
    @Transactional
    public void updateMemberRole(UUID projectId, UUID userId, String role) {
        String sql = "UPDATE project_members SET role = :role WHERE project_id = :projectId AND user_id = :userId";
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("role", role);
        query.setParameter("projectId", projectId);
        query.setParameter("userId", userId);
        query.executeUpdate();
    }
}