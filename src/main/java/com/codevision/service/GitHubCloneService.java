package com.codevision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class GitHubCloneService {

    private static final Logger log = LoggerFactory.getLogger(GitHubCloneService.class);

    @Value("${app.upload-dir:./uploads/projects}")
    private String uploadDir;

    private String gitPath = null;

    /**
     * Clone/download GitHub repository
     */
    public Path cloneRepo(String gitUrl, UUID projectId) {
        Path projectDir = Path.of(uploadDir).resolve(projectId.toString()).resolve("extracted");
        
        try {
            Files.createDirectories(projectDir);
            
            // Try Git first
            String gitCmd = findGit();
            
            if (gitCmd != null) {
                log.info("🔄 Cloning with Git: {}", gitUrl);
                return cloneWithGit(gitCmd, gitUrl, projectDir);
            } else {
                // Fallback: Download ZIP
                log.info("📦 Git not found, downloading ZIP instead");
                return downloadAsZip(gitUrl, projectDir, projectId);
            }
        } catch (Exception e) {
            log.error("❌ Clone failed: {}", e.getMessage());
            throw new RuntimeException("Failed to clone: " + e.getMessage());
        }
    }

    /**
     * Clone using Git command
     */
    private Path cloneWithGit(String gitCmd, String gitUrl, Path projectDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            gitCmd, "clone", "--depth", "1", gitUrl, projectDir.toString()
        );
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        boolean completed = process.waitFor(120, TimeUnit.SECONDS);
        
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Git clone timed out");
        }
        
        if (process.exitValue() == 0) {
            log.info("✅ Cloned successfully");
            return projectDir;
        } else {
            // Git failed, try ZIP fallback
            log.warn("Git clone failed, trying ZIP download");
            return downloadAsZip(gitUrl, projectDir, UUID.randomUUID());
        }
    }

    /**
     * Download repository as ZIP from GitHub
     */
    private Path downloadAsZip(String gitUrl, Path projectDir, UUID projectId) throws Exception {
        // Convert to ZIP URL
        String zipUrl = buildZipUrl(gitUrl);
        log.info("📥 Downloading: {}", zipUrl);
        
        Path zipPath = Path.of(uploadDir).resolve(projectId.toString()).resolve("repo.zip");
        Files.createDirectories(zipPath.getParent());
        
        // Download with progress
        try (InputStream in = new URL(zipUrl).openStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            long size = Files.size(zipPath);
            log.info("✅ Downloaded: {} bytes", size);
            
            if (size < 1000) {
                throw new RuntimeException("Downloaded file too small, repository may not exist");
            }
        } catch (Exception e) {
            throw new RuntimeException("Download failed. Check the URL or try a public repository.");
        }
        
        // Extract ZIP
        return extractZip(zipPath, projectDir);
    }

    /**
     * Build GitHub ZIP download URL
     */
    private String buildZipUrl(String gitUrl) {
        String url = gitUrl.replaceAll("/$", "").replace(".git", "");
        
        // https://github.com/user/repo → https://github.com/user/repo/archive/refs/heads/main.zip
        if (url.contains("github.com")) {
            return url + "/archive/refs/heads/main.zip";
        }
        
        // GitLab: https://gitlab.com/user/repo/-/archive/main/repo-main.zip
        if (url.contains("gitlab.com")) {
            return url + "/-/archive/main/" + extractRepoName(url) + "-main.zip";
        }
        
        return url + "/archive/refs/heads/main.zip";
    }

    /**
     * Extract ZIP file
     */
    private Path extractZip(Path zipPath, Path extractDir) throws Exception {
        log.info("📂 Extracting ZIP...");
        
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            var entries = zipFile.entries();
            int count = 0;
            
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Path entryPath = extractDir.resolve(entry.getName());
                
                // Handle the wrapper folder (repo-main/)
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        count++;
                    }
                }
            }
            log.info("✅ Extracted {} files", count);
        }
        
        return extractDir;
    }

    /**
     * Find Git executable
     */
    private String findGit() {
        if (gitPath != null && new File(gitPath).exists()) {
            return gitPath;
        }
        
        // Try system PATH
        try {
            new ProcessBuilder("git", "--version").start().waitFor(3, TimeUnit.SECONDS);
            gitPath = "git";
            return gitPath;
        } catch (Exception ignored) {}
        
        // Search common locations
        String[] paths = {
            "/usr/bin/git", "/usr/local/bin/git",
            "C:\\Program Files\\Git\\bin\\git.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Git\\cmd\\git.exe"
        };
        
        for (String path : paths) {
            if (new File(path).exists()) {
                gitPath = path;
                return gitPath;
            }
        }
        
        log.warn("Git not found, will use ZIP download");
        return null;
    }

    /**
     * Extract repo name from URL
     */
    private String extractRepoName(String url) {
        String[] parts = url.replace(".git", "").split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "repo";
    }
}