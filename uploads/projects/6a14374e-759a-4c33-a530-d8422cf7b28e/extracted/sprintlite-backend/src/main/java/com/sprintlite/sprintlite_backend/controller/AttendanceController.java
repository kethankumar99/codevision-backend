package com.sprintlite.sprintlite_backend.controller;

import com.sprintlite.sprintlite_backend.domain.dto.request.AttendanceRequest;
import com.sprintlite.sprintlite_backend.domain.dto.response.AttendanceResponse;
import com.sprintlite.sprintlite_backend.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance tracking APIs")
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    
    @PostMapping("/checkin")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check-in for the day")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.checkIn(request));
    }
    
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check-out for the day")
    public ResponseEntity<AttendanceResponse> checkOut() {
        return ResponseEntity.ok(attendanceService.checkOut());
    }
    
    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get today's attendance")
    public ResponseEntity<AttendanceResponse> getTodayAttendance() {
        return ResponseEntity.ok(attendanceService.getTodayAttendance());
    }
    
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my attendance history")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendance(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getMyAttendance(startDate, endDate));
    }
    
    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'PROJECT_MANAGER')")
    @Operation(summary = "Get team attendance (Admin only)")
    public ResponseEntity<List<AttendanceResponse>> getTeamAttendance(
            @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getTeamAttendance(date));
    }
}