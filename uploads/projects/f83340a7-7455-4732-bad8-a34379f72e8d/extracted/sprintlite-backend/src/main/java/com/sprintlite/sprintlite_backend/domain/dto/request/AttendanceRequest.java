package com.sprintlite.sprintlite_backend.domain.dto.request;



import lombok.Data;

@Data
public class AttendanceRequest {
    private String location;
    private String notes;
    private Double latitude;
    private Double longitude;
}
