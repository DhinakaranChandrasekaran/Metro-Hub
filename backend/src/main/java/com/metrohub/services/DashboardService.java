package com.metrohub.services;

import com.metrohub.dto.DashboardDTOs.*;
import com.metrohub.dto.DocumentDTOs.*;
import com.metrohub.dto.DashboardDTOs.DashboardSummaryDTO.*;
import com.metrohub.dto.DashboardDTOs.DeadlineTrackingDTO.*;

import com.metrohub.models.Department;
import com.metrohub.models.Document;
import com.metrohub.models.Document.DocumentStatus;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.models.Metadata;
import com.metrohub.repositories.AlertRepository;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.MetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final DocumentRepository documentRepository;
    private final MetadataRepository metadataRepository;
    private final DepartmentRepository departmentRepository;
    private final AlertRepository alertRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ============================================
    // SUMMARY OPERATIONS
    // ============================================

        public DashboardSummaryDTO getDashboardSummary() {
        log.info("📊 Generating dashboard summary...");
        
        long startTime = System.currentTimeMillis();
        
        // Calculate counts
        long totalDocs = documentRepository.count();
        long highPriorityDocs = documentRepository.countByPriority(Priority.HIGH);
        long todayUploads = documentRepository.countTodayUploads();
        long docsWithDeadlines = documentRepository.countDocumentsWithDeadlines();
        long pendingActions = documentRepository.countPendingActions();
        long overdueDocs = documentRepository.countOverdueDocuments();
        long dueSoonDocs = documentRepository.countDocumentsDueSoon(3);
        long unreadAlerts = alertRepository.countByIsReadFalse();

        // Build summary cards
        SummaryCard totalDocsCard = SummaryCard.builder()
                .label("Total Documents")
                .count(totalDocs)
                .icon("📁")
                .color("blue")
                .description("All documents in the system")
                .build();

        SummaryCard highPriorityCard = SummaryCard.builder()
                .label("High Priority")
                .count(highPriorityDocs)
                .icon("🔴")
                .color("red")
                .description("Documents requiring immediate attention")
                .build();

        SummaryCard deadlinesCard = SummaryCard.builder()
                .label("With Deadlines")
                .count(docsWithDeadlines)
                .icon("📅")
                .color("orange")
                .description("Documents with set deadlines")
                .build();

        SummaryCard pendingCard = SummaryCard.builder()
                .label("Pending Actions")
                .count(pendingActions)
                .icon("⚡")
                .color("yellow")
                .description("Documents requiring action")
                .build();

        SummaryCard todayCard = SummaryCard.builder()
                .label("Uploaded Today")
                .count(todayUploads)
                .icon("📤")
                .color("green")
                .description("Documents uploaded today")
                .build();

        // Get department breakdown
        List<DepartmentStat> departmentStats = getDepartmentBreakdown();

        // Get document type breakdown
        List<DocumentTypeStat> typeStats = getDocumentTypeBreakdown();

        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalDocuments(totalDocsCard)
                .highPriorityDocuments(highPriorityCard)
                .documentsWithDeadlines(deadlinesCard)
                .pendingActionsCount(pendingCard)
                .documentsUploadedToday(todayCard)
                .overdueCount(overdueDocs)
                .dueSoonCount(dueSoonDocs)
                .unreadAlertsCount(unreadAlerts)
                .departmentBreakdown(departmentStats)
                .documentTypeBreakdown(typeStats)
                .build();

        log.info("✅ Dashboard summary generated in {}ms", System.currentTimeMillis() - startTime);
        
        return summary;
    }

    // ============================================
    // DOCUMENT LISTING
    // ============================================

        public Page<DocumentCardDTO> getDocumentCards(Pageable pageable) {
        log.debug("📋 Fetching document cards, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Document> documents = documentRepository.findAllByOrderByUploadDateDesc(pageable);
        
        List<DocumentCardDTO> cards = documents.getContent().stream()
                .map(this::convertToDocumentCard)
                .collect(Collectors.toList());
        
        return new PageImpl<>(cards, pageable, documents.getTotalElements());
    }

        public List<DocumentCardDTO> getRecentDocuments(int days, int limit) {
        log.debug("📋 Fetching recent documents, days: {}, limit: {}", days, limit);
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        
        List<Document> documents = documentRepository.findRecentDocuments(since, pageable);
        
        return documents.stream()
                .map(this::convertToDocumentCard)
                .collect(Collectors.toList());
    }

    // ============================================
    // PENDING ACTIONS
    // ============================================

        public Page<PendingActionDTO> getPendingActions(Pageable pageable) {
        log.info("⚡ Fetching pending actions...");
        
        Page<Document> pendingDocs = documentRepository.findPendingActionDocuments(pageable);
        
        List<PendingActionDTO> actions = pendingDocs.getContent().stream()
                .map(this::convertToPendingAction)
                .collect(Collectors.toList());
        
        return new PageImpl<>(actions, pageable, pendingDocs.getTotalElements());
    }

        public Long getPendingActionsCount() {
        return documentRepository.countPendingActions();
    }

    // ============================================
    // DEADLINE TRACKING
    // ============================================

        public DeadlineTrackingDTO getDeadlineTracking() {
        log.info("📅 Generating deadline tracking data...");
        
        LocalDate today = LocalDate.now();
        LocalDate in3Days = today.plusDays(3);
        LocalDate in7Days = today.plusDays(7);
        LocalDate in30Days = today.plusDays(30);

        // Get counts
        long overdueCount = documentRepository.countOverdueDocuments();
        long dueIn3Days = documentRepository.countDocumentsDueInRange(today, in3Days);
        long dueIn7Days = documentRepository.countDocumentsDueInRange(today, in7Days);
        long dueIn30Days = documentRepository.countDocumentsDueInRange(today, in30Days);
        long totalWithDeadlines = documentRepository.countDocumentsWithDeadlines();

        // Get document lists (limit to 10 each)
        Pageable limitPage = PageRequest.of(0, 10);
        
        List<DeadlineDocumentDTO> overdueList = documentRepository.findOverdueDocuments(limitPage)
                .stream().map(this::convertToDeadlineDocument).collect(Collectors.toList());
        
        List<DeadlineDocumentDTO> due3DaysList = documentRepository.findDocumentsDueInRange(today, in3Days, limitPage)
                .stream().map(this::convertToDeadlineDocument).collect(Collectors.toList());
        
        List<DeadlineDocumentDTO> due7DaysList = documentRepository.findDocumentsDueInRange(today, in7Days, limitPage)
                .stream().map(this::convertToDeadlineDocument).collect(Collectors.toList());
        
        List<DeadlineDocumentDTO> due30DaysList = documentRepository.findDocumentsDueInRange(today, in30Days, limitPage)
                .stream().map(this::convertToDeadlineDocument).collect(Collectors.toList());

        return DeadlineTrackingDTO.builder()
                .totalWithDeadlines(totalWithDeadlines)
                .overdueCount(overdueCount)
                .dueIn3DaysCount(dueIn3Days)
                .dueIn7DaysCount(dueIn7Days)
                .dueIn30DaysCount(dueIn30Days)
                .overdueDocuments(overdueList)
                .dueIn3Days(due3DaysList)
                .dueIn7Days(due7DaysList)
                .dueIn30Days(due30DaysList)
                .build();
    }

        public Long getOverdueCount() {
        return documentRepository.countOverdueDocuments();
    }

        public Long getDueSoonCount(int days) {
        return documentRepository.countDocumentsDueSoon(days);
    }

    // ============================================
    // STATISTICS
    // ============================================

        public Long getTodayUploadCount() {
        return documentRepository.countTodayUploads();
    }

        public Long getHighPriorityCount() {
        return documentRepository.countByPriority(Priority.HIGH);
    }

        public Long getDocumentsWithDeadlinesCount() {
        return documentRepository.countDocumentsWithDeadlines();
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    

    private DocumentCardDTO convertToDocumentCard(Document doc) {
        // Get metadata for deadline
        Metadata metadata = metadataRepository.findByDocument_Id(doc.getId()).orElse(null);
        LocalDate deadline = metadata != null ? metadata.getDeadline() : null;
        String summary = metadata != null ? metadata.getSummary() : doc.getDescription();
        
        // Calculate deadline info
        Long daysUntilDeadline = null;
        Boolean isOverdue = false;
        String deadlineStatus = null;
        
        if (deadline != null) {
            daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            isOverdue = daysUntilDeadline < 0;
            deadlineStatus = getDeadlineStatusText(daysUntilDeadline);
        }

        // Get department name
        String deptName = doc.getDepartment() != null ? doc.getDepartment().getName() : "Unassigned";
        Long deptId = doc.getDepartment() != null ? doc.getDepartment().getId() : null;

        // Get uploader name
        String uploaderName = doc.getUploadedBy() != null ? doc.getUploadedBy().getName() : "Unknown";

        // Check if pending action
        boolean isPending = doc.getPriority() == Priority.HIGH || 
                           (deadline != null && !isOverdue && doc.getStatus() != DocumentStatus.ARCHIVED);

        return DocumentCardDTO.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileExtension(doc.getFileExtension())
                .fileSize(doc.getFileSize())
                .fileSizeFormatted(DocumentCardDTO.formatFileSize(doc.getFileSize()))
                .documentType(doc.getDocumentType())
                .documentTypeName(DocumentCardDTO.getDocumentTypeDisplayName(doc.getDocumentType()))
                .priority(doc.getPriority())
                .priorityDisplay(DocumentCardDTO.getPriorityDisplay(doc.getPriority()))
                .departmentName(deptName)
                .departmentId(deptId)
                .classificationConfidence(doc.getClassificationConfidence())
                .summary(truncate(summary, 200))
                .textPreview(truncate(doc.getExtractedText(), 200))
                .uploadDate(doc.getUploadDate())
                .uploadDateFormatted(formatDateTime(doc.getUploadDate()))
                .deadline(deadline)
                .deadlineFormatted(deadline != null ? deadline.format(DATE_FORMATTER) : null)
                .daysUntilDeadline(daysUntilDeadline)
                .deadlineStatus(deadlineStatus)
                .isOverdue(isOverdue)
                .status(doc.getStatus())
                .statusDisplay(DocumentCardDTO.getStatusDisplayName(doc.getStatus()))
                .uploadedByName(uploaderName)
                .hasDeadline(deadline != null)
                .isPendingAction(isPending)
                .downloadUrl("/api/documents/" + doc.getId() + "/download")
                .build();
    }

    

    private PendingActionDTO convertToPendingAction(Document doc) {
        Metadata metadata = metadataRepository.findByDocument_Id(doc.getId()).orElse(null);
        LocalDate deadline = metadata != null ? metadata.getDeadline() : null;
        String summary = metadata != null ? metadata.getSummary() : doc.getDescription();
        
        Long daysRemaining = null;
        Boolean isOverdue = false;
        Boolean isDueToday = false;
        
        if (deadline != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            isOverdue = daysRemaining < 0;
            isDueToday = daysRemaining == 0;
        }

        String deptName = doc.getDepartment() != null ? doc.getDepartment().getName() : "Unassigned";
        Long deptId = doc.getDepartment() != null ? doc.getDepartment().getId() : null;
        String uploaderName = doc.getUploadedBy() != null ? doc.getUploadedBy().getName() : "Unknown";

        return PendingActionDTO.builder()
                .documentId(doc.getId())
                .fileName(doc.getFileName())
                .documentType(doc.getDocumentType())
                .documentTypeName(DocumentCardDTO.getDocumentTypeDisplayName(doc.getDocumentType()))
                .departmentName(deptName)
                .departmentId(deptId)
                .priority(doc.getPriority())
                .priorityDisplay(DocumentCardDTO.getPriorityDisplay(doc.getPriority()))
                .isHighPriority(doc.getPriority() == Priority.HIGH)
                .deadline(deadline)
                .deadlineFormatted(deadline != null ? deadline.format(DATE_FORMATTER) : null)
                .daysRemaining(daysRemaining)
                .daysRemainingText(PendingActionDTO.calculateDaysRemainingText(daysRemaining))
                .isOverdue(isOverdue)
                .isDueToday(isDueToday)
                .hasDeadline(deadline != null)
                .status(doc.getStatus() != null ? doc.getStatus().name() : "UNKNOWN")
                .summary(truncate(summary, 150))
                .uploadDate(doc.getUploadDate())
                .uploadedByName(uploaderName)
                .actionReason(PendingActionDTO.buildActionReason(doc.getPriority(), daysRemaining, isOverdue))
                .urgencyLevel(PendingActionDTO.calculateUrgencyLevel(doc.getPriority(), daysRemaining, isOverdue))
                .build();
    }

    

    private DeadlineDocumentDTO convertToDeadlineDocument(Document doc) {
        Metadata metadata = metadataRepository.findByDocument_Id(doc.getId()).orElse(null);
        LocalDate deadline = metadata != null ? metadata.getDeadline() : null;
        
        Long daysRemaining = deadline != null ? 
                ChronoUnit.DAYS.between(LocalDate.now(), deadline) : null;
        
        String urgencyLevel = "LOW";
        if (daysRemaining != null) {
            if (daysRemaining < 0) urgencyLevel = "CRITICAL";
            else if (daysRemaining == 0) urgencyLevel = "HIGH";
            else if (daysRemaining <= 3) urgencyLevel = "MEDIUM";
        }

        String deptName = doc.getDepartment() != null ? doc.getDepartment().getName() : "Unassigned";

        return DeadlineDocumentDTO.builder()
                .documentId(doc.getId())
                .fileName(doc.getFileName())
                .documentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : "UNKNOWN")
                .departmentName(deptName)
                .priority(doc.getPriority() != null ? doc.getPriority().name() : "MEDIUM")
                .deadline(deadline != null ? deadline.format(DATE_FORMATTER) : null)
                .daysRemaining(daysRemaining)
                .status(doc.getStatus() != null ? doc.getStatus().name() : "ACTIVE")
                .summary(truncate(metadata != null ? metadata.getSummary() : doc.getDescription(), 100))
                .uploadDate(formatDateTime(doc.getUploadDate()))
                .urgencyLevel(urgencyLevel)
                .build();
    }

    

    private List<DepartmentStat> getDepartmentBreakdown() {
        List<DepartmentStat> stats = new ArrayList<>();
        
        List<Department> departments = departmentRepository.findAll();
        
        for (Department dept : departments) {
            long count = documentRepository.countByDepartmentId(dept.getId());
            long highPriorityCount = documentRepository.countByDepartmentIdAndPriority(dept.getId(), Priority.HIGH);
            
            if (count > 0) {
                stats.add(DepartmentStat.builder()
                        .departmentId(dept.getId())
                        .departmentName(dept.getName())
                        .departmentCode(dept.getCode())
                        .documentCount(count)
                        .highPriorityCount(highPriorityCount)
                        .build());
            }
        }
        
        return stats;
    }

    

    private List<DocumentTypeStat> getDocumentTypeBreakdown() {
        return Arrays.stream(DocumentType.values())
                .map(this::toDocumentTypeStat)
                .filter(this::hasDocuments)
                .collect(Collectors.toList());
    }

    private DocumentTypeStat toDocumentTypeStat(DocumentType type) {
        long count = documentRepository.countByDocumentType(type);
        return DocumentTypeStat.builder()
                .documentType(type.name())
                .displayName(DocumentCardDTO.getDocumentTypeDisplayName(type))
                .count(count)
                .build();
    }

    private boolean hasDocuments(DocumentTypeStat stat) {
        return stat.getCount() > 0;
    }

    

    private String getDeadlineStatusText(Long daysRemaining) {
        if (daysRemaining == null) return null;
        
        if (daysRemaining < 0) {
            return Math.abs(daysRemaining) + " days overdue";
        } else if (daysRemaining == 0) {
            return "Due today";
        } else if (daysRemaining == 1) {
            return "Due tomorrow";
        } else if (daysRemaining <= 3) {
            return "Due in " + daysRemaining + " days (urgent)";
        } else if (daysRemaining <= 7) {
            return "Due in " + daysRemaining + " days";
        } else {
            return "Due in " + daysRemaining + " days";
        }
    }

    

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
