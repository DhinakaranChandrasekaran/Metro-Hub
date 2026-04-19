package com.metrohub.dto;

import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.models.Document.DocumentStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class SearchDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SearchFilterDTO {
        private String keyword;
        private Long departmentId;
        private DocumentType documentType;
        private Priority priority;
        private DocumentStatus status;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private LocalDate deadlineFrom;
        private LocalDate deadlineTo;
        private Boolean hasDeadline;
        private Boolean isOverdue;
        private Boolean highPriorityOnly;

        @Builder.Default
        private String sortBy = "upload_date";
        @Builder.Default
        private String sortDirection = "desc";
        @Builder.Default
        private Integer page = 0;
        @Builder.Default
        private Integer size = 20;

        public boolean hasFilters() {
            return keyword != null && !keyword.isBlank() ||
                   departmentId != null || documentType != null || priority != null ||
                   status != null || dateFrom != null || dateTo != null ||
                   deadlineFrom != null || deadlineTo != null ||
                   Boolean.TRUE.equals(hasDeadline) || Boolean.TRUE.equals(isOverdue) ||
                   Boolean.TRUE.equals(highPriorityOnly);
        }

        public String getFilterDescription() {
            StringBuilder desc = new StringBuilder();
            if (keyword != null && !keyword.isBlank()) desc.append("keyword='").append(keyword).append("' ");
            if (departmentId != null) desc.append("dept=").append(departmentId).append(" ");
            if (documentType != null) desc.append("type=").append(documentType).append(" ");
            if (priority != null) desc.append("priority=").append(priority).append(" ");
            if (status != null) desc.append("status=").append(status).append(" ");
            if (dateFrom != null || dateTo != null) desc.append("dates=").append(dateFrom).append("-").append(dateTo).append(" ");
            if (Boolean.TRUE.equals(isOverdue)) desc.append("overdue ");
            if (Boolean.TRUE.equals(highPriorityOnly)) desc.append("highPriority ");
            return desc.length() > 0 ? desc.toString().trim() : "none";
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SearchRequestDTO {
        private String keyword;
        private Long departmentId;
        private DocumentType documentType;
        private Priority priority;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private Long uploadedBy;
        private String tags;

        @Builder.Default
        private Integer page = 0;
        @Builder.Default
        private Integer size = 10;
        @Builder.Default
        private String sortBy = "uploadDate";
        @Builder.Default
        private String sortDirection = "DESC";
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SearchResultDTO {
        private List<DocumentDTOs.DocumentCardDTO> documents;
        private Integer currentPage;
        private Integer totalPages;
        private Long totalElements;
        private Integer pageSize;
        private Boolean isFirstPage;
        private Boolean isLastPage;
        private Boolean hasNext;
        private Boolean hasPrevious;
        private String query;
        private String filtersApplied;
        private String sortBy;
        private String sortDirection;
        private Long searchTimeMs;
        private List<FacetCount> departmentFacets;
        private List<FacetCount> documentTypeFacets;
        private List<FacetCount> priorityFacets;

        @Data @NoArgsConstructor @AllArgsConstructor @Builder
        public static class FacetCount {
            private String name;
            private String displayName;
            private Long count;
        }
    }
}
