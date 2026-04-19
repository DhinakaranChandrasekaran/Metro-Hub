package com.metrohub.services;

import com.metrohub.dto.DocumentDTOs.ExtractedTextResponseDTO;
import com.metrohub.models.Document;
import com.metrohub.models.Document.Priority;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.services.DocumentClassificationService.ClassificationResult;
import com.metrohub.services.TextExtractionService.ExtractionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TextExtractionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionProcessingService.class);

    private final DocumentRepository documentRepository;
    private final DepartmentRepository departmentRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;
    private final DocumentClassificationService classificationService;
    private final DocumentNlpService documentNlpService;  // Phase 4 NLP Service

    @Value("${metrohub.extraction.ocr.language:eng}")
    private String defaultOcrLanguage;

    

    @Async("textExtractionExecutor")
    public void processDocument(Long documentId) {
        
        log.info("🚀 Starting async document processing for ID: {}", documentId);
        
        try {
            // Delay to ensure transaction is fully committed
            Thread.sleep(1000);
            ExtractedTextResponseDTO result = extractAndClassify(documentId, null);
            log.info("✅ Phase 3 text extraction completed for ID: {}", documentId);
            
            // ============================================
            // PHASE 4: TRIGGER NLP PROCESSING
            // ============================================
            // After successful text extraction, trigger NLP analysis
            if ("SUCCESS".equals(result.getStatus()) && result.getIsTextExtracted()) {
                log.info("📊 Triggering Phase 4 NLP analysis for document ID: {}", documentId);
                documentNlpService.processDocumentAsync(documentId);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Document processing interrupted for ID: {}", documentId);
        } catch (Exception e) {
            log.error("❌ Document processing failed for ID: {}: {}", documentId, e.getMessage());
        }
    }

    

    @Transactional
    public ExtractedTextResponseDTO extractAndClassify(Long documentId, String language) {
        
        log.info("📝 Starting extraction for document ID: {}", documentId);

        // ============================================
        // STEP 1: LOAD DOCUMENT
        // ============================================
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found with ID: " + documentId);
        }
        Document document = docOpt.get();

        // ============================================
        // STEP 2: READ FILE FROM STORAGE
        // ============================================
        byte[] fileBytes;
        try {
            fileBytes = fileStorageService.loadFile(document.getFilePath());
            log.info("📂 File loaded: {} bytes", fileBytes.length);
        } catch (Exception e) {
            log.error("❌ Failed to load file: {}", e.getMessage());
            return buildErrorResponse(document, "Failed to load file: " + e.getMessage());
        }

        // ============================================
        // STEP 3: EXTRACT TEXT
        // ============================================
        ExtractionResult extractionResult = textExtractionService.extractText(
                new ByteArrayInputStream(fileBytes),
                document.getFileName()
        );

        if (!extractionResult.success()) {
            log.warn("⚠️ Text extraction failed: {}", extractionResult.errorMessage());
            // Still save the attempt
            document.setIsTextExtracted(false);
            document.setExtractionMethod(extractionResult.method());
            documentRepository.save(document);
            
            return buildErrorResponse(document, extractionResult.errorMessage());
        }

        String extractedText = extractionResult.extractedText();
        String extractionMethod = extractionResult.method();
        
        log.info("✅ Text extracted: {} characters via {}", 
                extractedText != null ? extractedText.length() : 0, extractionMethod);

        // ============================================
        // STEP 4: CLASSIFY DOCUMENT
        // ============================================
        ClassificationResult classification = classificationService.classifyDocument(
                extractedText,
                document.getFileName()
        );

        log.info("🏷️ Classification result - Type: {}, Dept: {}, Priority: {}", 
                classification.documentType(),
                classification.suggestedDepartment(),
                classification.priority()
        );

        // ============================================
        // STEP 5: UPDATE DOCUMENT IN DATABASE
        // ============================================
        document.setExtractedText(extractedText);
        document.setExtractionMethod(extractionMethod);
        document.setIsTextExtracted(true);
        document.setOcrLanguage(language != null ? language : 
                ("OCR".equals(extractionMethod) ? defaultOcrLanguage : null));

        // Apply classification if not manually set
        if (!Boolean.TRUE.equals(document.getIsManuallyClassified())) {
            if (document.getDocumentType() == null) {
                document.setDocumentType(classification.documentType());
            }
            if (document.getPriority() == null || document.getPriority() == Priority.MEDIUM) {
                document.setPriority(classification.priority());
            }
            document.setClassificationConfidence(classification.confidence());
            
            // Set department if suggested and not already set
            if (classification.suggestedDepartmentId() != null && document.getDepartment() == null) {
                departmentRepository.findById(classification.suggestedDepartmentId())
                        .ifPresent(document::setDepartment);
            }
        }

        // Store extracted text to S3
        try {
            String deptName = document.getDepartment() != null ? document.getDepartment().getName() : "general";
            String baseName = document.getStoredFileName() != null 
                    ? document.getStoredFileName().replaceAll("\\.[^.]+$", "")
                    : String.valueOf(document.getId());
            String extractedS3Key = fileStorageService.storeExtractedText(extractedText, deptName, baseName);
            document.setExtractedFilePath(extractedS3Key);
            log.info("Extracted text stored in S3: {}", extractedS3Key);
        } catch (Exception e) {
            log.warn("Failed to store extracted text in S3: {}", e.getMessage());
        }

        Document savedDocument = documentRepository.save(document);
        log.info("Document updated with extracted text and classification");

        // Build response
        return buildSuccessResponse(savedDocument, classification);
    }

    

    @Transactional(readOnly = true)
    public ExtractedTextResponseDTO getExtractedText(Long documentId) {
        
        log.info("📖 Getting extracted text for document ID: {}", documentId);

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found with ID: " + documentId);
        }
        Document document = docOpt.get();

        // If text hasn't been extracted yet, trigger extraction
        if (!Boolean.TRUE.equals(document.getIsTextExtracted()) && document.getExtractedText() == null) {
            log.info("📝 Text not yet extracted, triggering extraction...");
            return extractAndClassify(documentId, null);
        }

        // Build response from existing data
        return buildResponseFromDocument(document);
    }

    

    @Transactional(readOnly = true)
    public boolean isDocumentProcessed(Long documentId) {
        return documentRepository.findById(documentId)
                .map(this::isTextExtracted)
                .orElse(false);
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    

    private ExtractedTextResponseDTO buildSuccessResponse(
            Document document, 
            ClassificationResult classification) {
        
        String text = document.getExtractedText();
        
        return ExtractedTextResponseDTO.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .extractedText(text)
                .extractionMethod(document.getExtractionMethod())
                .isTextExtracted(true)
                .ocrLanguage(document.getOcrLanguage())
                .textLength(text != null ? text.length() : 0)
                .wordCount(text != null ? countWords(text) : 0)
                .suggestedDocumentType(classification.documentType())
                .suggestedDepartment(classification.suggestedDepartment())
                .suggestedPriority(classification.priority())
                .classificationConfidence(classification.confidence())
                .detectedKeywords(classification.detectedKeywords())
                .extractionDate(LocalDateTime.now())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .status("SUCCESS")
                .errorMessage(null)
                .build();
    }

    

    private ExtractedTextResponseDTO buildErrorResponse(Document document, String errorMessage) {
        return ExtractedTextResponseDTO.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .extractedText(null)
                .extractionMethod(document.getExtractionMethod())
                .isTextExtracted(false)
                .textLength(0)
                .wordCount(0)
                .extractionDate(LocalDateTime.now())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }

    

    private ExtractedTextResponseDTO buildResponseFromDocument(Document document) {
        String text = document.getExtractedText();
        
        // Re-classify to get keywords (not stored in DB)
        ClassificationResult classification = null;
        if (text != null) {
            classification = classificationService.classifyDocument(text, document.getFileName());
        }
        
        return ExtractedTextResponseDTO.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .extractedText(text)
                .extractionMethod(document.getExtractionMethod())
                .isTextExtracted(document.getIsTextExtracted())
                .ocrLanguage(document.getOcrLanguage())
                .textLength(text != null ? text.length() : 0)
                .wordCount(text != null ? countWords(text) : 0)
                .suggestedDocumentType(document.getDocumentType())
                .suggestedDepartment(document.getDepartment() != null ? 
                        document.getDepartment().getName() : null)
                .suggestedPriority(document.getPriority())
                .classificationConfidence(document.getClassificationConfidence())
                .detectedKeywords(classification != null ? classification.detectedKeywords() : new String[]{})
                .extractionDate(document.getUpdatedAt())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .status(Boolean.TRUE.equals(document.getIsTextExtracted()) ? "SUCCESS" : "NOT_PROCESSED")
                .errorMessage(null)
                .build();
    }

    

    private int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private boolean isTextExtracted(Document doc) {
        return Boolean.TRUE.equals(doc.getIsTextExtracted());
    }
}
