package com.sprintlite.sprintlite_backend.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sprintlite.sprintlite_backend.domain.entity.Attendance;

// Add to AttendanceRepository.java
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    
    Optional<Attendance> findByUserIdAndDate(UUID userId, LocalDate date);
    
    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.date BETWEEN :startDate AND :endDate")
    List<Attendance> findByUserIdAndDateBetween(@Param("userId") UUID userId, 
                                                 @Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate);
    
    @Query("SELECT a FROM Attendance a WHERE a.company.id = :companyId AND a.date = :date")
    List<Attendance> findByCompanyIdAndDate(@Param("companyId") UUID companyId, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.company.id = :companyId AND a.date = :date AND a.checkInTime IS NOT NULL")
    long countPresentToday(@Param("companyId") UUID companyId, @Param("date") LocalDate date);
}