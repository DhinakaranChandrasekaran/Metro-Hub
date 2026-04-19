package com.metrohub.services;

import com.metrohub.exception.ResourceNotFoundException;
import com.metrohub.dto.PolicyDTOs.LegalHoldRequestDTO;
import com.metrohub.dto.PolicyDTOs.LegalHoldResponseDTO;
import com.metrohub.models.Document;
import com.metrohub.models.User;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalHoldService {

    private static final Logger log = LoggerFactory.getLogger(LegalHoldService.class);

    private final DocumentRepository documentRepository;

    @Transactional
    public LegalHoldResponseDTO applyLegalHold(Long documentId, LegalHoldRequestDTO request) {
        log.info("🔒 Applying legal hold to document ID={}", documentId);

        Document document = findDocumentOrThrow(documentId);

        // Check if already under legal hold
        if (Boolean.TRUE.equals(document.getLegalHold())) {
            log.warn("Document ID={} is already under legal hold", documentId);
            throw new IllegalStateException("Document is already under legal hold");
        }

        // Get current user
        User currentUser = SecurityUtils.getCurrentUser();

        // Apply legal hold
        document.setLegalHold(true);
        document.setLegalHoldReason(request.getReason());
        document.setLegalHoldBy(currentUser);
        document.setLegalHoldDate(LocalDateTime.now());

        Document saved = documentRepository.save(document);

        log.info("🔒 Legal hold applied to document ID={} by user={}, reason={}", 
                documentId, currentUser.getEmail(), request.getReason());

        LegalHoldResponseDTO response = toDTO(saved);
        response.setMessage("Legal hold applied successfully");

        return response;
    }

    @Transactional
    public LegalHoldResponseDTO removeLegalHold(Long documentId, String removalReason) {
        log.info("🔓 Removing legal hold from document ID={}", documentId);

        Document document = findDocumentOrThrow(documentId);

        // Check if under legal hold
        if (!Boolean.TRUE.equals(document.getLegalHold())) {
            log.warn("Document ID={} is not under legal hold", documentId);
            throw new IllegalStateException("Document is not under legal hold");
        }

        // Get current user
        User currentUser = SecurityUtils.getCurrentUser();

        // Log the removal before clearing
        String previousReason = document.getLegalHoldReason();
        String previousHeldBy = document.getLegalHoldBy() != null ? 
                document.getLegalHoldBy().getName() : "Unknown";

        // Remove legal hold
        document.setLegalHold(false);
        document.setLegalHoldReason(null);
        document.setLegalHoldBy(null);
        document.setLegalHoldDate(null);

        Document saved = documentRepository.save(document);

        log.info("🔓 Legal hold removed from document ID={} by user={}. " +
                 "Previous reason: {}, Previous held by: {}, Removal reason: {}", 
                documentId, currentUser.getEmail(), previousReason, previousHeldBy, removalReason);

        LegalHoldResponseDTO response = toDTO(saved);
        response.setMessage("Legal hold removed successfully. Removal reason: " + removalReason);

        return response;
    }

        public LegalHoldResponseDTO getLegalHoldStatus(Long documentId) {
        log.debug("Getting legal hold status for document ID={}", documentId);

        Document document = findDocumentOrThrow(documentId);
        return toDTO(document);
    }

        public boolean isUnderLegalHold(Long documentId) {
        Document document = findDocumentOrThrow(documentId);
        return isUnderLegalHold(document);
    }

        public boolean isUnderLegalHold(Document document) {
        return Boolean.TRUE.equals(document.getLegalHold());
    }

        public List<LegalHoldResponseDTO> getAllDocumentsUnderLegalHold() {
        log.info("Fetching all documents under legal hold");

        List<Document> documents = documentRepository.findByLegalHoldTrue();

        return documents.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

        public List<LegalHoldResponseDTO> getDocumentsUnderLegalHoldByDepartment(Long departmentId) {
        log.info("Fetching documents under legal hold for department ID={}", departmentId);

        List<Document> documents = documentRepository.findByDepartmentIdAndLegalHoldTrue(departmentId);

        return documents.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

        public void validateActionAllowed(Long documentId, String actionDescription) {
        Document document = findDocumentOrThrow(documentId);
        validateActionAllowed(document, actionDescription);
    }

        public void validateActionAllowed(Document document, String actionDescription) {
        if (isUnderLegalHold(document)) {
            String message = String.format(
                "Action '%s' is not allowed on document '%s' (ID=%d) - Document is under LEGAL HOLD. " +
                "Reason: %s. Applied by: %s on %s",
                actionDescription,
                document.getFileName(),
                document.getId(),
                document.getLegalHoldReason(),
                document.getLegalHoldBy() != null ? document.getLegalHoldBy().getName() : "Unknown",
                document.getLegalHoldDate()
            );

            log.warn("🚫 Legal hold violation: {}", message);
            throw new IllegalStateException(message);
        }
    }

        public LegalHoldResponseDTO toDTO(Document document) {
        return LegalHoldResponseDTO.builder()
                .documentId(document.getId())
                .documentName(document.getFileName())
                .legalHold(document.getLegalHold())
                .legalHoldReason(document.getLegalHoldReason())
                .legalHoldById(document.getLegalHoldBy() != null ? 
                        document.getLegalHoldBy().getId() : null)
                .legalHoldByName(document.getLegalHoldBy() != null ? 
                        document.getLegalHoldBy().getName() : null)
                .legalHoldDate(document.getLegalHoldDate())
                .build();
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private Document findDocumentOrThrow(Long documentId) {
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }
        return docOpt.get();
    }
}
