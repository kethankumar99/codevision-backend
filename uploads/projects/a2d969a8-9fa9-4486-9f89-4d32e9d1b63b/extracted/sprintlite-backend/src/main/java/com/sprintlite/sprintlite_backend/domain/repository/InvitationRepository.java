package com.sprintlite.sprintlite_backend.domain.repository;



import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sprintlite.sprintlite_backend.domain.entity.Invitation;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    
    Optional<Invitation> findByTokenAndStatus(String token, String status);
    
    Optional<Invitation> findByEmailAndStatus(String email, String status);
    
    boolean existsByEmailAndStatus(String email, String status);
}
