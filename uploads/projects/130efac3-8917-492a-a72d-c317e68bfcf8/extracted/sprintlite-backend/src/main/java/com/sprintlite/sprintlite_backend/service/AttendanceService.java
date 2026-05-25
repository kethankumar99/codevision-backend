package com.sprintlite.sprintlite_backend.service;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintlite.sprintlite_backend.domain.dto.request.AttendanceRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.AttendanceResponse;
import com.sprintlite.sprintlite_backend.domain.entity.Attendance;
import com.sprintlite.sprintlite_backend.domain.entity.User;
import com.sprintlite.sprintlite_backend.domain.repository.AttendanceRepository;
import com.sprintlite.sprintlite_backend.exception.BadRequestException;
import com.sprintlite.sprintlite_backend.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    
    private final AttendanceRepository attendanceRepository;
    private final SecurityUtils securityUtils;
    
    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        LocalDate today = LocalDate.now();
        
        // Check if already checked in today
        Attendance existing = attendanceRepository.findByUserIdAndDate(currentUser.getId(), today).orElse(null);
        
        if (existing != null && existing.getCheckInTime() != null) {
            throw new BadRequestException("Already checked in today");
        }
        
        Attendance attendance;
        if (existing == null) {
            attendance = Attendance.builder()
                    .user(currentUser)
                    .company(currentUser.getCompany())
                    .date(today)
                    .checkInTime(LocalDateTime.now())
                    .checkInLocation(request.getLocation())
                    .status("present")
                    .workedHours(0.0)
                    .overtimeHours(0.0)
                    .build();
        } else {
            attendance = existing;
            attendance.setCheckInTime(LocalDateTime.now());
            attendance.setCheckInLocation(request.getLocation());
        }
        
        attendance = attendanceRepository.save(attendance);
        return mapToResponse(attendance);
    }
    
    @Transactional
    public AttendanceResponse checkOut() {
        User currentUser = securityUtils.getCurrentUser();
        LocalDate today = LocalDate.now();
        
        Attendance attendance = attendanceRepository.findByUserIdAndDate(currentUser.getId(), today)
                .orElseThrow(() -> new BadRequestException("No check-in found for today"));
        
        if (attendance.getCheckOutTime() != null) {
            throw new BadRequestException("Already checked out today");
        }
        
        LocalDateTime checkOutTime = LocalDateTime.now();
        attendance.setCheckOutTime(checkOutTime);
        
        // Calculate worked hours
        if (attendance.getCheckInTime() != null) {
            double hours = java.time.Duration.between(attendance.getCheckInTime(), checkOutTime).toMinutes() / 60.0;
            attendance.setWorkedHours(Math.round(hours * 100.0) / 100.0);
            
            // Calculate overtime (assuming 8 hours standard)
            if (hours > 8) {
                attendance.setOvertimeHours(Math.round((hours - 8) * 100.0) / 100.0);
            }
        }
        
        attendance = attendanceRepository.save(attendance);
        return mapToResponse(attendance);
    }
    
    public AttendanceResponse getTodayAttendance() {
        User currentUser = securityUtils.getCurrentUser();
        Attendance attendance = attendanceRepository.findByUserIdAndDate(currentUser.getId(), LocalDate.now()).orElse(null);
        return attendance != null ? mapToResponse(attendance) : null;
    }
    
    public List<AttendanceResponse> getMyAttendance(LocalDate startDate, LocalDate endDate) {
        User currentUser = securityUtils.getCurrentUser();
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        
        List<Attendance> attendances = attendanceRepository.findByUserIdAndDateBetween(currentUser.getId(), startDate, endDate);
        return attendances.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    public List<AttendanceResponse> getTeamAttendance(LocalDate date) {
        User currentUser = securityUtils.getCurrentUser();
        if (date == null) date = LocalDate.now();
        
        List<Attendance> attendances = attendanceRepository.findByCompanyIdAndDate(currentUser.getCompany().getId(), date);
        return attendances.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    private AttendanceResponse mapToResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .date(attendance.getDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .checkInLocation(attendance.getCheckInLocation())
                .checkOutLocation(attendance.getCheckOutLocation())
                .status(attendance.getStatus())
                .workedHours(attendance.getWorkedHours())
                .overtimeHours(attendance.getOvertimeHours())
                .remarks(attendance.getRemarks())
                .build();
    }
}
