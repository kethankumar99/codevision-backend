package com.codevision.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project upload response")
public class ProjectUploadResponse {
    
    @Schema(description = "Unique project ID")
    private String projectId;
    
    @Schema(description = "Project name")
    private String projectName;
    
    @Schema(description = "Original ZIP filename")
    private String originalFilename;
    
    @Schema(description = "Current status (UPLOADED, EXTRACTING, EXTRACTED, ANALYZING, COMPLETED)")
    private String status;
    
    @Schema(description = "Total files in project")
    private int totalFiles;
    
    @Schema(description = "Success/Error message")
    private String message;
    
    @Schema(description = "Request tracking ID")
    private String requestId;
    
    @Schema(description = "When project was created")
    private String createdAt;
    
    @Schema(description = "When analysis was completed")
    private String analyzedAt;
}