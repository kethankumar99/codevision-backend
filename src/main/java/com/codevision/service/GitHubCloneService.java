package com.codevision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GitHubCloneService {

    private static final Logger log = LoggerFactory.getLogger(GitHubCloneService.class);

    @Value("${app.upload-dir:./uploads/projects}")
    private String uploadDir;

    // Cache the git path after first detection
    private String gitPath = null;

    /**
     * Clone GitHub repository
     */
    public Path cloneRepo(String gitUrl, UUID projectId) {
        Path projectDir = Path.of(uploadDir).resolve(projectId.toString()).resolve("extracted");
        
        try {
            Files.createDirectories(projectDir);
            
            // Find git dynamically
            String gitCmd = findGit();
            
            if (gitCmd == null) {
                throw new RuntimeException(
                    "Git is not installed. Please install Git from https://git-scm.com/downloads"
                );
            }
            
            log.info("🔄 Using git at: {}", gitCmd);
            
            // Clone using git
           ProcessBuilder pb = new ProcessBuilder(
    gitCmd, "clone", "--depth", "1", "--config", "core.longpaths=true", gitUrl, projectDir.toString()
);
            pb.redirectErrorStream(true);
            
            log.info("🔄 Cloning: {}", gitUrl);
            Process process = pb.start();
            
            // Read output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("[GIT] {}", line);
                }
            }
            
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Git clone timed out after 60 seconds");
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode == 0) {
                log.info("✅ Cloned successfully to: {}", projectDir);
                return projectDir;
            } else {
                String error = output.toString();
                log.error("Git clone failed: {}", error);
                
                if (error.contains("Repository not found") || error.contains("not found")) {
                    throw new RuntimeException("Repository not found. Check the URL.");
                } else if (error.contains("Permission denied") || error.contains("Authentication failed")) {
                    throw new RuntimeException("Access denied. Repository may be private or requires authentication.");
                } else if (error.contains("already exists")) {
                    throw new RuntimeException("Project directory already exists.");
                } else {
                    throw new RuntimeException("Git clone failed: " + error.lines().limit(3).collect(java.util.stream.Collectors.joining(" ")));
                }
            }
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Clone failed: {}", e.getMessage());
            throw new RuntimeException("Clone failed: " + e.getMessage());
        }
    }

    /**
     * Find git executable dynamically
     */
    private String findGit() {
        // Return cached path if already found
        if (gitPath != null && new File(gitPath).exists()) {
            return gitPath;
        }
        
        // Try 1: Check system PATH
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "--version");
            Process process = pb.start();
            boolean ok = process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0;
            if (ok) {
                gitPath = "git";
                log.info("✅ Found git in system PATH");
                return gitPath;
            }
        } catch (Exception ignored) {}
        
        // Try 2: Common Windows locations
        List<String> possiblePaths = new ArrayList<>();
        
        // User's AppData
        String userHome = System.getProperty("user.home");
        possiblePaths.add(userHome + "\\AppData\\Local\\Programs\\Git\\cmd\\git.exe");
        possiblePaths.add(userHome + "\\AppData\\Local\\Programs\\Git\\bin\\git.exe");
        
        // Program Files
        possiblePaths.add("C:\\Program Files\\Git\\bin\\git.exe");
        possiblePaths.add("C:\\Program Files\\Git\\cmd\\git.exe");
        possiblePaths.add("C:\\Program Files (x86)\\Git\\bin\\git.exe");
        possiblePaths.add("C:\\Program Files (x86)\\Git\\cmd\\git.exe");
        
        // Other drives
        possiblePaths.add("D:\\Git\\bin\\git.exe");
        possiblePaths.add("D:\\Program Files\\Git\\bin\\git.exe");
        
        // Check each path
        for (String path : possiblePaths) {
            File gitFile = new File(path);
            if (gitFile.exists()) {
                log.info("✅ Found git at: {}", path);
                gitPath = path;
                return gitPath;
            }
        }
        
        // Try 3: Search using where command (Windows)
        try {
            ProcessBuilder pb = new ProcessBuilder("where", "git");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    log.info("✅ Found git via where: {}", line.trim());
                    gitPath = line.trim();
                    return gitPath;
                }
            }
        } catch (Exception ignored) {}
        
        log.error("❌ Git not found in any location");
        return null;
    }
}