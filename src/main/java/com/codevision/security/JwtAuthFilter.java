package com.codevision.security;

import com.codevision.model.User;
import com.codevision.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        log.debug("🔍 Filter: {} {}", method, uri);
        
        // SKIP: Public endpoints
        if (uri.startsWith("/api/auth/") || 
            uri.startsWith("/api/health") ||
            uri.startsWith("/api/test/") ||
            uri.startsWith("/swagger-ui") ||
            uri.startsWith("/v3/api-docs")) {
            log.debug("⏭️ Public endpoint, skipping filter");
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract token
        String token = extractToken(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("❌ No token in request: {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }
        
        log.debug("🔑 Token found: {}...", token.substring(0, Math.min(20, token.length())));
        
        try {
            // Validate token
            if (!jwtService.validateToken(token)) {
                log.warn("❌ Invalid token");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or expired token");
                return;
            }
            
            // Get user ID from token
            String userId = jwtService.getUserIdFromToken(token);
            log.debug("👤 User ID from token: {}", userId);
            
            // Find user
            Optional<User> userOptional = userRepository.findById(UUID.fromString(userId));
            
            if (userOptional.isEmpty()) {
                log.warn("❌ User not found: {}", userId);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "User not found");
                return;
            }
            
            User user = userOptional.get();
            
            // Set authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.info("✅ User authenticated: {} ({} {})", user.getEmail(), method, uri);
            
        } catch (Exception e) {
            log.error("💥 Auth error: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication failed");
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        log.debug("🔍 Auth header: {}", authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "NULL");
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}