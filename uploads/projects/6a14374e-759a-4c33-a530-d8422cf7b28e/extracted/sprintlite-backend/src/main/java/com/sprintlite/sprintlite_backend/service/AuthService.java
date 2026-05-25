package com.sprintlite.sprintlite_backend.service;

import com.sprintlite.sprintlite_backend.domain.dto.request.ChangePasswordRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.LoginRequest;
import com.sprintlite.sprintlite_backend.domain.dto.request.RegisterRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.AuthResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Company;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.repository.CompanyRepository;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.exception.BadRequestException;
import com.sprintlite.sprintlite_backend.exception.ResourceNotFoundException;
import com.sprintlite.sprintlite_backend.security.JwtTokenProvider;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ProjectMemberService projectMemberService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("🚀 Starting registration for email: {}", request.getEmail());
        
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("❌ Email already exists: {}", request.getEmail());
            throw new BadRequestException("Email already exists");
        }
        
        // Create company
        String subdomain = request.getCompanyName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-");
        
        if (companyRepository.existsBySubdomain(subdomain)) {
            subdomain = subdomain + "-" + System.currentTimeMillis();
        }
        
        log.info("🏢 Creating company: {} with subdomain: {}", request.getCompanyName(), subdomain);
        
        Company company = Company.builder()
                .name(request.getCompanyName())
                .subdomain(subdomain)
                .planType("free")
               // .subscriptionStatus("active")
                .maxUsers(100)
                .maxProjects(50)
                .maxStorageGb(5)
                .isActive(true)
                .settings("{}")
                .build();
        company = companyRepository.save(company);
        log.info("✅ Company created with ID: {}", company.getId());
        
        // Create user
        log.info("👤 Creating user: {}", request.getEmail());
        
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .company(company)
                .role("COMPANY_ADMIN")
                .isActive(true)
                .isEmailVerified(true)
                .theme("dark")
                .language("en")
                .preferences("{}")
                .permissions("[]")
                .build();
        user = userRepository.save(user);
        log.info("✅ User created with ID: {}", user.getId());
        
        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        long expiresIn = tokenProvider.getExpirationInMs();
        
        log.info("🎉 Registration completed for: {}", request.getEmail());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        log.info("🔐 Login attempt for email: {}", request.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("❌ User not found: {}", request.getEmail());
                    return new ResourceNotFoundException("User not found");
                });
        
        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        long expiresIn = tokenProvider.getExpirationInMs();
        
        log.info("✅ User logged in: {}", request.getEmail());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
    
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        log.info("🔑 Password change for user: {}", currentUser.getEmail());
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            log.warn("❌ Current password is incorrect for user: {}", currentUser.getEmail());
            throw new BadRequestException("Current password is incorrect");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("❌ New password and confirmation do not match");
            throw new BadRequestException("New password and confirmation do not match");
        }
        
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setIsEmailVerified(true);
        userRepository.save(currentUser);
        
        log.info("✅ Password changed successfully for user: {}", currentUser.getEmail());
    }
    
    @Transactional
    public AuthResponse acceptInvitation(String token, String firstName, String lastName, String password) {
        log.info("📧 Accepting invitation with token: {}", token);
        
        User user = projectMemberService.acceptInvitation(token, firstName, lastName, password);
        
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        
        log.info("✅ Invitation accepted for user: {}", user.getEmail());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationInMs())
                .build();
    }
}