package com.metrohub.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    @Value("${metrohub.file-storage.upload-dir}")
    private String uploadDir;

    @Value("#{'${metrohub.file-storage.allowed-types}'.split(',')}")
    private List<String> allowedTypes;

    @Value("${metrohub.file-storage.max-file-size-mb}")
    private int maxFileSizeMB;

    private Path rootLocation;

    

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("✅ File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            log.error("❌ Could not initialize storage directory: {}", uploadDir, e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    

    public String storeFile(MultipartFile file) {
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
        return storeFile(file, uniqueFileName);
    }

    

    public String storeFile(MultipartFile file, String customFileName) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }

            // Validate file type
            if (!isValidFileType(file)) {
                throw new RuntimeException("File type not allowed: " + file.getContentType());
            }

            // Validate file size
            if (file.getSize() > (long) maxFileSizeMB * 1024 * 1024) {
                throw new RuntimeException("File size exceeds " + maxFileSizeMB + "MB limit");
            }

            // Clean filename
            String fileName = StringUtils.cleanPath(customFileName);

            // Security check: prevent directory traversal
            if (fileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence: " + fileName);
            }

            // Create date-based directory structure
            LocalDate today = LocalDate.now();
            String dateFolder = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = rootLocation.resolve(dateFolder);
            Files.createDirectories(targetDir);

            // Resolve destination path
            Path destinationFile = targetDir.resolve(fileName);

            // Copy file to destination
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path from root
            String relativePath = dateFolder + "/" + fileName;
            log.info("✅ File stored successfully: {}", relativePath);

            return relativePath;

        } catch (IOException e) {
            log.error("❌ Failed to store file: {}", customFileName, e);
            throw new RuntimeException("Failed to store file: " + customFileName, e);
        }
    }

    

    public byte[] loadFile(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();

            if (!Files.exists(file)) {
                throw new RuntimeException("File not found: " + filePath);
            }

            return Files.readAllBytes(file);

        } catch (IOException e) {
            log.error("❌ Failed to load file: {}", filePath, e);
            throw new RuntimeException("Failed to load file: " + filePath, e);
        }
    }

    

    public boolean deleteFile(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("❌ Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    

    public boolean fileExists(String filePath) {
        Path file = rootLocation.resolve(filePath).normalize();
        return Files.exists(file);
    }

    

    public long getFileSize(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    

    public boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        return allowedTypes.contains(contentType);
    }

    

    public String generateUniqueFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String cleanName = StringUtils.cleanPath(originalFileName)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return uuid + "-" + cleanName;
    }

    

    public Path getFullPath(String relativePath) {
        return rootLocation.resolve(relativePath).normalize();
    }

    

    public Path getRootLocation() {
        return rootLocation;
    }

    @Override
    public String storeExtractedText(String extractedText, String department, String baseName) {
        try {
            String fileName = baseName + "_extracted.txt";
            LocalDate today = LocalDate.now();
            String dateFolder = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = rootLocation.resolve("extracted").resolve(dateFolder);
            Files.createDirectories(targetDir);
            Path destinationFile = targetDir.resolve(fileName);
            Files.writeString(destinationFile, extractedText);
            String relativePath = "extracted/" + dateFolder + "/" + fileName;
            log.info("Extracted text stored locally: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store extracted text locally: {}", e.getMessage());
            throw new RuntimeException("Failed to store extracted text", e);
        }
    }
}
