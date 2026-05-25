package com.sprintlite.sprintlite_backend.domain.dto.response;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponse {
    private List<InvitedUserInfo> invitedUsers;
    private List<String> failedEmails;
    private int successCount;
    private int failedCount;
    private String message;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitedUserInfo {
        private String email;
        private String temporaryPassword;
        private String invitationLink;
        private boolean isNewUser;
    }
}