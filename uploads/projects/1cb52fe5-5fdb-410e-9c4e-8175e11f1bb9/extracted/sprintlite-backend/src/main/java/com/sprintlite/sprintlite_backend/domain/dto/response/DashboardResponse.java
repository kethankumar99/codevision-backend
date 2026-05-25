package com.sprintlite.sprintlite_backend.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    // Stats
    private Long totalProjects;
    private Long totalTasks;
    private Long completedTasks;
    private Long inProgressTasks;
    private Long pendingTasks;
    private Long overdueTasks;
    
    // Team stats
    private Long totalMembers;
    private Long activeMembers;
    private Long presentToday;
    
    // Progress
    private Integer overallProgress;
    
    // Recent data
    private List<TaskResponse> recentTasks;
    private List<ProjectResponse> recentProjects;
    private List<NotificationResponse> recentNotifications;
    
    // Chart data
    private Map<String, Long> taskStatusDistribution;
    private Map<String, Long> taskPriorityDistribution;
    private List<Map<String, Object>> weeklyProgress;
}