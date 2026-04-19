package com.metrohub.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorageService.class);

    private final S3Client s3Client;

    @Value("${metrohub.s3.bucket-name}")
    private String bucketName;

    @Value("${metrohub.file-storage.allowed-types}")
    private String allowedTypes;

    // ============================================================
    // STORE ORIGINAL FILE
    // ============================================================

        public String storeFile(MultipartFile file) {
        return storeFile(file, generateUniqueFileName(file.getOriginalFilename()));
    }

        public String storeFile(MultipartFile file, String customFileName) {
        try {
            String key = buildS3Key("original", "general", customFileName);
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("✅ Uploaded original file to S3: {}", key);
            return key;
        } catch (IOException e) {
            log.error("❌ Failed to upload file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to store file in S3", e);
        }
    }

    

    public String storeFile(MultipartFile file, String customFileName, String department) {
        try {
            String key = buildS3Key("original", department, customFileName);
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("✅ Uploaded original file to S3: {}", key);
            return key;
        } catch (IOException e) {
            log.error("❌ Failed to upload file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to store file in S3", e);
        }
    }

    // ============================================================
    // STORE EXTRACTED TEXT FILE
    // ============================================================

    

    public String storeExtractedText(String extractedText, String department, String baseName) {
        String fileName = baseName + "_extracted.txt";
        String key = buildS3Key("extracted", department, fileName);

        byte[] textBytes = extractedText.getBytes(StandardCharsets.UTF_8);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/plain; charset=utf-8")
                .contentLength((long) textBytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(textBytes));
        log.info("✅ Uploaded extracted text to S3: {}", key);
        return key;
    }

    // ============================================================
    // LOAD FILE
    // ============================================================

        public byte[] loadFile(String filePath) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (Exception e) {
            log.error("❌ Failed to load file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to load file from S3", e);
        }
    }

    // ============================================================
    // DELETE FILE
    // ============================================================

        public boolean deleteFile(String filePath) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("🗑️ Deleted file from S3: {}", filePath);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to delete file from S3: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

        public boolean fileExists(String filePath) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

        public long getFileSize(String filePath) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            return s3Client.headObject(headRequest).contentLength();
        } catch (Exception e) {
            return -1;
        }
    }

        public boolean isValidFileType(MultipartFile file) {
        if (file == null || file.getContentType() == null)
            return false;
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        return allowed.contains(file.getContentType().trim());
    }

        public String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    

    public String getFileUrl(String s3Key) {
        // Return a direct download path through the backend API
        return "/documents/download?key=" + s3Key;
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private String buildS3Key(String prefix, String department, String fileName) {
        LocalDate now = LocalDate.now();
        String dept = sanitize(department);
        return String.format("%s/%s/%d/%02d/%s",
                prefix, dept, now.getYear(), now.getMonthValue(), fileName);
    }

    private String sanitize(String input) {
        if (input == null)
            return "general";
        return input.replaceAll("[^a-zA-Z0-9-_]", "_").toLowerCase();
    }
}
