package com.metrohub.services;

import com.metrohub.dto.AcknowledgementDTOs.AcknowledgementDTO;
import com.metrohub.dto.AcknowledgementDTOs.AcknowledgementResponseDTO;
import com.metrohub.dto.DocumentDTOs.DocumentResponseDTO;
import com.metrohub.models.Document;
import com.metrohub.models.DocumentAcknowledgement;
import com.metrohub.models.User;
import com.metrohub.repositories.DocumentAcknowledgementRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.UserRepository;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcknowledgementService {

    private static final Logger log = LoggerFactory.getLogger(AcknowledgementService.class);

    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final DocumentRepository documentRepository;
    private final com.metrohub.services.ComplianceViolationService violationService;
    private final UserRepository userRepository;

    @Transactional
    public AcknowledgementResponseDTO acknowledgeDocument(Long documentId, String notes, String ipAddress) {
        log.info("📝 Acknowledging document: {} by user: {}", documentId, SecurityUtils.getCurrentUserId());

        // Get current user ID from security context, then re-fetch from DB within this transaction
        // to ensure Hibernate session is active for lazy-loaded fields
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("No authenticated user");
        }
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + currentUserId));

        // Check if user can acknowledge
        if (!currentUser.canAcknowledge()) {
            throw new AccessDeniedException("You don't have permission to acknowledge documents");
        }

        // Get document
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        Document document = docOpt.get();

        // Verify user belongs to document's department (unless SUPER_ADMIN or UPLOAD_ADMIN)
        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isDepartmentUploadAdmin()) {
            Long userDeptId = currentUser.getDepartmentId();
            Long docDeptId = document.getDepartment() != null ? document.getDepartment().getId() : null;

            // Primary check: compare department IDs
            boolean deptMatch = userDeptId != null && docDeptId != null && userDeptId.equals(docDeptId);

            if (!deptMatch) {
                log.warn("Department mismatch: user dept_id={}, doc dept_id={}", userDeptId, docDeptId);
                throw new AccessDeniedException("You can only acknowledge documents in your department");
            }
        }

        // Check if already acknowledged
        if (acknowledgementRepository.existsByDocument_IdAndUser_Id(documentId, currentUser.getId())) {
            throw new IllegalArgumentException("Document already acknowledged by this user");
        }

        // Create acknowledgement
        DocumentAcknowledgement acknowledgement = DocumentAcknowledgement.builder()
            .document(document)
            .user(currentUser)
            .ipAddress(ipAddress)
            .notes(notes)
            .build();

        DocumentAcknowledgement saved = acknowledgementRepository.save(acknowledgement);
        log.info("✅ Document {} acknowledged by user {}", documentId, currentUser.getId());

        // Phase 7: Check if this is a late acknowledgement (violation exists)
        try {
            if (violationService.violationExists(documentId, currentUser.getId())) {
                violationService.markLateAcknowledgement(documentId, currentUser.getId());
                log.info("📝 Marked late acknowledgement for document {} by user {}", 
                        documentId, currentUser.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to process late acknowledgement check: {}", e.getMessage());
        }

        return convertToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public boolean hasAcknowledged(Long documentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return false;
        return acknowledgementRepository.existsByDocument_IdAndUser_Id(documentId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasUserAcknowledged(Long documentId, Long userId) {
        return acknowledgementRepository.existsByDocument_IdAndUser_Id(documentId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasUserAcknowledgedDocument(Long documentId) {
        return hasAcknowledged(documentId);
    }

    @Transactional(readOnly = true)
    public AcknowledgementDTO getAcknowledgement(Long documentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return null;
        
        return acknowledgementRepository.findByDocument_IdAndUser_Id(documentId, userId)
            .map(this::convertToDTO)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AcknowledgementDTO> getAcknowledgementsForDocument(Long documentId) {
        log.debug("Fetching acknowledgements for document: {}", documentId);

        // Verify access to document
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found");
        }
        Document document = docOpt.get();
        
        if (!SecurityUtils.canAccessDepartment(document.getDepartment().getId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }

        return acknowledgementRepository.findByDocument_IdOrderByAcknowledgedAtDesc(documentId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AcknowledgementResponseDTO> getAcknowledgementsForDocument(Long documentId, boolean asResponseDTO) {
        log.debug("Fetching acknowledgements for document: {} as ResponseDTO", documentId);

        // Verify access to document
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found");
        }
        Document document = docOpt.get();
        
        if (!SecurityUtils.canAccessDepartment(document.getDepartment().getId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }

        return acknowledgementRepository.findByDocument_IdOrderByAcknowledgedAtDesc(documentId)
            .stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AcknowledgementDTO> getMyAcknowledgements(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user");
        }

        Page<DocumentAcknowledgement> acknowledgements = 
            acknowledgementRepository.findByUser_Id(userId, pageable);
        
        List<AcknowledgementDTO> dtos = acknowledgements.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, acknowledgements.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<Document> getPendingDocuments(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentUserDepartmentId();
        
        if (userId == null || departmentId == null) {
            throw new IllegalStateException("User or department not found");
        }

        return acknowledgementRepository.findDocumentsPendingAcknowledgementByUser(
            userId, departmentId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Document> getPendingDocumentsList() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentUserDepartmentId();
        
        if (userId == null || departmentId == null) {
            return List.of();
        }

        return acknowledgementRepository.findDocumentsPendingAcknowledgementByUser(
            userId, departmentId);
    }

    @Transactional(readOnly = true)
    public long countPendingForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentUserDepartmentId();
        
        if (userId == null || departmentId == null) {
            return 0;
        }

        return acknowledgementRepository.countPendingForUser(userId, departmentId);
    }

    @Transactional(readOnly = true)
    public long countAcknowledgements(Long documentId) {
        return acknowledgementRepository.countByDocument_Id(documentId);
    }

    @Transactional(readOnly = true)
    public long countPendingAcknowledgements(Long documentId) {
        return acknowledgementRepository.countPendingAcknowledgements(documentId);
    }

    @Transactional(readOnly = true)
    public Double getAcknowledgementRatePercentage(Long documentId) {
        Double rate = acknowledgementRepository.getAcknowledgementRate(documentId);
        return rate != null ? rate : 0.0;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAcknowledgementRate(Long documentId) {
        Map<String, Object> result = new HashMap<>();
        Double rate = getAcknowledgementRatePercentage(documentId);
        long total = countPendingAcknowledgements(documentId) + countAcknowledgements(documentId);
        long acknowledged = countAcknowledgements(documentId);
        
        result.put("rate", rate);
        result.put("total", total);
        result.put("acknowledged", acknowledged);
        result.put("pending", total - acknowledged);
        
        return result;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getDocumentsPendingAcknowledgement() {
        List<Document> documents = getPendingDocumentsList();
        return documents.stream()
            .map(this::convertToDocumentResponseDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getPendingAcknowledgementCount() {
        return countPendingForCurrentUser();
    }

    @Transactional(readOnly = true)
    public Page<AcknowledgementDTO> getAcknowledgementsByDepartment(Long departmentId, Pageable pageable) {
        // Verify access
        if (!SecurityUtils.canAccessDepartment(departmentId)) {
            throw new AccessDeniedException("You don't have access to this department");
        }

        Page<DocumentAcknowledgement> acknowledgements = 
            acknowledgementRepository.findByDepartmentId(departmentId, pageable);
        
        List<AcknowledgementDTO> dtos = acknowledgements.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, acknowledgements.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Long> getUsersNotAcknowledged(Long documentId) {
        return acknowledgementRepository.findUsersNotAcknowledged(documentId)
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private AcknowledgementDTO convertToDTO(DocumentAcknowledgement acknowledgement) {
        Document document = acknowledgement.getDocument();
        User user = acknowledgement.getUser();

        return AcknowledgementDTO.builder()
            .id(acknowledgement.getId())
            .documentId(document != null ? document.getId() : null)
            .documentName(document != null ? document.getFileName() : null)
            .documentType(document != null && document.getDocumentType() != null ? 
                document.getDocumentType().name() : null)
            .priority(document != null && document.getPriority() != null ? 
                document.getPriority().name() : null)
            .userId(user != null ? user.getId() : null)
            .userName(user != null ? user.getName() : null)
            .userEmail(user != null ? user.getEmail() : null)
            .employeeId(user != null ? user.getEmployeeId() : null)
            .departmentName(user != null && user.getDepartmentEntity() != null ? 
                user.getDepartmentEntity().getName() : null)
            .acknowledgedAt(acknowledgement.getAcknowledgedAt())
            .ipAddress(acknowledgement.getIpAddress())
            .notes(acknowledgement.getNotes())
            .acknowledged(true)
            .timeAgo(calculateTimeAgo(acknowledgement.getAcknowledgedAt()))
            .build();
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        if (days < 30) return days + " day" + (days == 1 ? "" : "s") + " ago";
        
        return dateTime.toLocalDate().toString();
    }

    private AcknowledgementResponseDTO convertToResponseDTO(DocumentAcknowledgement acknowledgement) {
        Document document = acknowledgement.getDocument();
        User user = acknowledgement.getUser();

        return AcknowledgementResponseDTO.builder()
            .id(acknowledgement.getId())
            .documentId(document != null ? document.getId() : null)
            .documentName(document != null ? document.getFileName() : null)
            .userId(user != null ? user.getId() : null)
            .userName(user != null ? user.getName() : null)
            .userEmail(user != null ? user.getEmail() : null)
            .departmentId(user != null ? user.getDepartmentId() : null)
            .departmentName(user != null && user.getDepartmentEntity() != null ? 
                user.getDepartmentEntity().getName() : null)
            .acknowledgedAt(acknowledgement.getAcknowledgedAt())
            .ipAddress(acknowledgement.getIpAddress())
            .notes(acknowledgement.getNotes())
            .acknowledged(true)
            .build();
    }

    private DocumentResponseDTO convertToDocumentResponseDTO(Document document) {
        return DocumentResponseDTO.builder()
            .id(document.getId())
            .fileName(document.getFileName())
            .fileType(document.getFileType())
            .fileSize(document.getFileSize())
            .fileExtension(document.getFileExtension())
            .documentType(document.getDocumentType())
            .priority(document.getPriority())
            .departmentId(document.getDepartment() != null ? document.getDepartment().getId() : null)
            .departmentName(document.getDepartment() != null ? document.getDepartment().getName() : null)
            .uploadDate(document.getUploadDate())
            .status(document.getStatus())
            .description(document.getDescription())
            .build();
    }
}
