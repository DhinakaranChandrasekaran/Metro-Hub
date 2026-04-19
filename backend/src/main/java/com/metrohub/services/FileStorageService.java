package com.metrohub.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);

    String storeFile(MultipartFile file, String customFileName);

    byte[] loadFile(String filePath);

    boolean deleteFile(String filePath);

    boolean fileExists(String filePath);

    long getFileSize(String filePath);

    boolean isValidFileType(MultipartFile file);

    String generateUniqueFileName(String originalFileName);

    String storeExtractedText(String extractedText, String department, String baseName);
}
