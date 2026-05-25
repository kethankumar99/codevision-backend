package com.sprintlite.sprintlite_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sprintlite.sprintlite_backend.domain.dto.response.UserResponse;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.mapper.UserMapper;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.exception.ResourceNotFoundException;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    
    public UserResponse getCurrentUser() {
        User currentUser = securityUtils.getCurrentUser();
        return userMapper.toResponse(currentUser);
    }
    
    public Page<UserResponse> getUsers(Pageable pageable) {
        User currentUser = securityUtils.getCurrentUser();
        Page<User> users = userRepository.findByCompanyId(currentUser.getCompany().getId(), pageable);
        return users.map(userMapper::toResponse);
    }
    
    public UserResponse getUser(UUID userId) {
        User currentUser = securityUtils.getCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (!user.getCompany().getId().equals(currentUser.getCompany().getId())) {
            throw new ResourceNotFoundException("User not found");
        }
        
        return userMapper.toResponse(user);
    }
}