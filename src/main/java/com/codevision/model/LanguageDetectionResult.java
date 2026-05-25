package com.codevision.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDetectionResult {
    
    private String primaryLanguage;      // "Java"
    private String languageEmoji;        // "☕"
    private double confidence;           // 0.95 (95%)
    private String platform;             // "JVM"
    private String detectedFramework;    // "Spring Boot"
    private List<String> secondaryLanguages;  // ["Kotlin", "XML"]
    private int totalFiles;
    private int languageFiles;
    private String error;
    private boolean success;
    
    public static LanguageDetectionResult unknown() {
        return LanguageDetectionResult.builder()
                .primaryLanguage("Unknown")
                .languageEmoji("❓")
                .confidence(0.0)
                .platform("Unknown")
                .secondaryLanguages(new ArrayList<>())
                .success(false)
                .error("Could not detect language")
                .build();
    }
    
    public static LanguageDetectionResult error(String message) {
        return LanguageDetectionResult.builder()
                .success(false)
                .error(message)
                .primaryLanguage("Error")
                .languageEmoji("⚠️")
                .build();
    }
    
    public String getConfidencePercent() {
        return String.format("%.1f%%", confidence * 100);
    }
}