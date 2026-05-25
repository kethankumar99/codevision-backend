package com.sprintlite.sprintlite_backend.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {
    
    private final UserRepository userRepository;
    
    /**
     * Get currently authenticated user from Security Context
     * @return User entity of current authenticated user
     * @throws RuntimeException if user is not authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
    }
    
    /**
     * Get current user ID
     * @return UUID of current authenticated user
     */
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    /**
     * Get current user's company ID
     * @return UUID of current user's company
     */
    public UUID getCurrentCompanyId() {
        return getCurrentUser().getCompany().getId();
    }
    
    /**
     * Get current user's email
     * @return Email of current authenticated user
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
    
    /**
     * Get current user's role
     * @return Role of current authenticated user
     */
    public String getCurrentUserRole() {
        User user = getCurrentUser();
        return user.getRole();
    }
    
    /**
     * Check if current user has specific role
     * @param role Role to check (e.g., "COMPANY_ADMIN", "PROJECT_MANAGER")
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        User user = getCurrentUser();
        return user.getRole().equals(role);
    }
    
    /**
     * Check if current user is SUPER_ADMIN
     * @return true if user is super admin
     */
    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }
    
    /**
     * Check if current user is COMPANY_ADMIN
     * @return true if user is company admin
     */
    public boolean isCompanyAdmin() {
        return hasRole("COMPANY_ADMIN");
    }
    
    /**
     * Check if current user is PROJECT_MANAGER
     * @return true if user is project manager
     */
    public boolean isProjectManager() {
        return hasRole("PROJECT_MANAGER");
    }
    
    /**
     * Check if current user is TEAM_LEAD
     * @return true if user is team lead
     */
    public boolean isTeamLead() {
        return hasRole("TEAM_LEAD");
    }
    
    /**
     * Check if current user is EMPLOYEE
     * @return true if user is employee
     */
    public boolean isEmployee() {
        return hasRole("EMPLOYEE");
    }
    
    /**
     * Check if current user is CLIENT
     * @return true if user is client
     */
    public boolean isClient() {
        return hasRole("CLIENT");
    }
    
    /**
     * Get authentication object from security context
     * @return Authentication object
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * Get UserDetails of current user
     * @return UserDetails object
     */
    public UserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * Check if user is authenticated
     * @return true if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !(authentication.getPrincipal() instanceof String 
                && authentication.getPrincipal().equals("anonymousUser"));
    }
    
    /**
     * Clear security context (for logout)
     */
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
    
    /**
     * Validate if current user has access to a specific company
     * @param companyId Company ID to check access for
     * @return true if user belongs to the company
     */
    public boolean hasCompanyAccess(UUID companyId) {
        if (isSuperAdmin()) {
            return true; // Super admin has access to all companies
        }
        UUID currentCompanyId = getCurrentCompanyId();
        return currentCompanyId.equals(companyId);
    }
    
    /**
     * Validate if current user has access to a specific project
     * @param projectOwnerCompanyId Company ID that owns the project
     * @return true if user belongs to the project's company
     */
    public boolean hasProjectAccess(UUID projectOwnerCompanyId) {
        return hasCompanyAccess(projectOwnerCompanyId);
    }
    
    /**
     * Get current user's company subdomain
     * @return Company subdomain
     */
    public String getCurrentCompanySubdomain() {
        return getCurrentUser().getCompany().getSubdomain();
    }
}