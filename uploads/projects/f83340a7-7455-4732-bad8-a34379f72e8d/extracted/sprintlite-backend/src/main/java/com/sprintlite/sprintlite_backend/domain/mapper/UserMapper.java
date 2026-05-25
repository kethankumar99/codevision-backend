package com.sprintlite.sprintlite_backend.domain.mapper;

import org.springframework.stereotype.Component;

import com.sprintlite.sprintlite_backend.domain.dto.response.UserResponse;
import com.sprintlite.sprintlite_backend.domain.entity.User;

@Component
public class UserMapper {
    
    public UserResponse toResponse(User user) {
        if (user == null) return null;
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .theme(user.getTheme())
                .isActive(user.getIsActive())
                .build();
    }
}