package com.sprintlite.sprintlite_backend.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DateUtils {
    
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DEFAULT_FORMATTER);
    }
    
    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    
    public static boolean isOverdue(LocalDate dueDate) {
        if (dueDate == null) return false;
        return dueDate.isBefore(LocalDate.now());
    }
    
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }
    
    public static List<String> getLast7Days() {
        LocalDate today = LocalDate.now();
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> today.minusDays(i).toString())
                .collect(java.util.stream.Collectors.toList());
    }
}