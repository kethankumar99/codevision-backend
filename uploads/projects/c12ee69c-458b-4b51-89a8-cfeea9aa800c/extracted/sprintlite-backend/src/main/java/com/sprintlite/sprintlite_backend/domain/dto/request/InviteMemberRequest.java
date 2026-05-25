package com.sprintlite.sprintlite_backend.domain.dto.request;



import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class InviteMemberRequest {
    
    @NotEmpty(message = "At least one email is required")
    private List<@Email String> emails;
    
    private String role = "EMPLOYEE";  // EMPLOYEE, TEAM_LEAD
    
    private String message;
    
    private Boolean sendEmail = true;  // Whether to send email invitation
}