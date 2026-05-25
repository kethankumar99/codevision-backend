package com.codevision.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.codevision.model.LanguageDetectionResult;
import com.codevision.model.LanguageSignature;

@Service
public class LanguageDetectionService {

    private static final Logger log = LoggerFactory.getLogger(LanguageDetectionService.class);

    // ============================================
    // LANGUAGE SIGNATURES MAP
    // ============================================
    
    private static final Map<String, LanguageSignature> LANGUAGE_SIGNATURES = new LinkedHashMap<>();
    
    static {
        // Java & JVM Languages
        LANGUAGE_SIGNATURES.put("Java", new LanguageSignature(
                "Java", "JVM", "Spring Boot, Micronaut, Quarkus",
                List.of(".java"),
                List.of("pom.xml", "build.gradle", "build.gradle.kts", ".mvn"),
                List.of("@SpringBootApplication", "@RestController", "@Service", "@Repository",
                        "public class", "import java.", "package ", "javax.", "jakarta."),
                "☕"
        ));
        
        LANGUAGE_SIGNATURES.put("Kotlin", new LanguageSignature(
                "Kotlin", "JVM", "Spring Boot, Ktor",
                List.of(".kt", ".kts"),
                List.of("build.gradle.kts"),
                List.of("fun ", "val ", "var ", "import kotlin.", "package "),
                "🟣"
        ));
        
        LANGUAGE_SIGNATURES.put("Scala", new LanguageSignature(
                "Scala", "JVM", "Play, Akka",
                List.of(".scala"),
                List.of("build.sbt"),
                List.of("object ", "def ", "val ", "import scala."),
                "🔴"
        ));

        // Python Family
        LANGUAGE_SIGNATURES.put("Python", new LanguageSignature(
                "Python", "Native", "Flask, FastAPI, Django",
                List.of(".py", ".pyw", ".pyx"),
                List.of("requirements.txt", "setup.py", "Pipfile", "pyproject.toml"),
                List.of("import ", "from ", "def ", "class ", "if __name__",
                        "@app.route", "@app.get", "@app.post", "print("),
                "🐍"
        ));

        // JavaScript/TypeScript Family
        LANGUAGE_SIGNATURES.put("TypeScript", new LanguageSignature(
                "TypeScript", "Node.js/Browser", "React, Angular, Next.js",
                List.of(".ts", ".tsx"),
                List.of("tsconfig.json", "angular.json"),
                List.of("import {", "export ", "interface ", "type ", ": string", ": number"),
                "💙"
        ));
        
        LANGUAGE_SIGNATURES.put("JavaScript", new LanguageSignature(
                "JavaScript", "Node.js/Browser", "React, Express, Vue",
                List.of(".js", ".jsx", ".mjs", ".cjs"),
                List.of("package.json", "node_modules"),
                List.of("const ", "let ", "var ", "function ", "require(", "module.exports",
                        "import ", "export ", "=>"),
                "💛"
        ));

        // .NET Family
        LANGUAGE_SIGNATURES.put("C#", new LanguageSignature(
                "C#", ".NET", "ASP.NET Core, Blazor",
                List.of(".cs"),
                List.of(".csproj", ".sln", "appsettings.json"),
                List.of("using System", "namespace ", "public class", "[ApiController]",
                        "[HttpGet]", "async Task", "var "),
                "🟪"
        ));
        
        LANGUAGE_SIGNATURES.put("F#", new LanguageSignature(
                "F#", ".NET", "Giraffe, Saturn",
                List.of(".fs", ".fsx"),
                List.of(".fsproj"),
                List.of("let ", "module ", "type ", "match ", "|>"),
                "💜"
        ));
        
        LANGUAGE_SIGNATURES.put("Visual Basic", new LanguageSignature(
                "Visual Basic", ".NET", "ASP.NET",
                List.of(".vb"),
                List.of(".vbproj"),
                List.of("Module ", "Sub ", "Function ", "Dim ", "As "),
                "💚"
        ));

        // C/C++ Family
        LANGUAGE_SIGNATURES.put("C++", new LanguageSignature(
                "C++", "Native", "Qt, Boost",
                List.of(".cpp", ".cc", ".cxx", ".hpp", ".hxx", ".h"),
                List.of("CMakeLists.txt", "Makefile"),
                List.of("#include <", "std::", "namespace ", "template<", "class ", "public:", "private:"),
                "⚡"
        ));
        
        LANGUAGE_SIGNATURES.put("C", new LanguageSignature(
                "C", "Native", "Embedded",
                List.of(".c", ".h"),
                List.of("Makefile"),
                List.of("#include <", "printf(", "scanf(", "malloc(", "typedef struct"),
                "⚙️"
        ));

        // Go
        LANGUAGE_SIGNATURES.put("Go", new LanguageSignature(
                "Go", "Native", "Gin, Echo, Fiber",
                List.of(".go"),
                List.of("go.mod", "go.sum"),
                List.of("package ", "import (", "func ", "go ", "defer ", "goroutine"),
                "🔵"
        ));

        // Rust
        LANGUAGE_SIGNATURES.put("Rust", new LanguageSignature(
                "Rust", "Native", "Actix, Rocket",
                List.of(".rs"),
                List.of("Cargo.toml", "Cargo.lock"),
                List.of("fn ", "let mut", "impl ", "use ", "mod ", "pub ", "struct "),
                "🦀"
        ));

        // Ruby
        LANGUAGE_SIGNATURES.put("Ruby", new LanguageSignature(
                "Ruby", "Native", "Rails, Sinatra",
                List.of(".rb", ".rbw"),
                List.of("Gemfile", "Rakefile", "config.ru"),
                List.of("def ", "class ", "module ", "require ", "puts ", "end"),
                "💎"
        ));

        // PHP
        LANGUAGE_SIGNATURES.put("PHP", new LanguageSignature(
                "PHP", "Native", "Laravel, Symfony",
                List.of(".php", ".phtml", ".php3", ".php4", ".php5"),
                List.of("composer.json", "artisan"),
                List.of("<?php", "namespace ", "use ", "public function", "$"),
                "🐘"
        ));

        // Swift
        LANGUAGE_SIGNATURES.put("Swift", new LanguageSignature(
                "Swift", "Apple", "Vapor, Kitura",
                List.of(".swift"),
                List.of("Package.swift", ".xcodeproj"),
                List.of("import Foundation", "var ", "let ", "func ", "class ", "struct "),
                "🦅"
        ));

        // Shell/Bash
        LANGUAGE_SIGNATURES.put("Shell", new LanguageSignature(
                "Shell", "Unix", "Bash, Zsh",
                List.of(".sh", ".bash", ".zsh"),
                List.of(),
                List.of("#!/bin/bash", "#!/bin/sh", "echo ", "export ", "source "),
                "🐚"
        ));

        // SQL
        LANGUAGE_SIGNATURES.put("SQL", new LanguageSignature(
                "SQL", "Database", "PostgreSQL, MySQL",
                List.of(".sql"),
                List.of(),
                List.of("CREATE TABLE", "SELECT ", "INSERT INTO", "ALTER TABLE", "DROP "),
                "🗄️"
        ));

        // Others
        LANGUAGE_SIGNATURES.put("Dart", new LanguageSignature(
                "Dart", "Native", "Flutter",
                List.of(".dart"),
                List.of("pubspec.yaml"),
                List.of("import 'package:", "void main()", "Widget ", "class ", "final "),
                "🎯"
        ));
        
        LANGUAGE_SIGNATURES.put("R", new LanguageSignature(
                "R", "Native", "Shiny, Plumber",
                List.of(".r", ".R", ".Rmd"),
                List.of(".Rproj"),
                List.of("<-", "library(", "ggplot", "dplyr", "data.frame"),
                "📊"
        ));
        
        LANGUAGE_SIGNATURES.put("Perl", new LanguageSignature(
                "Perl", "Native", "Mojolicious, Catalyst",
                List.of(".pl", ".pm"),
                List.of("Makefile.PL"),
                List.of("#!/usr/bin/perl", "use strict", "my $", "sub ", "print "),
                "🐪"
        ));
        
        LANGUAGE_SIGNATURES.put("Lua", new LanguageSignature(
                "Lua", "Native", "Embedded",
                List.of(".lua"),
                List.of(),
                List.of("function ", "local ", "require(", "print(", "end"),
                "🌙"
        ));
        
        LANGUAGE_SIGNATURES.put("Haskell", new LanguageSignature(
                "Haskell", "Native", "Yesod, Scotty",
                List.of(".hs"),
                List.of("stack.yaml", ".cabal"),
                List.of("import ", "module ", "data ", "type ", "where", "::"),
                "λ"
        ));
    }

