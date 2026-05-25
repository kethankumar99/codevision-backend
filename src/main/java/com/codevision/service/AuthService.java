package com.codevision.service;

import com.codevision.dto.AuthResponse;
import com.codevision.dto.LoginRequest;
import com.codevision.dto.RegisterRequest;
import com.codevision.model.User;
import com.codevision.repository.UserRepository;
import com.codevision.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        
        log.info("✅ AuthService initialized successfully");
    }
    
    public AuthResponse register(RegisterRequest request) {
        log.debug("Starting registration process for email: {}", request.getEmail());
        
        // Check if email exists
        log.debug("Checking if email already exists: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - Email already registered: {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }
        
        // Check if username exists
        log.debug("Checking if username already exists: {}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - Username already taken: {}", request.getUsername());
            throw new RuntimeException("Username already taken");
        }
        
        // Create user
        log.debug("Building user entity...");
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(true)
                .emailVerified(false)
                .planType("FREE")
                .build();
        
        log.debug("Saving user to database...");
        user = userRepository.save(user);
        
        log.info("User saved successfully - userId: {}, email: {}", user.getId(), user.getEmail());
        
        // Generate tokens
        log.debug("Generating access token for userId: {}", user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        log.debug("Access token generated - length: {} chars", accessToken.length());
        
        log.debug("Generating refresh token for userId: {}", user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        log.debug("Refresh token generated - length: {} chars", refreshToken.length());
        
        log.info("Registration complete for userId: {}", user.getId());
        return buildAuthResponse(user, accessToken, refreshToken);
    }
    
    public AuthResponse login(LoginRequest request) {
    String loginInput = request.getEmail(); // This can be email OR username
    log.debug("Starting login process for: {}", loginInput);
    
    // Try find by email first, then by username
    User user = userRepository.findByEmail(loginInput)
            .orElseGet(() -> userRepository.findByUsername(loginInput)
            .orElseThrow(() -> {
                log.warn("Login failed - User not found: {}", loginInput);
                return new RuntimeException("Invalid email/username or password");
            }));
    
    log.debug("User found - userId: {}, checking password...", user.getId());
    
    // Check password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        log.warn("Login failed - Invalid password for userId: {}", user.getId());
        throw new RuntimeException("Invalid email/username or password");
    }
    
    log.debug("Password matched for userId: {}", user.getId());
    
    // Check if active
    if (!user.getIsActive()) {
        log.warn("Login failed - Account deactivated for userId: {}", user.getId());
        throw new RuntimeException("Account is deactivated");
    }
    
    // Update last login
    log.debug("Updating last login timestamp for userId: {}", user.getId());
    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);
    
    // Generate tokens
    log.debug("Generating access token for userId: {}", user.getId());
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
    
    log.debug("Generating refresh token for userId: {}", user.getId());
    String refreshToken = jwtService.generateRefreshToken(user.getId());
    
    log.info("Login successful - userId: {}, email: {}", user.getId(), user.getEmail());
    return buildAuthResponse(user, accessToken, refreshToken);
}
    
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        log.debug("Building AuthResponse for userId: {}", user.getId());
        
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .planType(user.getPlanType())
                .build();
        
        log.debug("UserInfo built - planType: {}, email: {}", user.getPlanType(), user.getEmail());
        
        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L) // 24 hours
                .user(userInfo)
                .build();
        
        log.debug("AuthResponse built successfully");
        return response;
    }
}