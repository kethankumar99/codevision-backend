package com.codevision.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    
    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;
    
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        
        // Decode Base64 secret
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        
        log.info("✅ JwtService initialized - Token expiration: {}ms", expirationMs);
    }
    
    public String generateAccessToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
        
        log.debug("🎫 Access token generated for user: {} (expires: {})", email, expiryDate);
        return token;
    }
    
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);
        
        String token = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
        
        log.debug("🔄 Refresh token generated for userId: {}", userId);
        return token;
    }
    
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("❌ Failed to parse token: {}", e.getMessage());
            throw new RuntimeException("Invalid token");
        }
    }
    
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.get("email", String.class);
        } catch (JwtException e) {
            log.error("❌ Failed to parse token: {}", e.getMessage());
            throw new RuntimeException("Invalid token");
        }
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            log.debug("✅ Token validated successfully");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⏰ Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("❌ Invalid token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("💥 Token validation error: {}", e.getMessage());
            return false;
        }
    }
}