    // ============================================
    // MAIN DETECTION METHODS
    // ============================================

    /**
     * Detect primary language from extracted project path
     */
    public LanguageDetectionResult detectProjectLanguage(Path projectPath) {
        log.info("🔍 Detecting languages in: {}", projectPath);
        
        try {
            // Get all files
            List<Path> allFiles = getAllFiles(projectPath);
            log.debug("Total files found: {}", allFiles.size());
            
            // Score each language
            Map<String, Double> languageScores = new HashMap<>();
            
            for (LanguageSignature lang : LANGUAGE_SIGNATURES.values()) {
                double score = calculateLanguageScore(allFiles, lang);
                if (score > 0) {
                    languageScores.put(lang.getName(), score);
                }
            }
            
            // Sort by score
            List<Map.Entry<String, Double>> sorted = languageScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());
            
            if (sorted.isEmpty()) {
                log.warn("⚠️ No language detected - marking as UNKNOWN");
                return LanguageDetectionResult.unknown();
            }
            
            // Get primary and secondary languages
            LanguageSignature primary = LANGUAGE_SIGNATURES.get(sorted.get(0).getKey());
            List<String> secondaryLangs = sorted.stream()
                    .skip(1)
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            // Detect framework
            String framework = detectFramework(allFiles, primary);
            
            LanguageDetectionResult result = LanguageDetectionResult.builder()
                    .primaryLanguage(primary.getName())
                    .languageEmoji(primary.getEmoji())
                    .confidence(sorted.get(0).getValue())
                    .platform(primary.getPlatform())
                    .detectedFramework(framework)
                    .secondaryLanguages(secondaryLangs)
                    .totalFiles(allFiles.size())
                    .languageFiles(countLanguageFiles(allFiles, primary))
                    .build();
            
            log.info("✅ Language detected: {} {} (Confidence: {:.1f}%, Framework: {})",
                    result.getLanguageEmoji(), 
                    result.getPrimaryLanguage(), 
                    result.getConfidence() * 100,
                    result.getDetectedFramework() != null ? result.getDetectedFramework() : "None");
            
            return result;
            
        } catch (IOException e) {
            log.error("❌ Error detecting language: {}", e.getMessage(), e);
            return LanguageDetectionResult.error(e.getMessage());
        }
    }

    /**
     * Calculate how likely this language matches the project
     */
    private double calculateLanguageScore(List<Path> files, LanguageSignature lang) {
        double score = 0.0;
        
        // Check file extensions (weight: 40%)
        long matchingExtensions = files.stream()
                .filter(f -> {
                    String name = f.getFileName().toString().toLowerCase();
                    return lang.getExtensions().stream().anyMatch(name::endsWith);
                })
                .count();
        
        if (matchingExtensions > 0) {
            double extRatio = (double) matchingExtensions / files.size();
            score += extRatio * 0.4;
        }
        
        // Check config files (weight: 30%)
        long matchingConfigs = files.stream()
                .filter(f -> {
                    String name = f.getFileName().toString();
                    return lang.getConfigFiles().stream().anyMatch(name::equals);
                })
                .count();
        
        if (matchingConfigs > 0) {
            double configScore = Math.min(matchingConfigs * 0.15, 0.3);
            score += configScore;
        }
        
        // Check file content (weight: 30%)
        // Sample first 5 files of matching extension
        long contentMatches = files.stream()
                .filter(f -> {
                    String name = f.getFileName().toString().toLowerCase();
                    return lang.getExtensions().stream().anyMatch(name::endsWith);
                })
                .limit(5)
                .filter(f -> hasLanguagePatterns(f, lang))
                .count();
        
        if (contentMatches > 0) {
            double contentScore = (contentMatches / 5.0) * 0.3;
            score += contentScore;
        }
        
        return Math.min(score, 1.0);
    }

    /**
     * Check if file contains language-specific patterns
     */
    private boolean hasLanguagePatterns(Path file, LanguageSignature lang) {
        try {
            String content = Files.readString(file);
            // Check only first 100 lines for performance
            String[] lines = content.split("\n");
            int linesToCheck = Math.min(lines.length, 100);
            
            for (int i = 0; i < linesToCheck; i++) {
                String line = lines[i];
                for (String pattern : lang.getPatterns()) {
                    if (line.contains(pattern)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            // Ignore unreadable files
        }
        return false;
    }

    /**
     * Detect specific framework
     */
    private String detectFramework(List<Path> files, LanguageSignature language) {
        String lang = language.getName();
        
        // Framework detection rules
        Map<String, List<String>> frameworkIndicators = new HashMap<>();
        
        // Java Frameworks
        frameworkIndicators.put("Spring Boot", List.of("@SpringBootApplication", "spring-boot-starter"));
        frameworkIndicators.put("Micronaut", List.of("io.micronaut", "@MicronautTest"));
        frameworkIndicators.put("Quarkus", List.of("io.quarkus", "@QuarkusTest"));
        
        // Python Frameworks
        frameworkIndicators.put("Flask", List.of("from flask import", "@app.route", "Flask(__name__)"));
        frameworkIndicators.put("FastAPI", List.of("from fastapi import", "@app.get", "@app.post"));
        frameworkIndicators.put("Django", List.of("from django.", "urlpatterns", "django.contrib"));
        
        // JavaScript/TypeScript Frameworks
        frameworkIndicators.put("React", List.of("import React", "from 'react'", "useState"));
        frameworkIndicators.put("Express", List.of("require('express')", "app.get(", "app.post("));
        frameworkIndicators.put("Next.js", List.of("import { NextPage }", "getServerSideProps"));
        frameworkIndicators.put("Angular", List.of("@Component", "@NgModule", "@Injectable"));
        
        // .NET Frameworks
        frameworkIndicators.put("ASP.NET Core", List.of("Microsoft.AspNetCore", "[ApiController]"));
        frameworkIndicators.put("Blazor", List.of("@page \"", "Microsoft.AspNetCore.Components"));
        
        // Go Frameworks
        frameworkIndicators.put("Gin", List.of("github.com/gin-gonic/gin", "gin.Default()"));
        frameworkIndicators.put("Echo", List.of("github.com/labstack/echo", "echo.New()"));
        
        // Rust Frameworks
        frameworkIndicators.put("Actix", List.of("use actix_web", "actix-web"));
        frameworkIndicators.put("Rocket", List.of("use rocket", "#[get(\"/"));
        
        // Ruby Frameworks
        frameworkIndicators.put("Rails", List.of("ApplicationController", "ActiveRecord::Base"));
        frameworkIndicators.put("Sinatra", List.of("require 'sinatra'", "Sinatra::Base"));
        
        // PHP Frameworks
        frameworkIndicators.put("Laravel", List.of("Illuminate\\", "laravel", "artisan"));
        frameworkIndicators.put("Symfony", List.of("Symfony\\", "App\\Controller"));
        
        // Check files for framework indicators
        for (Map.Entry<String, List<String>> entry : frameworkIndicators.entrySet()) {
            String framework = entry.getKey();
            List<String> indicators = entry.getValue();
            
            for (Path file : files) {
                try {
                    String content = Files.readString(file);
                    for (String indicator : indicators) {
                        if (content.contains(indicator)) {
                            log.debug("Framework detected: {} (found '{}' in {})", 
                                    framework, indicator, file.getFileName());
                            return framework;
                        }
                    }
                } catch (IOException e) {
                    // Skip unreadable files
                }
            }
        }
        
        return null;
    }

    /**
     * Get all files recursively
     */
    private List<Path> getAllFiles(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .filter(this::isNotBinary)
                    .filter(this::isNotHidden)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Filter out binary files
     */
    private boolean isNotBinary(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return !name.endsWith(".exe") && 
               !name.endsWith(".dll") && 
               !name.endsWith(".so") && 
               !name.endsWith(".dylib") &&
               !name.endsWith(".jar") &&
               !name.endsWith(".war") &&
               !name.endsWith(".class") &&
               !name.endsWith(".pyc") &&
               !name.endsWith(".o") &&
               !name.endsWith(".obj") &&
               !name.endsWith(".bin") &&
               !name.endsWith(".zip") &&
               !name.endsWith(".tar") &&
               !name.endsWith(".gz") &&
               !name.endsWith(".png") &&
               !name.endsWith(".jpg") &&
               !name.endsWith(".jpeg") &&
               !name.endsWith(".gif") &&
               !name.endsWith(".ico") &&
               !name.endsWith(".pdf") &&
               !name.endsWith(".mp3") &&
               !name.endsWith(".mp4");
    }

    /**
     * Filter out hidden files and folders
     */
    private boolean isNotHidden(Path path) {
        return !path.getFileName().toString().startsWith(".");
    }

    /**
     * Count files matching language extensions
     */
    private int countLanguageFiles(List<Path> files, LanguageSignature lang) {
        return (int) files.stream()
                .filter(f -> {
                    String name = f.getFileName().toString().toLowerCase();
                    return lang.getExtensions().stream().anyMatch(name::endsWith);
                })
                .count();
    }
}