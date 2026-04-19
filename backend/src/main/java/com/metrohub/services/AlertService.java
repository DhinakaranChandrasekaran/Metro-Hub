package com.metrohub.services;

import com.metrohub.dto.AlertDTO;
import com.metrohub.models.Alert;
import com.metrohub.models.Alert.AlertType;
import com.metrohub.models.Document;
import com.metrohub.models.Metadata;
import com.metrohub.repositories.AlertRepository;
import com.metrohub.repositories.MetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final MetadataRepository metadataRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ============================================
    // CREATE OPERATIONS
    // ============================================

    @Transactional
    public AlertDTO createAlert(Document document, AlertType alertType, String message) {
        log.info("🔔 Creating alert: type={}, documentId={}", alertType, document.getId());
        
        // Check for duplicate alert in last 24 hours
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        if (alertRepository.existsByDocumentIdAndAlertTypeSince(document.getId(), alertType, since)) {
            log.debug("Alert already exists for document {} with type {}, skipping", document.getId(), alertType);
            // Return existing alert instead
            List<Alert> existing = alertRepository.findByDocumentIdAndAlertType(document.getId(), alertType);
            if (!existing.isEmpty()) {
                return convertToAlertDTO(existing.get(0));
            }
        }

        Alert alert = Alert.builder()
                .document(document)
                .alertType(alertType)
                .message(message)
                .isRead(false)
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("✅ Alert created: id={}", savedAlert.getId());
        
        return convertToAlertDTO(savedAlert);
    }

    @Transactional
    public AlertDTO createHighPriorityAlert(Document document) {
        String message = String.format(
                "🔴 High priority document uploaded: '%s' - Requires immediate attention",
                document.getFileName()
        );
        return createAlert(document, AlertType.HIGH_PRIORITY_UPLOAD, message);
    }

    @Transactional
    public AlertDTO createDeadlineApproachingAlert(Document document, long daysRemaining) {
        String message = String.format(
                "⏰ Deadline approaching for '%s' - %d day%s remaining",
                document.getFileName(),
                daysRemaining,
                daysRemaining == 1 ? "" : "s"
        );
        return createAlert(document, AlertType.DEADLINE_APPROACHING, message);
    }

    @Transactional
    public AlertDTO createOverdueAlert(Document document) {
        String message = String.format(
                "❌ Document '%s' is now OVERDUE - Immediate action required",
                document.getFileName()
        );
        return createAlert(document, AlertType.DEADLINE_OVERDUE, message);
    }

    @Transactional
    public AlertDTO createDeadlineTodayAlert(Document document) {
        String message = String.format(
                "📅 Deadline TODAY for '%s' - Action required",
                document.getFileName()
        );
        return createAlert(document, AlertType.DEADLINE_TODAY, message);
    }

    // ============================================
    // RETRIEVE OPERATIONS
    // ============================================

    @Transactional(readOnly = true)
    public Page<AlertDTO> getAllAlerts(Pageable pageable) {
        log.debug("📋 Fetching all alerts, page: {}", pageable.getPageNumber());
        
        Page<Alert> alerts = alertRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<AlertDTO> dtos = alerts.getContent().stream()
                .map(this::convertToAlertDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, alerts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<AlertDTO> getUnreadAlerts(Pageable pageable) {
        log.debug("📋 Fetching unread alerts");
        
        Page<Alert> alerts = alertRepository.findByIsReadFalseOrderByCreatedAtDesc(pageable);
        
        List<AlertDTO> dtos = alerts.getContent().stream()
                .map(this::convertToAlertDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, alerts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getAlertsForDocument(Long documentId) {
        log.debug("📋 Fetching alerts for document: {}", documentId);
        
        return alertRepository.findByDocument_IdOrderByCreatedAtDesc(documentId).stream()
                .map(this::convertToAlertDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AlertDTO> getAlertsByType(AlertType alertType, Pageable pageable) {
        log.debug("📋 Fetching alerts by type: {}", alertType);
        
        Page<Alert> alerts = alertRepository.findByAlertTypeOrderByCreatedAtDesc(alertType, pageable);
        
        List<AlertDTO> dtos = alerts.getContent().stream()
                .map(this::convertToAlertDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, alerts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AlertDTO getAlertById(Long alertId) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            throw new RuntimeException("Alert not found: " + alertId);
        }
        Alert alert = alertOpt.get();
        return convertToAlertDTO(alert);
    }

    // ============================================
    // COUNT OPERATIONS
    // ============================================

    @Transactional(readOnly = true)
    public Long getUnreadCount() {
        return alertRepository.countByIsReadFalse();
    }

    @Transactional(readOnly = true)
    public Long getUnreadCountByType(AlertType alertType) {
        return alertRepository.countByAlertTypeAndIsReadFalse(alertType);
    }

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    @Transactional
    public AlertDTO markAsRead(Long alertId) {
        log.info("✓ Marking alert as read: {}", alertId);

        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            throw new RuntimeException("Alert not found: " + alertId);
        }
        Alert alert = alertOpt.get();
        
        alert.setIsRead(true);
        Alert savedAlert = alertRepository.save(alert);
        
        return convertToAlertDTO(savedAlert);
    }

    @Transactional
    public int markMultipleAsRead(List<Long> alertIds) {
        log.info("✓ Marking {} alerts as read", alertIds.size());
        
        int count = 0;
        for (Long id : alertIds) {
            try {
                Alert alert = alertRepository.findById(id).orElse(null);
                if (alert != null && !alert.getIsRead()) {
                    alert.setIsRead(true);
                    alertRepository.save(alert);
                    count++;
                }
            } catch (Exception e) {
                log.warn("Failed to mark alert {} as read: {}", id, e.getMessage());
            }
        }
        
        return count;
    }

    @Transactional
    public int markAllAsRead() {
        log.info("✓ Marking all alerts as read");
        return alertRepository.markAllAsRead();
    }

    @Transactional
    public int markAllAsReadForDocument(Long documentId) {
        log.info("✓ Marking all alerts as read for document: {}", documentId);
        return alertRepository.markAsReadByDocumentId(documentId);
    }

    // ============================================
    // SCHEDULED OPERATIONS
    // ============================================

    

    @Scheduled(cron = "${metrohub.alerts.deadline-check-cron:0 0 * * * *}")
    @Transactional
    public void checkAndCreateDeadlineAlerts() {
        log.info("⏰ Running scheduled deadline alert check...");
        
        LocalDate today = LocalDate.now();
        int alertsCreated = 0;

        // Get all documents with metadata containing deadlines
        List<Metadata> allMetadata = metadataRepository.findAll();
        
        for (Metadata metadata : allMetadata) {
            if (metadata.getDeadline() == null) continue;
            
            LocalDate deadline = metadata.getDeadline();
            Document document = metadata.getDocument();
            
            if (document == null || document.getStatus() == Document.DocumentStatus.ARCHIVED) {
                continue;
            }
            
            long daysUntil = ChronoUnit.DAYS.between(today, deadline);
            
            try {
                // Check for overdue
                if (daysUntil < 0) {
                    LocalDateTime since = LocalDateTime.now().minusDays(1);
                    if (!alertRepository.existsByDocumentIdAndAlertTypeSince(
                            document.getId(), AlertType.DEADLINE_OVERDUE, since)) {
                        createOverdueAlert(document);
                        alertsCreated++;
                    }
                }
                // Check for deadline today
                else if (daysUntil == 0) {
                    LocalDateTime since = LocalDateTime.now().minusHours(12);
                    if (!alertRepository.existsByDocumentIdAndAlertTypeSince(
                            document.getId(), AlertType.DEADLINE_TODAY, since)) {
                        createDeadlineTodayAlert(document);
                        alertsCreated++;
                    }
                }
                // Check for approaching deadline (within 3 days)
                else if (daysUntil <= 3) {
                    LocalDateTime since = LocalDateTime.now().minusDays(1);
                    if (!alertRepository.existsByDocumentIdAndAlertTypeSince(
                            document.getId(), AlertType.DEADLINE_APPROACHING, since)) {
                        createDeadlineApproachingAlert(document, daysUntil);
                        alertsCreated++;
                    }
                }
            } catch (Exception e) {
                log.error("Error creating deadline alert for document {}: {}", 
                        document.getId(), e.getMessage());
            }
        }
        
        log.info("✅ Deadline alert check complete. Alerts created: {}", alertsCreated);
    }

    

    @Scheduled(cron = "${metrohub.alerts.cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void scheduledCleanup() {
        log.info("🧹 Running scheduled alert cleanup...");
        int deleted = cleanupOldAlerts(30); // Keep alerts for 30 days
        log.info("✅ Alert cleanup complete. Deleted: {}", deleted);
    }

    @Transactional
    public int cleanupOldAlerts(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        return alertRepository.deleteOldReadAlerts(cutoff);
    }

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    @Transactional
    public void deleteAlertsForDocument(Long documentId) {
        log.info("🗑️ Deleting alerts for document: {}", documentId);
        alertRepository.deleteByDocument_Id(documentId);
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    

    private AlertDTO convertToAlertDTO(Alert alert) {
        Document doc = alert.getDocument();
        
        String documentName = doc != null ? doc.getFileName() : "Unknown";
        String documentPriority = doc != null && doc.getPriority() != null ? 
                doc.getPriority().name() : "UNKNOWN";
        String departmentName = doc != null && doc.getDepartment() != null ? 
                doc.getDepartment().getName() : "Unassigned";

        return AlertDTO.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .alertTypeName(AlertDTO.getAlertTypeName(alert.getAlertType()))
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .createdAtFormatted(alert.getCreatedAt() != null ? 
                        alert.getCreatedAt().format(DATETIME_FORMATTER) : null)
                .timeAgo(AlertDTO.calculateTimeAgo(alert.getCreatedAt()))
                .documentId(doc != null ? doc.getId() : null)
                .documentName(documentName)
                .documentPriority(documentPriority)
                .departmentName(departmentName)
                .icon(AlertDTO.getAlertIcon(alert.getAlertType()))
                .colorClass(AlertDTO.getAlertColorClass(alert.getAlertType()))
                .build();
    }
}
