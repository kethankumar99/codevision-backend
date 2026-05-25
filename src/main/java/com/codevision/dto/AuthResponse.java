package com.codevision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    
    @Builder.Default // <--- Ee annotation add cheyandi, warning pothundi!
    private String tokenType = "Bearer";
    
    private Long expiresIn;
    
    private UserInfo user;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String username;
        private String fullName;
        private String planType;
    }
}