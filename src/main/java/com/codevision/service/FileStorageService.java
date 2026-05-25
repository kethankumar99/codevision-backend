package com.codevision.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload-dir:./uploads/projects}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("✅ Upload directory created: {}", uploadPath);
        } catch (IOException e) {
            log.error("❌ Failed to create upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Save uploaded ZIP file
     */
    public Path saveZipFile(MultipartFile file, UUID projectId) {
        try {
            // Create project directory
            Path projectDir = uploadPath.resolve(projectId.toString());
            Files.createDirectories(projectDir);
            log.debug("Project directory created: {}", projectDir);

            // Save ZIP file
            String originalFilename = file.getOriginalFilename();
            Path zipPath = projectDir.resolve(originalFilename);
            Files.copy(file.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("✅ ZIP file saved: {} ({} bytes)", zipPath, file.getSize());
            return zipPath;

        } catch (IOException e) {
            log.error("❌ Failed to save ZIP file: {}", e.getMessage());
            throw new RuntimeException("Failed to save uploaded file", e);
        }
    }

    /**
     * Extract ZIP file to project directory
     */
    public Path extractZip(Path zipPath, UUID projectId) {
        Path extractDir = uploadPath.resolve(projectId.toString()).resolve("extracted");
        
        try {
            Files.createDirectories(extractDir);
            
            // Use java.util.zip to extract
            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
                var entries = zipFile.entries();
                
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    Path entryPath = extractDir.resolve(entry.getName()).normalize();
                    
                    // Security check - prevent zip slip attack
                    if (!entryPath.startsWith(extractDir)) {
                        log.warn("⚠️ Attempted Zip Slip attack: {}", entry.getName());
                        throw new RuntimeException("Invalid ZIP entry: " + entry.getName());
                    }
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        // Create parent directories
                        Files.createDirectories(entryPath.getParent());
                        
                        // Extract file
                        try (var inputStream = zipFile.getInputStream(entry)) {
                            Files.copy(inputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            
            log.info("✅ ZIP extracted successfully to: {}", extractDir);
            
            // Count files
            long fileCount = Files.walk(extractDir)
                    .filter(Files::isRegularFile)
                    .count();
            log.debug("Total files extracted: {}", fileCount);
            
            return extractDir;

        } catch (IOException e) {
            log.error("❌ Failed to extract ZIP: {}", e.getMessage());
            throw new RuntimeException("Failed to extract ZIP file", e);
        }
    }

    /**
     * Delete project files
     */
    public void deleteProjectFiles(UUID projectId) {
        Path projectDir = uploadPath.resolve(projectId.toString());
        try {
            if (Files.exists(projectDir)) {
                Files.walk(projectDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("🗑️ Project files deleted: {}", projectDir);
            }
        } catch (IOException e) {
            log.error("❌ Failed to delete project files: {}", e.getMessage());
        }
    }

    /**
     * Get upload directory path
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}