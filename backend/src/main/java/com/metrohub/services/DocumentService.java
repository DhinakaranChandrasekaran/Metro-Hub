package com.metrohub.services;

import com.metrohub.dto.DocumentDTOs.DocumentResponseDTO;
import com.metrohub.dto.DocumentDTOs.DocumentSlaConfigDTO;
import com.metrohub.dto.DocumentDTOs.DocumentUploadDTO;
import com.metrohub.models.*;
import com.metrohub.models.Document.*;
import com.metrohub.repositories.*;
import com.metrohub.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DepartmentRepository departmentRepository;
    private final MetadataRepository metadataRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileStorageService fileStorageService;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private TextExtractionProcessingService textExtractionProcessingService;
    private LegalHoldService legalHoldService;
    private NotificationService notificationService;

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
            DepartmentRepository departmentRepository,
            MetadataRepository metadataRepository,
            AuditLogRepository auditLogRepository,
            @Qualifier("s3FileStorageService") FileStorageService fileStorageService,
            DocumentAcknowledgementRepository acknowledgementRepository) {
        this.documentRepository = documentRepository;
        this.departmentRepository = departmentRepository;
        this.metadataRepository = metadataRepository;
        this.auditLogRepository = auditLogRepository;
        this.fileStorageService = fileStorageService;
        this.acknowledgementRepository = acknowledgementRepository;
    }

    @Autowired
    public void setTextExtractionProcessingService(
            @Lazy TextExtractionProcessingService textExtractionProcessingService) {
        this.textExtractionProcessingService = textExtractionProcessingService;
    }

    @Autowired
    public void setLegalHoldService(@Lazy LegalHoldService legalHoldService) {
        this.legalHoldService = legalHoldService;
    }

    @Autowired
    public void setNotificationService(@Lazy NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Upload document (no audit info)
    public DocumentResponseDTO uploadDocument(DocumentUploadDTO uploadDTO) {
        return uploadDocument(uploadDTO, null, null);
    }

    // Upload document with audit info
    public DocumentResponseDTO uploadDocument(DocumentUploadDTO uploadDTO, String ipAddress, String userAgent) {
        log.info("Starting document upload: {}", uploadDTO.getFile().getOriginalFilename());

        // Validate input
        MultipartFile file = uploadDTO.getFile();
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }
        if (uploadDTO.getDepartmentId() == null) {
            throw new RuntimeException("Department is required");
        }

        // Get department
        Department department = departmentRepository.findById(uploadDTO.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + uploadDTO.getDepartmentId()));

        // Get authenticated user
        User uploadUser = SecurityUtils.getCurrentUser();
        if (uploadUser == null) {
            throw new RuntimeException("Authentication required. Please login to upload documents.");
        }
        if (!uploadUser.canUpload()) {
            throw new RuntimeException("You don't have permission to upload documents.");
        }

        // Generate unique filename and store file
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String storedFileName = fileStorageService.generateUniqueFileName(originalFilename);
        String filePath = fileStorageService.storeFile(file, storedFileName);
        log.info("File stored: {} -> {}", originalFilename, filePath);

        // ===== SLA HANDLING AT UPLOAD =====
        // If SLA values provided at upload, set them (Manual SLA at upload)
        // Otherwise, leave NULL - Auto-SLA will be applied at T+30 by scheduler
        boolean isManualSla = uploadDTO.getSlaAckHours() != null && uploadDTO.getSlaAckHours() > 0;
        Integer ackHours = isManualSla ? uploadDTO.getSlaAckHours() : null;
        Integer esc1Hours = isManualSla && uploadDTO.getSlaEsc1() != null ? uploadDTO.getSlaEsc1() : null;
        Integer esc2Hours = isManualSla && uploadDTO.getSlaEsc2() != null ? uploadDTO.getSlaEsc2() : null;
        Integer esc3Hours = isManualSla && uploadDTO.getSlaEsc3() != null ? uploadDTO.getSlaEsc3() : null;

        // ===== MANUAL CLASSIFICATION CHECK =====
        // Mark as manually classified if user provided document type OR priority at upload
        // This prevents NLP from overwriting user-provided metadata
        boolean isManuallyClassified = uploadDTO.getDocumentType() != null || uploadDTO.getPriority() != null;

        // Create document entity
        Document document = Document.builder()
                .fileName(originalFilename)
                .storedFileName(storedFileName)
                .filePath(filePath)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileExtension(fileExtension)
                .documentType(uploadDTO.getDocumentType())
                .priority(uploadDTO.getPriority() != null ? uploadDTO.getPriority() : Priority.MEDIUM)
                .department(department)
                .uploadedBy(uploadUser)
                .status(DocumentStatus.ACTIVE)
                .isArchived(false)
                .description(uploadDTO.getDescription())
                .tags(uploadDTO.getTags())
                .isManuallyClassified(isManuallyClassified)
                .isTextExtracted(false)
                .slaReminderHours(ackHours)
                .slaDeptAdminEscalationHours(esc1Hours)
                .slaSuperAdminEscalationHours(esc2Hours)
                .slaViolationHours(esc3Hours)
                .isSlaManual(isManualSla)
                .slaConfiguredAt(isManualSla ? LocalDateTime.now() : null) // NULL = auto-SLA will apply at T+30
                .slaEmailEnabled(isManualSla ? true : null)
                .slaSmsEnabled(isManualSla ? true : null)
                .slaDashboardEnabled(isManualSla ? true : null)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Document saved with ID: {}", savedDocument.getId());

        // Create metadata record
        Metadata metadata = Metadata.builder()
                .document(savedDocument)
                .equipmentId(uploadDTO.getEquipmentId())
                .vendorName(uploadDTO.getVendorName())
                .referenceNumber(uploadDTO.getReferenceNumber())
                .build();
        metadataRepository.save(metadata);

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .user(uploadUser)
                .userEmail(uploadUser.getEmail())
                .action(AuditLog.AuditAction.DOCUMENT_UPLOADED)
                .entityType("Document")
                .entityId(savedDocument.getId())
                .entityName(originalFilename)
                .status(AuditLog.AuditStatus.SUCCESS)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details("{\"fileSize\": " + file.getSize() +
                        ", \"department\": \"" + department.getName() + "\"" +
                        ", \"fileType\": \"" + file.getContentType() + "\"}")
                .build();
        auditLogRepository.save(auditLog);

        // Trigger text extraction after commit
        final Long docId = savedDocument.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                try {
                    textExtractionProcessingService.processDocument(docId);
                } catch (Exception e) {
                    log.warn("Text extraction could not be triggered: {}", e.getMessage());
                }
            }
        });

        // Send email notifications
        try {
            notificationService.notifyDepartmentOnUpload(savedDocument);
        } catch (Exception e) {
            log.warn("Email notifications could not be sent: {}", e.getMessage());
        }

        log.info("Document upload completed: {}", savedDocument.getId());
        return toDTO(savedDocument);
    }

    // Get document by ID
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        return toDTO(document);
    }

    // Get all documents (paginated) — Maintenance dept users see all, others see only their dept
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getAllDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").descending());
        User currentUser = SecurityUtils.getCurrentUser();
        // SUPER_ADMIN sees all documents across all departments
        if (currentUser != null && currentUser.getRole() == User.UserRole.SUPER_ADMIN) {
            return documentRepository.findAll(pageable).map(this::toDTO);
        }
        // MAINTENANCE department (ID=1) users see all documents
        Long deptId = currentUser != null ? currentUser.getDepartmentId() : null;
        if (deptId != null && deptId == 1L) {
            log.info("Maintenance dept user - showing all documents");
            return documentRepository.findAll(pageable).map(this::toDTO);
        }
        // All other departments see only their own department's documents
        if (deptId != null) {
            return documentRepository.findByDepartmentId(deptId, pageable).map(this::toDTO);
        }
        return documentRepository.findAll(pageable).map(this::toDTO);
    }

    // Get documents by department
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getDocumentsByDepartment(Long departmentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").descending());
        return documentRepository.findByDepartmentId(departmentId, pageable).map(this::toDTO);
    }

    // Hard delete document — removes from storage, DB and all related records
    @Transactional
    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        if (legalHoldService != null) {
            legalHoldService.validateActionAllowed(document, "delete");
        }

        // Delete files from storage (S3 or local)
        try {
            if (document.getFilePath() != null) {
                fileStorageService.deleteFile(document.getFilePath());
                log.info("Deleted original file from storage: {}", document.getFilePath());
            }
        } catch (Exception e) {
            log.warn("Failed to delete original file from storage: {}", e.getMessage());
        }
        try {
            if (document.getExtractedFilePath() != null) {
                fileStorageService.deleteFile(document.getExtractedFilePath());
                log.info("Deleted extracted file from storage: {}", document.getExtractedFilePath());
            }
        } catch (Exception e) {
            log.warn("Failed to delete extracted file from storage: {}", e.getMessage());
        }

        // Delete ALL related records to avoid FK constraint errors
        try { acknowledgementRepository.deleteByDocument_Id(id); } catch (Exception e) { log.warn("Cleanup acknowledgements: {}", e.getMessage()); }
        try { metadataRepository.deleteByDocument_Id(id); } catch (Exception e) { log.warn("Cleanup metadata: {}", e.getMessage()); }
        try { documentRepository.deleteRemindersByDocumentId(id); } catch (Exception e) { log.warn("Cleanup reminders: {}", e.getMessage()); }
        try { documentRepository.deleteViolationsByDocumentId(id); } catch (Exception e) { log.warn("Cleanup violations: {}", e.getMessage()); }
        try { documentRepository.deleteAlertsByDocumentId(id); } catch (Exception e) { log.warn("Cleanup alerts: {}", e.getMessage()); }
        try { documentRepository.deleteAuditLogsByDocumentId(id); } catch (Exception e) { log.warn("Cleanup audit logs: {}", e.getMessage()); }

        // Delete the document itself
        documentRepository.delete(document);
        log.info("Document hard-deleted from DB: {}", id);
    }

    // Archive document
    public void archiveDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        document.setStatus(DocumentStatus.ARCHIVED);
        document.setIsArchived(true);
        documentRepository.save(document);
        log.info("Document archived: {}", id);
    }

    // Get document file content
    @Transactional(readOnly = true)
    public byte[] getDocumentFile(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        return fileStorageService.loadFile(document.getFilePath());
    }

    // Get extracted text file content (S3 first, fallback to DB column)
    @Transactional(readOnly = true)
    public byte[] getExtractedFile(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        // Try S3 file first
        if (document.getExtractedFilePath() != null) {
            try {
                return fileStorageService.loadFile(document.getExtractedFilePath());
            } catch (Exception e) {
                log.warn("Failed to load extracted file from S3, falling back to DB: {}", e.getMessage());
            }
        }
        // Fallback: fetch extractedText directly via query to avoid @Lob lazy-loading
        String text = documentRepository.findExtractedTextById(id);
        if (text != null) {
            return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        throw new RuntimeException("No extracted text available for document ID: " + id);
    }

    // Update document metadata
    public DocumentResponseDTO updateDocument(Long id, DocumentUploadDTO updateDTO) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
        if (legalHoldService != null) {
            legalHoldService.validateActionAllowed(document, "modify");
        }
        if (updateDTO.getDocumentType() != null) document.setDocumentType(updateDTO.getDocumentType());
        if (updateDTO.getPriority() != null) document.setPriority(updateDTO.getPriority());
        if (updateDTO.getDescription() != null) document.setDescription(updateDTO.getDescription());
        if (updateDTO.getTags() != null) document.setTags(updateDTO.getTags());
        if (updateDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(updateDTO.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            document.setDepartment(department);
        }
        Document saved = documentRepository.save(document);
        log.info("Document updated: {}", id);
        return toDTO(saved);
    }

    // Convert entity to DTO
    public DocumentResponseDTO toDTO(Document document) {
        String textPreview = null;
        try {
            if (document.getExtractedText() != null) {
                textPreview = document.getExtractedText().substring(0, Math.min(500, document.getExtractedText().length()));
            }
        } catch (Exception e) {
            log.debug("Could not load extracted text preview for doc {}", document.getId());
        }
        return DocumentResponseDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .fileExtension(document.getFileExtension())
                .documentType(document.getDocumentType())
                .documentTypeName(document.getDocumentType() != null ? formatEnumName(document.getDocumentType().name()) : "Unclassified")
                .priority(document.getPriority())
                .departmentName(document.getDepartment() != null ? document.getDepartment().getName() : "Unknown")
                .departmentId(document.getDepartment() != null ? document.getDepartment().getId() : null)
                .classificationConfidence(document.getClassificationConfidence())
                .extractedTextPreview(textPreview)
                .isTextExtracted(document.getIsTextExtracted())
                .extractionMethod(document.getExtractionMethod())
                .uploadedByName(document.getUploadedBy() != null ? document.getUploadedBy().getName() : "Unknown")
                .uploadedByEmail(document.getUploadedBy() != null ? document.getUploadedBy().getEmail() : null)
                .uploadDate(document.getUploadDate())
                .status(document.getStatus())
                .description(document.getDescription())
                .tags(document.getTags())
                .downloadUrl("/api/documents/" + document.getId() + "/download")
                .previewUrl("/api/documents/" + document.getId() + "/preview")
                .slaReminderHours(document.getSlaReminderHours())
                .slaDeptAdminEscalationHours(document.getSlaDeptAdminEscalationHours())
                .slaSuperAdminEscalationHours(document.getSlaSuperAdminEscalationHours())
                .slaViolationHours(document.getSlaViolationHours())
                .build();
    }

    // ===== MANUAL SLA CONFIGURATION =====

    // Set manual SLA for document
    @Transactional
    public DocumentResponseDTO setManualSla(Long documentId, Integer reminderHours,
            Integer deptAdminEscalationHours, Integer superAdminEscalationHours,
            Integer violationHours, Boolean emailEnabled, Boolean smsEnabled,
            Boolean dashboardEnabled) {

        log.info("Setting manual SLA for document ID: {}", documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime gracePeriodEnd = document.getUploadDate().plusMinutes(30);

        // Get current user
        User currentUser = SecurityUtils.getCurrentUser();
        String userEmail = currentUser != null ? currentUser.getEmail() : "unknown";

        log.info("Manual SLA request from user: {} at {}", userEmail, now);

        // ===== CHECK IF SLA ALREADY EXISTS FIRST =====
        // If manual SLA was already set at upload time, reject immediately with "Already Exists" error
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaConfiguredAt() != null) {
            LocalDateTime slaSetTime = document.getSlaConfiguredAt();
            LocalDateTime updateWindowEnd = slaSetTime.plusMinutes(30);

            if (now.isAfter(updateWindowEnd)) {
                log.warn("❌ SLA UPDATE WINDOW EXPIRED: Attempted update more than 30 mins after SLA was set");
                throw new RuntimeException("SLA_UPDATE_WINDOW_EXPIRED: Cannot update SLA. Update window (30 mins from SLA set time) has expired. " +
                        "SLA is now STATIC and cannot be modified by anyone.");
            }

            // Within update window - this is an update
            log.info("✅ Updating existing manual SLA (within 30-min update window)");
            // Allow update to proceed below
        } else if (Boolean.TRUE.equals(document.getIsSlaManual())) {
            // Manual SLA exists but no slaConfiguredAt - this shouldn't happen but handle it
            throw new RuntimeException("SLA_ALREADY_EXISTS: Cannot add SLA. This document already has SLA timings configured.");
        }

        // ===== NOW CHECK GRACE PERIOD =====
        // Check if grace period has expired (only matters if SLA doesn't already exist)
        // Add 10-second buffer for timezone/timing issues (network delay, server clock diff, etc)
        LocalDateTime gracePeriodEndWithBuffer = gracePeriodEnd.plusSeconds(10);

        if (Boolean.FALSE.equals(document.getIsSlaManual()) && now.isAfter(gracePeriodEndWithBuffer)) {
            log.warn("❌ GRACE PERIOD EXPIRED: User {} attempted to add SLA at {} (grace ended at {})",
                    userEmail, now, gracePeriodEnd);

            // Check if auto-SLA has already been applied
            boolean autoSlaAlreadyApplied = document.getSlaConfiguredAt() != null
                    && document.getSlaConfiguredAt().isAfter(gracePeriodEnd.minusSeconds(1));

            if (autoSlaAlreadyApplied) {
                throw new RuntimeException("SLA_AUTO_ALREADY_APPLIED: Cannot add manual SLA. Grace period (30 mins) has expired. " +
                        "Auto-SLA has been automatically applied to this document. SLA is now STATIC and cannot be changed.");
            }

            // Block EVERYONE - no Super Admin bypass allowed
            throw new RuntimeException("SLA_GRACE_PERIOD_EXPIRED: Cannot add SLA. The 30-minute grace period has expired. " +
                    "Auto-SLA should have been applied automatically at T+30. SLA is now STATIC.");
        }

        // ===== WITHIN GRACE PERIOD OR WITHIN UPDATE WINDOW - Allow adding/updating SLA =====
        log.info("✅ SLA can be added/updated. Time remaining: {} seconds",
                java.time.temporal.ChronoUnit.SECONDS.between(now, gracePeriodEnd));

        // ===== SET MANUAL SLA VALUES =====
        document.setSlaReminderHours(reminderHours);
        document.setSlaDeptAdminEscalationHours(deptAdminEscalationHours);
        document.setSlaSuperAdminEscalationHours(superAdminEscalationHours);
        document.setSlaViolationHours(violationHours);
        document.setSlaEmailEnabled(emailEnabled != null ? emailEnabled : true);
        document.setSlaSmsEnabled(smsEnabled != null ? smsEnabled : true);
        document.setSlaDashboardEnabled(dashboardEnabled != null ? dashboardEnabled : true);

        // ALWAYS set slaConfiguredAt when SLA is configured (for first time or update)
        // This ensures the 30-min update window timer starts immediately
        document.setSlaConfiguredAt(now);

        document.setIsSlaManual(true);

        Document saved = documentRepository.save(document);
        log.info("✅ Manual SLA configured for document ID: {} by user: {}", documentId, userEmail);
        return toDTO(saved);
    }

    // Get SLA config for document
    @Transactional(readOnly = true)
    public DocumentSlaConfigDTO getSlaConfig(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));
        return DocumentSlaConfigDTO.builder()
                .reminderHours(document.getSlaReminderHours())
                .deptAdminEscalationHours(document.getSlaDeptAdminEscalationHours())
                .superAdminEscalationHours(document.getSlaSuperAdminEscalationHours())
                .violationHours(document.getSlaViolationHours())
                .emailEnabled(document.getSlaEmailEnabled())
                .smsEnabled(document.getSlaSmsEnabled())
                .dashboardEnabled(document.getSlaDashboardEnabled())
                .build();
    }

    // Calculate SLA timings for display (hours remaining until each milestone)
    public Map<String, Object> calculateSlaTimings(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

        Map<String, Object> timings = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime uploadDate = document.getUploadDate();

        if (uploadDate == null) {
            timings.put("uploadDate", null);
            timings.put("acknowledgementDeadline", null);
            timings.put("acknowledgementHoursRemaining", 0);
            timings.put("escalationL1Deadline", null);
            timings.put("escalationL1HoursRemaining", 0);
            timings.put("escalationL2Deadline", null);
            timings.put("escalationL2HoursRemaining", 0);
            timings.put("violationDeadline", null);
            timings.put("violationHoursRemaining", 0);
            return timings;
        }

        timings.put("uploadDate", uploadDate);
        timings.put("isManualSla", document.getIsSlaManual());
        timings.put("slaConfiguredAt", document.getSlaConfiguredAt());

        // Acknowledgement timing
        if (document.getSlaReminderHours() != null && document.getSlaReminderHours() > 0) {
            LocalDateTime ackDeadline = uploadDate.plusHours(document.getSlaReminderHours());
            timings.put("acknowledgementDeadline", ackDeadline);
            long hoursRemaining = java.time.temporal.ChronoUnit.HOURS.between(now, ackDeadline);
            timings.put("acknowledgementHoursRemaining", Math.max(0, hoursRemaining));
            timings.put("acknowledgementStatus", now.isBefore(ackDeadline) ? "PENDING" : "OVERDUE");
        }

        // Escalation L1 timing
        if (document.getSlaDeptAdminEscalationHours() != null && document.getSlaDeptAdminEscalationHours() > 0) {
            LocalDateTime escL1Deadline = uploadDate.plusHours(document.getSlaDeptAdminEscalationHours());
            timings.put("escalationL1Deadline", escL1Deadline);
            long hoursRemaining = java.time.temporal.ChronoUnit.HOURS.between(now, escL1Deadline);
            timings.put("escalationL1HoursRemaining", Math.max(0, hoursRemaining));
            timings.put("escalationL1Status", now.isBefore(escL1Deadline) ? "PENDING" : "ESCALATED");
        }

        // Escalation L2 timing
        if (document.getSlaSuperAdminEscalationHours() != null && document.getSlaSuperAdminEscalationHours() > 0) {
            LocalDateTime escL2Deadline = uploadDate.plusHours(document.getSlaSuperAdminEscalationHours());
            timings.put("escalationL2Deadline", escL2Deadline);
            long hoursRemaining = java.time.temporal.ChronoUnit.HOURS.between(now, escL2Deadline);
            timings.put("escalationL2HoursRemaining", Math.max(0, hoursRemaining));
            timings.put("escalationL2Status", now.isBefore(escL2Deadline) ? "PENDING" : "ESCALATED");
        }

        // Violation timing
        if (document.getSlaViolationHours() != null && document.getSlaViolationHours() > 0) {
            LocalDateTime violationDeadline = uploadDate.plusHours(document.getSlaViolationHours());
            timings.put("violationDeadline", violationDeadline);
            long hoursRemaining = java.time.temporal.ChronoUnit.HOURS.between(now, violationDeadline);
            timings.put("violationHoursRemaining", Math.max(0, hoursRemaining));
            timings.put("violationStatus", now.isBefore(violationDeadline) ? "PENDING" : "VIOLATED");
        }

        return timings;
    }

    // Clear manual SLA (revert to policy-based)
    @Transactional
    public DocumentResponseDTO clearManualSla(Long documentId) {
        log.info("Clearing manual SLA for document ID: {}", documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));
        document.setSlaReminderHours(null);
        document.setSlaDeptAdminEscalationHours(null);
        document.setSlaSuperAdminEscalationHours(null);
        document.setSlaViolationHours(null);
        document.setSlaEmailEnabled(null);
        document.setSlaSmsEnabled(null);
        document.setSlaDashboardEnabled(null);
        document.setSlaConfiguredAt(null);
        document.setIsSlaManual(false);
        Document saved = documentRepository.save(document);
        log.info("Manual SLA cleared for document ID: {}", documentId);
        return toDTO(saved);
    }

    // ===== HELPERS =====

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String formatEnumName(String enumName) {
        return enumName.replace("_", " ").toLowerCase().substring(0, 1).toUpperCase() +
                enumName.replace("_", " ").toLowerCase().substring(1);
    }
}
