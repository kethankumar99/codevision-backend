package com.sprintlite.sprintlite_backend.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sprintlite.sprintlite_backend.domain.dto.response.DashboardResponse;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.mapper.NotificationMapper;
import com.sprintlite.sprintlite_backend.domain.mapper.TaskMapper;
import com.sprintlite.sprintlite_backend.domain.repository.AttendanceRepository;
import com.sprintlite.sprintlite_backend.domain.repository.NotificationRepository;
import com.sprintlite.sprintlite_backend.domain.repository.ProjectRepository;
import com.sprintlite.sprintlite_backend.domain.repository.TaskRepository;
import com.sprintlite.sprintlite_backend.domain.repository.UserRepository;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskMapper taskMapper;
    private final NotificationMapper notificationMapper;
    private final SecurityUtils securityUtils;
    
    public DashboardResponse getDashboardStats() {
        User currentUser = securityUtils.getCurrentUser();
        UUID companyId = currentUser.getCompany().getId();
        
        // Project stats
        Long totalProjects = projectRepository.countByCompanyId(companyId);
        
        // Task stats
        Long totalTasks = taskRepository.count();
        Long completedTasks = 0L;
        Long inProgressTasks = 0L;
        Long pendingTasks = 0L;
        Long overdueTasks = (long) taskRepository.findOverdueTasks().size();
        
        // Get all tasks for the company and calculate stats
        // This is simplified - you'd want to add company filtering
        
        // Team stats
        Long totalMembers = userRepository.countActiveUsersByCompanyId(companyId);
        Long presentToday = attendanceRepository.countPresentToday(companyId, LocalDate.now());
        
        // Recent data
        var recentTasks = taskRepository.findAll(PageRequest.of(0, 5))
                .stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
        
        var recentNotifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(0, 5))
                .map(notificationMapper::toResponse)
                .getContent();
        
        // Task distribution
        Map<String, Long> taskStatusDistribution = new HashMap<>();
        taskStatusDistribution.put("todo", 0L);
        taskStatusDistribution.put("in_progress", 0L);
        taskStatusDistribution.put("in_review", 0L);
        taskStatusDistribution.put("completed", 0L);
        
        Map<String, Long> taskPriorityDistribution = new HashMap<>();
        taskPriorityDistribution.put("low", 0L);
        taskPriorityDistribution.put("medium", 0L);
        taskPriorityDistribution.put("high", 0L);
        taskPriorityDistribution.put("urgent", 0L);
        
        return DashboardResponse.builder()
                .totalProjects(totalProjects)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .pendingTasks(pendingTasks)
                .overdueTasks(overdueTasks)
                .totalMembers(totalMembers)
                .activeMembers(totalMembers)
                .presentToday(presentToday)
                .overallProgress(calculateOverallProgress())
                .recentTasks(recentTasks)
                .recentProjects(null)
                .recentNotifications(recentNotifications)
                .taskStatusDistribution(taskStatusDistribution)
                .taskPriorityDistribution(taskPriorityDistribution)
                .weeklyProgress(null)
                .build();
    }
    
    private Integer calculateOverallProgress() {
        // Calculate overall progress based on completed tasks vs total tasks
        Long totalTasks = taskRepository.count();
        if (totalTasks == 0) return 0;
        Long completedTasks = taskRepository.countByProjectIdAndStatus(null, "completed");
        return (int) ((completedTasks * 100) / totalTasks);
    }
}