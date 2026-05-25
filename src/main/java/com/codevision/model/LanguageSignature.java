package com.codevision.model;


import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LanguageSignature {
    
    private String name;           // "Java", "Python", etc.
    private String platform;       // "JVM", "Native", ".NET", etc.
    private String frameworks;     // "Spring Boot, Micronaut"
    private List<String> extensions;     // [".java", ".class"]
    private List<String> configFiles;    // ["pom.xml", "build.gradle"]
    private List<String> patterns;       // Code patterns to identify
    private String emoji;          // "☕", "🐍", etc.
}