package com.metrohub.controllers;

import com.metrohub.dto.AcknowledgementDTOs.*;
import com.metrohub.dto.DocumentDTOs.*;
import com.metrohub.dto.NlpDTOs.*;
import com.metrohub.dto.PolicyDTOs.LegalHoldRequestDTO;
import com.metrohub.dto.PolicyDTOs.LegalHoldResponseDTO;
import com.metrohub.dto.SearchDTOs.*;
import com.metrohub.models.Alert;
import com.metrohub.models.Document;
import com.metrohub.models.Document.DocumentStatus;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.models.User;
import com.metrohub.repositories.DocumentAcknowledgementRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.services.AcknowledgementService;
import com.metrohub.services.DocumentNlpService;
import com.metrohub.services.DocumentReminderService;
import com.metrohub.services.DocumentService;
import com.metrohub.services.LegalHoldService;
import com.metrohub.services.NotificationService;
import com.metrohub.services.SearchService;
import com.metrohub.services.TextExtractionProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final AcknowledgementService acknowledgementService;
    private final LegalHoldService legalHoldService;
    private final DocumentNlpService documentNlpService;
    private final NotificationService notificationService;
    private final DocumentRepository documentRepository;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final TextExtractionProcessingService extractionProcessingService;
    private final SearchService searchService;
    private final DocumentReminderService reminderService;

    // ===== DOCUMENT UPLOAD =====

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<DocumentResponseDTO> uploadDocument(
            @Valid @ModelAttribute DocumentUploadDTO uploadDTO,
            HttpServletRequest httpRequest) {
        log.info("Upload request: {}", uploadDTO.getFile() != null ? uploadDTO.getFile().getOriginalFilename() : "no file");
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            DocumentResponseDTO response = documentService.uploadDocument(uploadDTO, ipAddress, userAgent);
            log.info("Document uploaded successfully with ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            throw e;
        }
    }

    // ===== DOCUMENT CRUD =====

    @GetMapping("/documents")
    public ResponseEntity<Page<DocumentResponseDTO>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(documentService.getAllDocuments(page, size));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/documents/department/{departmentId}")
    public ResponseEntity<Page<DocumentResponseDTO>> getDocumentsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(documentService.getDocumentsByDepartment(departmentId, page, size));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        DocumentResponseDTO document = documentService.getDocumentById(id);
        byte[] fileContent = documentService.getDocumentFile(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(document.getFileType() != null ? document.getFileType() : "application/octet-stream"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(document.getFileName()).build());
        headers.setContentLength(fileContent.length);
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    @GetMapping("/documents/{id}/download-extracted")
    public ResponseEntity<byte[]> downloadExtractedDocument(@PathVariable Long id) {
        DocumentResponseDTO document = documentService.getDocumentById(id);
        byte[] fileContent = documentService.getExtractedFile(id);
        String fileName = document.getFileName() != null
                ? document.getFileName().replaceAll("\\.[^.]+$", "") + "_extracted.txt"
                : "extracted_" + id + ".txt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(fileContent.length);
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<DocumentResponseDTO> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUploadDTO updateDTO) {
        return ResponseEntity.ok(documentService.updateDocument(id, updateDTO));
    }

    @PutMapping("/documents/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Void> archiveDocument(@PathVariable Long id) {
        documentService.archiveDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documents/{id}/reprocess-nlp")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> reprocessNlp(@PathVariable Long id) {
        try {
            NlpAnalysisResultDTO result = documentNlpService.analyzeDocument(id, true);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.getSuccess());
            response.put("documentId", id);
            response.put("documentType", result.getDocumentType());
            response.put("priority", result.getPriority());
            response.put("department", result.getDepartmentName());
            response.put("confidence", result.getClassificationConfidence());
            response.put("summary", result.getSummary());
            response.put("keywords", result.getMatchedKeywords());
            response.put("message", "NLP reprocessing completed. Metadata updated.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("NLP reprocess failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/documents/{id}/send-reminders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> sendReminders(@PathVariable Long id) {
        try {
            Document document = documentRepository.findById(id).orElse(null);
            if (document == null) {
                throw new RuntimeException("Document not found with ID: " + id);
            }
            List<User> unacknowledgedUsers = acknowledgementRepository.findUsersNotAcknowledged(id);

            if (unacknowledgedUsers.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "documentId", id, "remindersSent", 0,
                        "message", "All users have already acknowledged this document."));
            }

            int remindersSent = 0;
            for (User user : unacknowledgedUsers) {
                String message = String.format("REMINDER: Document '%s' requires your acknowledgement.", document.getFileName());
                try {
                    notificationService.sendNotification(user, document, Alert.AlertType.ACKNOWLEDGEMENT_REQUIRED, message);
                    remindersSent++;
                } catch (Exception e) {
                    log.warn("Failed to send reminder to {}: {}", user.getEmail(), e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", id);
            response.put("documentName", document.getFileName());
            response.put("totalPendingUsers", unacknowledgedUsers.size());
            response.put("remindersSent", remindersSent);
            List<String> emails = new ArrayList<>();
            for (User u : unacknowledgedUsers) {
                emails.add(u.getEmail());
            }
            response.put("usersNotified", emails);
            response.put("message", String.format("Reminders sent to %d users.", remindersSent));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Send reminders failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ===== SLA =====

    @PutMapping("/documents/{id}/sla")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> setManualSla(
            @PathVariable Long id,
            @Valid @RequestBody DocumentSlaConfigDTO slaConfig) {
        try {
            DocumentResponseDTO updated = documentService.setManualSla(id,
                    slaConfig.getReminderHours(), slaConfig.getDeptAdminEscalationHours(),
                    slaConfig.getSuperAdminEscalationHours(), slaConfig.getViolationHours(),
                    slaConfig.getEmailEnabled(), slaConfig.getSmsEnabled(), slaConfig.getDashboardEnabled());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Manual SLA configured successfully");
            response.put("documentId", id);
            response.put("slaConfig", slaConfig);
            response.put("document", updated);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to configure SLA");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/documents/{id}/sla")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSlaConfig(@PathVariable Long id) {
        try {
            Document document = documentRepository.findById(id).orElse(null);
            if (document == null) {
                throw new RuntimeException("Document not found with ID: " + id);
            }
            DocumentSlaConfigDTO slaConfig = documentService.getSlaConfig(id);
            Map<String, Object> slaTimings = documentService.calculateSlaTimings(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", id);
            response.put("isManualSla", document.getIsSlaManual());
            response.put("slaConfiguredAt", document.getSlaConfiguredAt());
            response.put("slaConfig", slaConfig);
            response.put("slaTimings", slaTimings);

            if (document.getUploadDate() != null) {
                java.time.LocalDateTime gracePeriodEnd = document.getUploadDate().plusMinutes(30);
                response.put("uploadDate", document.getUploadDate());
                response.put("gracePeriodEnds", gracePeriodEnd);
                response.put("withinGracePeriod", java.time.LocalDateTime.now().isBefore(gracePeriodEnd));
            }
            response.put("message", Boolean.TRUE.equals(document.getIsSlaManual()) ? "Using manual SLA configuration" : "Using automatic policy-based SLA");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{id}/sla")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearManualSla(@PathVariable Long id) {
        try {
            documentService.clearManualSla(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Manual SLA cleared. Document will use automatic policy-based SLA.", "documentId", id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ===== ACKNOWLEDGEMENTS =====

    @PostMapping("/documents/{id}/acknowledge")
    public ResponseEntity<AcknowledgementResponseDTO> acknowledgeDocument(
            @PathVariable Long id,
            @RequestBody(required = false) AcknowledgementRequestDTO request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String notes = request != null ? request.getNotes() : null;
        AcknowledgementResponseDTO response = acknowledgementService.acknowledgeDocument(id, notes, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{id}/acknowledgements")
    public ResponseEntity<List<AcknowledgementResponseDTO>> getDocumentAcknowledgements(@PathVariable Long id) {
        return ResponseEntity.ok(acknowledgementService.getAcknowledgementsForDocument(id, true));
    }

    @GetMapping("/documents/{id}/acknowledged")
    public ResponseEntity<Map<String, Boolean>> hasUserAcknowledged(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("acknowledged", acknowledgementService.hasUserAcknowledgedDocument(id)));
    }

    @GetMapping("/documents/pending-acknowledgement")
    public ResponseEntity<List<DocumentResponseDTO>> getDocumentsPendingAcknowledgement() {
        return ResponseEntity.ok(acknowledgementService.getDocumentsPendingAcknowledgement());
    }

    @GetMapping("/documents/pending-acknowledgement/count")
    public ResponseEntity<Map<String, Long>> getPendingAcknowledgementCount() {
        return ResponseEntity.ok(Map.of("count", acknowledgementService.getPendingAcknowledgementCount()));
    }

    @GetMapping("/documents/{id}/acknowledgement-rate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAcknowledgementRate(@PathVariable Long id) {
        return ResponseEntity.ok(acknowledgementService.getAcknowledgementRate(id));
    }

    @GetMapping("/documents/{id}/acknowledgements/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Long>> getAcknowledgementCount(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("count", acknowledgementService.countAcknowledgements(id)));
    }

    @GetMapping("/documents/{id}/not-acknowledged")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersNotAcknowledged(@PathVariable Long id) {
        List<Long> userIds = acknowledgementService.getUsersNotAcknowledged(id);
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", id);
        response.put("userIds", userIds);
        response.put("count", userIds.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/acknowledgements/my")
    public ResponseEntity<Page<AcknowledgementDTO>> getMyAcknowledgements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(acknowledgementService.getMyAcknowledgements(pageable));
    }

    @GetMapping("/acknowledgements/department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Page<AcknowledgementDTO>> getAcknowledgementsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(acknowledgementService.getAcknowledgementsByDepartment(departmentId, pageable));
    }

    // ===== LEGAL HOLD =====

    @PostMapping("/documents/{id}/legal-hold")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> applyLegalHold(
            @PathVariable Long id,
            @Valid @RequestBody LegalHoldRequestDTO request) {
        try {
            LegalHoldResponseDTO response = legalHoldService.applyLegalHold(id, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Legal hold applied successfully", "data", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot apply legal hold", "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to apply legal hold", "error", e.getMessage()));
        }
    }

    @PostMapping("/documents/{id}/legal-hold/remove")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> removeLegalHold(@PathVariable Long id, @RequestParam String reason) {
        try {
            LegalHoldResponseDTO response = legalHoldService.removeLegalHold(id, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "Legal hold removed successfully", "data", response));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot remove legal hold", "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to remove legal hold", "error", e.getMessage()));
        }
    }

    @GetMapping("/documents/{id}/legal-hold")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getLegalHoldStatus(@PathVariable Long id) {
        try {
            LegalHoldResponseDTO response = legalHoldService.getLegalHoldStatus(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Legal hold status retrieved", "data", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Failed to get legal hold status", "error", e.getMessage()));
        }
    }

    @GetMapping("/documents/legal-hold")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDocumentsUnderLegalHold(@RequestParam(required = false) Long departmentId) {
        try {
            List<LegalHoldResponseDTO> documents = departmentId != null
                    ? legalHoldService.getDocumentsUnderLegalHoldByDepartment(departmentId)
                    : legalHoldService.getAllDocumentsUnderLegalHold();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Documents under legal hold retrieved");
            result.put("data", documents);
            result.put("count", documents.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to retrieve documents", "error", e.getMessage()));
        }
    }

    // ===== TEXT EXTRACTION =====

    @GetMapping("/documents/{id}/extracted-text")
    public ResponseEntity<ExtractedTextResponseDTO> getExtractedText(@PathVariable Long id) {
        return ResponseEntity.ok(extractionProcessingService.getExtractedText(id));
    }

    @PostMapping("/documents/{id}/extract")
    public ResponseEntity<ExtractedTextResponseDTO> extractText(
            @PathVariable Long id,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(extractionProcessingService.extractAndClassify(id, language));
    }

    @GetMapping("/documents/{id}/extraction-status")
    public ResponseEntity<ExtractionStatusResponse> getExtractionStatus(@PathVariable Long id) {
        ExtractedTextResponseDTO fullResponse = extractionProcessingService.getExtractedText(id);
        return ResponseEntity.ok(new ExtractionStatusResponse(id, fullResponse.getIsTextExtracted(),
                fullResponse.getExtractionMethod(), fullResponse.getTextLength(), fullResponse.getStatus()));
    }

    public record ExtractionStatusResponse(Long documentId, Boolean isExtracted, String extractionMethod, Integer textLength, String status) {}

    // ===== NLP =====

    @PostMapping("/nlp/analyze/{documentId}")
    public ResponseEntity<NlpAnalysisResultDTO> analyzeDocument(
            @PathVariable Long documentId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            NlpAnalysisResultDTO result = documentNlpService.analyzeDocument(documentId, force);
            if (result.getSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NlpAnalysisResultDTO.builder()
                    .documentId(documentId).success(false).errorMessage(e.getMessage()).build());
        }
    }

    @PostMapping("/nlp/analyze-text")
    public ResponseEntity<NlpAnalysisResultDTO> analyzeText(@RequestBody NlpAnalysisRequestDTO request) {
        if (request.getText() == null || request.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(NlpAnalysisResultDTO.builder()
                    .success(false).errorMessage("Text content is required").build());
        }
        NlpAnalysisResultDTO result = documentNlpService.analyzeText(request.getText(), request.getFileName());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/nlp/result/{documentId}")
    public ResponseEntity<NlpAnalysisResultDTO> getAnalysisResult(@PathVariable Long documentId) {
        try {
            if (documentNlpService.isNlpProcessed(documentId)) {
                return ResponseEntity.ok(documentNlpService.analyzeDocument(documentId));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NlpAnalysisResultDTO.builder()
                        .documentId(documentId).success(false).errorMessage("Document has not been processed by NLP yet").build());
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NlpAnalysisResultDTO.builder()
                    .documentId(documentId).success(false).errorMessage(e.getMessage()).build());
        }
    }

    @PostMapping("/nlp/reanalyze/{documentId}")
    public ResponseEntity<NlpAnalysisResultDTO> reanalyzeDocument(@PathVariable Long documentId) {
        try {
            NlpAnalysisResultDTO result = documentNlpService.reanalyzeDocument(documentId);
            return result.getSuccess() ? ResponseEntity.ok(result) : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NlpAnalysisResultDTO.builder()
                    .documentId(documentId).success(false).errorMessage(e.getMessage()).build());
        }
    }

    @GetMapping("/nlp/status/{documentId}")
    public ResponseEntity<Map<String, Object>> checkNlpStatus(@PathVariable Long documentId) {
        Map<String, Object> status = new HashMap<>();
        status.put("documentId", documentId);
        try {
            boolean isProcessed = documentNlpService.isNlpProcessed(documentId);
            status.put("isNlpProcessed", isProcessed);
            status.put("status", isProcessed ? "COMPLETED" : "PENDING");
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            status.put("isNlpProcessed", false);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(status);
        }
    }

    @PostMapping("/nlp/process-async/{documentId}")
    public ResponseEntity<Map<String, Object>> triggerAsyncProcessing(@PathVariable Long documentId) {
        documentNlpService.processDocumentAsync(documentId);
        return ResponseEntity.accepted().body(Map.of("documentId", documentId, "message", "NLP processing started in background", "status", "PROCESSING"));
    }

    @GetMapping("/nlp/health")
    public ResponseEntity<Map<String, Object>> nlpHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "NLP Service");
        health.put("status", "UP");
        health.put("features", new String[]{"Department Classification", "Document Type Detection", "Priority Determination", "Summary Generation", "Deadline Detection"});
        return ResponseEntity.ok(health);
    }

    // ===== SEARCH =====

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, mapSortField(sortBy));
            SearchResultDTO results = searchService.search(keyword, PageRequest.of(page, size, sort));
            return ResponseEntity.ok(Map.of("success", true, "message", "Search completed successfully", "data", results));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Search failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<Map<String, Object>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineTo,
            @RequestParam(required = false) Boolean hasDeadline,
            @RequestParam(required = false) Boolean isOverdue,
            @RequestParam(required = false) Boolean highPriorityOnly,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            SearchFilterDTO filters = SearchFilterDTO.builder()
                    .keyword(keyword).departmentId(departmentId)
                    .documentType(parseDocumentType(documentType)).priority(parsePriority(priority))
                    .status(parseStatus(status)).dateFrom(dateFrom).dateTo(dateTo)
                    .deadlineFrom(deadlineFrom).deadlineTo(deadlineTo)
                    .hasDeadline(hasDeadline).isOverdue(isOverdue).highPriorityOnly(highPriorityOnly)
                    .sortBy(sortBy).sortDirection(sortDir).page(page).size(size).build();
            SearchResultDTO results = searchService.advancedSearch(filters);
            return ResponseEntity.ok(Map.of("success", true, "message", "Advanced search completed successfully",
                    "data", results, "filtersApplied", filters.getFilterDescription()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Advanced search failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/search/high-priority")
    public ResponseEntity<Map<String, Object>> searchHighPriority(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            SearchResultDTO results = searchService.searchHighPriority(PageRequest.of(page, size));
            return ResponseEntity.ok(Map.of("success", true, "message", "High priority documents retrieved", "data", results));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Search failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/search/overdue")
    public ResponseEntity<Map<String, Object>> searchOverdue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            SearchResultDTO results = searchService.searchOverdue(PageRequest.of(page, size));
            return ResponseEntity.ok(Map.of("success", true, "message", "Overdue documents retrieved", "data", results));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Search failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/search/department/{departmentId}")
    public ResponseEntity<Map<String, Object>> searchByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            SearchResultDTO results = searchService.searchByDepartment(departmentId, PageRequest.of(page, size));
            return ResponseEntity.ok(Map.of("success", true, "message", "Department documents retrieved", "data", results, "departmentId", departmentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Search failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/search/upcoming")
    public ResponseEntity<Map<String, Object>> searchUpcomingDeadlines(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            SearchResultDTO results = searchService.searchUpcomingDeadlines(days, PageRequest.of(page, size));
            return ResponseEntity.ok(Map.of("success", true, "message", "Upcoming deadline documents retrieved", "data", results, "daysAhead", days));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Search failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/search/options")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> documentTypes = new HashMap<>();
        for (DocumentType type : DocumentType.values()) documentTypes.put(type.name(), formatEnumName(type.name()));
        options.put("documentTypes", documentTypes);
        Map<String, String> priorities = new HashMap<>();
        for (Priority p : Priority.values()) priorities.put(p.name(), p.name());
        options.put("priorities", priorities);
        Map<String, String> statuses = new HashMap<>();
        for (DocumentStatus s : DocumentStatus.values()) statuses.put(s.name(), formatEnumName(s.name()));
        options.put("statuses", statuses);
        options.put("sortOptions", Map.of("uploadDate", "Upload Date", "priority", "Priority", "fileName", "File Name", "documentType", "Document Type"));
        return ResponseEntity.ok(Map.of("success", true, "options", options));
    }

    // ===== REMINDERS =====

    @PostMapping("/reminders")
    @PreAuthorize("hasAnyRole('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createReminder(@Valid @RequestBody DocumentReminderRequestDTO request) {
        try {
            DocumentReminderDTO created = reminderService.createReminder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Reminder created successfully", "data", created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid request", "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to create reminder", "error", e.getMessage()));
        }
    }

    @GetMapping("/reminders/document/{documentId}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getRemindersForDocument(@PathVariable Long documentId) {
        try {
            List<DocumentReminderDTO> reminders = reminderService.getRemindersForDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Reminders retrieved successfully", "data", reminders, "count", reminders.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to retrieve reminders", "error", e.getMessage()));
        }
    }

    @GetMapping("/reminders/document/{documentId}/active")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getActiveRemindersForDocument(@PathVariable Long documentId) {
        try {
            List<DocumentReminderDTO> reminders = reminderService.getActiveRemindersForDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Active reminders retrieved successfully", "data", reminders, "count", reminders.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to retrieve active reminders", "error", e.getMessage()));
        }
    }

    @GetMapping("/reminders/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getReminderById(@PathVariable Long id) {
        try {
            DocumentReminderDTO reminder = reminderService.getReminderById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Reminder retrieved successfully", "data", reminder));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Reminder not found", "error", e.getMessage()));
        }
    }

    @DeleteMapping("/reminders/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelReminder(@PathVariable Long id) {
        try {
            reminderService.cancelReminder(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Reminder cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to cancel reminder", "error", e.getMessage()));
        }
    }

    @DeleteMapping("/reminders/document/{documentId}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelAllRemindersForDocument(@PathVariable Long documentId) {
        try {
            reminderService.cancelAllRemindersForDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "All reminders cancelled for document"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to cancel reminders", "error", e.getMessage()));
        }
    }

    @PostMapping("/reminders/document/{documentId}/send-now")
    @PreAuthorize("hasAnyRole('DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> sendImmediateReminder(
            @PathVariable Long documentId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String customMessage = body != null ? body.get("message") : null;
            int sentCount = reminderService.sendImmediateReminder(documentId, customMessage);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", sentCount > 0 ? String.format("Reminders sent to %d users", sentCount) : "No pending users to remind",
                    "remindersSent", sentCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Failed to send reminder", "error", e.getMessage()));
        }
    }

    // ===== HELPERS =====

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) return xForwardedFor.split(",")[0].trim();
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;
        return request.getRemoteAddr();
    }

    private DocumentType parseDocumentType(String type) {
        if (type == null || type.isBlank()) return null;
        try { return DocumentType.valueOf(type.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private Priority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) return null;
        try { return Priority.valueOf(priority.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private DocumentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return DocumentStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "uploaddate", "upload_date" -> "uploadDate";
            case "priority" -> "priority";
            case "filename", "file_name" -> "fileName";
            case "documenttype", "document_type" -> "documentType";
            default -> "uploadDate";
        };
    }

    private String formatEnumName(String name) {
        if (name == null) return "";
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)));
            result.append(word.substring(1));
        }
        return result.toString();
    }
}
