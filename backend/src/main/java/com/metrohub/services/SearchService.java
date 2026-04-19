package com.metrohub.services;

import com.metrohub.dto.DocumentDTOs.DocumentCardDTO;
import com.metrohub.dto.SearchDTOs.SearchFilterDTO;
import com.metrohub.dto.SearchDTOs.SearchResultDTO;
import com.metrohub.dto.SearchDTOs.SearchResultDTO.FacetCount;
import com.metrohub.models.Document;
import com.metrohub.models.Document.DocumentStatus;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.models.Metadata;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.MetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final DocumentRepository documentRepository;
    private final MetadataRepository metadataRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ============================================
    // SIMPLE SEARCH
    // ============================================

        public SearchResultDTO search(String keyword, Pageable pageable) {
        log.info("🔍 Simple search: keyword='{}', page={}", keyword, pageable.getPageNumber());
        
        long startTime = System.currentTimeMillis();
        
        Page<Document> results;
        
        if (keyword == null || keyword.trim().isEmpty()) {
            // If no keyword, return all documents
            results = documentRepository.findAllByOrderByUploadDateDesc(pageable);
        } else {
            // Search by keyword - use unsorted pageable for native query
            Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            results = documentRepository.searchByKeyword(keyword.trim(), unsortedPageable);
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return buildSearchResult(results, keyword, null, searchTime, pageable);
    }

    // ============================================
    // ADVANCED SEARCH
    // ============================================

        public SearchResultDTO advancedSearch(SearchFilterDTO filters) {
        log.info("🔍 Advanced search: {}", filters.getFilterDescription());
        
        long startTime = System.currentTimeMillis();
        
        // Build pageable with sorting
        Pageable pageable = buildPageable(filters);
        
        Page<Document> results;
        
        // Use custom query based on filters
        if (Boolean.TRUE.equals(filters.getIsOverdue())) {
            // Special case: search overdue documents
            results = searchOverdueInternal(filters, pageable);
        } else if (Boolean.TRUE.equals(filters.getHasDeadline())) {
            // Special case: documents with deadlines
            results = searchWithDeadlinesInternal(filters, pageable);
        } else if (filters.hasFilters()) {
            // General filtered search
            results = searchWithFiltersInternal(filters, pageable);
        } else {
            // No filters, return all
            results = documentRepository.findAllByOrderByUploadDateDesc(pageable);
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return buildSearchResult(results, filters.getKeyword(), filters.getFilterDescription(), 
                searchTime, pageable);
    }

    // ============================================
    // QUICK FILTERS
    // ============================================

        public SearchResultDTO searchHighPriority(Pageable pageable) {
        log.info("🔍 Searching high priority documents");
        
        long startTime = System.currentTimeMillis();
        
        Page<Document> results = documentRepository.findByPriority(Priority.HIGH, pageable);
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return buildSearchResult(results, null, "High Priority", searchTime, pageable);
    }

        public SearchResultDTO searchOverdue(Pageable pageable) {
        log.info("🔍 Searching overdue documents");
        
        long startTime = System.currentTimeMillis();
        
        Page<Document> results = documentRepository.findOverdueDocuments(pageable);
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return buildSearchResult(results, null, "Overdue", searchTime, pageable);
    }

        public SearchResultDTO searchByDepartment(Long departmentId, Pageable pageable) {
        log.info("🔍 Searching by department: {}", departmentId);
        
        long startTime = System.currentTimeMillis();
        
        Page<Document> results = documentRepository.findByDepartmentId(departmentId, pageable);
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return buildSearchResult(results, null, "Department ID: " + departmentId, searchTime, pageable);
    }

        public SearchResultDTO searchUpcomingDeadlines(int days, Pageable pageable) {
        log.info("🔍 Searching upcoming deadlines within {} days", days);
        
        long startTime = System.currentTimeMillis();
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        
        Page<Document> results = documentRepository.findDocumentsDueInRange(today, endDate, pageable);
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return buildSearchResult(results, null, "Due within " + days + " days", searchTime, pageable);
    }

    // ============================================
    // CONVERSION
    // ============================================

        public DocumentCardDTO convertToDocumentCard(Document doc) {
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

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    

    private SearchResultDTO buildSearchResult(Page<Document> page, String query, 
            String filtersApplied, long searchTimeMs, Pageable pageable) {
        
        List<DocumentCardDTO> cards = page.getContent().stream()
                .map(this::convertToDocumentCard)
                .collect(Collectors.toList());

        // Build facets
        List<FacetCount> priorityFacets = Arrays.stream(Priority.values())
                .map(this::toPriorityFacet)
                .filter(this::hasFacetCount)
                .collect(Collectors.toList());

        List<FacetCount> typeFacets = Arrays.stream(DocumentType.values())
                .map(this::toDocumentTypeFacet)
                .filter(this::hasFacetCount)
                .collect(Collectors.toList());

        Sort sort = pageable.getSort();
        String sortBy = "upload_date";
        String sortDirection = "desc";
        if (sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            sortBy = order.getProperty();
            sortDirection = order.getDirection().name().toLowerCase();
        }

        return SearchResultDTO.builder()
                .documents(cards)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .isFirstPage(page.isFirst())
                .isLastPage(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .query(query)
                .filtersApplied(filtersApplied)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchTimeMs(searchTimeMs)
                .priorityFacets(priorityFacets)
                .documentTypeFacets(typeFacets)
                .build();
    }

    

    private Pageable buildPageable(SearchFilterDTO filters) {
        Sort sort;
        String sortBy = filters.getSortBy() != null ? filters.getSortBy() : "upload_date";
        Sort.Direction direction = "asc".equalsIgnoreCase(filters.getSortDirection()) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;

        // Map frontend sort names to entity fields
        String sortField = switch (sortBy.toLowerCase()) {
            case "upload_date", "uploaddate" -> "uploadDate";
            case "priority" -> "priority";
            case "file_name", "filename" -> "fileName";
            case "document_type", "documenttype" -> "documentType";
            default -> "uploadDate";
        };

        sort = Sort.by(direction, sortField);

        return PageRequest.of(
                filters.getPage() != null ? filters.getPage() : 0,
                filters.getSize() != null ? filters.getSize() : 20,
                sort
        );
    }

    

    private Page<Document> searchWithFiltersInternal(SearchFilterDTO filters, Pageable pageable) {
        // Convert dates to LocalDateTime for query
        LocalDateTime dateFrom = filters.getDateFrom() != null ? 
                filters.getDateFrom().atStartOfDay() : null;
        LocalDateTime dateTo = filters.getDateTo() != null ? 
                filters.getDateTo().atTime(23, 59, 59) : null;

        // Get document type and priority as strings for native query
        String documentType = filters.getDocumentType() != null ? 
                filters.getDocumentType().name() : null;
        String priority = filters.getPriority() != null ? 
                filters.getPriority().name() : null;

        // Handle high priority only filter
        if (Boolean.TRUE.equals(filters.getHighPriorityOnly())) {
            priority = Priority.HIGH.name();
        }

        // Use unsorted pageable for native query
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        return documentRepository.searchWithFilters(
                filters.getKeyword(),
                filters.getDepartmentId(),
                documentType,
                priority,
                dateFrom,
                dateTo,
                unsortedPageable
        );
    }

    

    private Page<Document> searchOverdueInternal(SearchFilterDTO filters, Pageable pageable) {
        return documentRepository.findOverdueDocuments(pageable);
    }

    

    private Page<Document> searchWithDeadlinesInternal(SearchFilterDTO filters, Pageable pageable) {
        // Get all documents with deadlines
        return documentRepository.findDocumentsWithDeadlines(pageable);
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

    private FacetCount toPriorityFacet(Priority p) {
        return FacetCount.builder()
                .name(p.name())
                .displayName(DocumentCardDTO.getPriorityDisplay(p))
                .count(documentRepository.countByPriority(p))
                .build();
    }

    private FacetCount toDocumentTypeFacet(DocumentType t) {
        return FacetCount.builder()
                .name(t.name())
                .displayName(DocumentCardDTO.getDocumentTypeDisplayName(t))
                .count(documentRepository.countByDocumentType(t))
                .build();
    }

    private boolean hasFacetCount(FacetCount f) {
        return f.getCount() > 0;
    }
}
