package com.metrohub.dto;

import com.metrohub.models.Document.Priority;
import com.metrohub.models.Document.DocumentType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardSummaryDTO {
        private SummaryCard totalDocuments;
        private SummaryCard highPriorityDocuments;
        private SummaryCard documentsWithDeadlines;
        private SummaryCard pendingActionsCount;
        private SummaryCard documentsUploadedToday;
        private Long overdueCount;
        private Long dueSoonCount;
        private Long unreadAlertsCount;
        private List<DepartmentStat> departmentBreakdown;
        private List<DocumentTypeStat> documentTypeBreakdown;

        @Data @NoArgsConstructor @AllArgsConstructor @Builder
        public static class SummaryCard {
            private String label;
            private Long count;
            private String icon;
            private String color;
            private String description;
        }

        @Data @NoArgsConstructor @AllArgsConstructor @Builder
        public static class DepartmentStat {
            private Long departmentId;
            private String departmentName;
            private String departmentCode;
            private Long documentCount;
            private Long highPriorityCount;
        }

        @Data @NoArgsConstructor @AllArgsConstructor @Builder
        public static class DocumentTypeStat {
            private String documentType;
            private String displayName;
            private Long count;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DeadlineTrackingDTO {
        private Long totalWithDeadlines;
        private Long overdueCount;
        private Long dueIn3DaysCount;
        private Long dueIn7DaysCount;
        private Long dueIn30DaysCount;
        private List<DeadlineDocumentDTO> overdueDocuments;
        private List<DeadlineDocumentDTO> dueIn3Days;
        private List<DeadlineDocumentDTO> dueIn7Days;
        private List<DeadlineDocumentDTO> dueIn30Days;

        @Data @NoArgsConstructor @AllArgsConstructor @Builder
        public static class DeadlineDocumentDTO {
            private Long documentId;
            private String fileName;
            private String documentType;
            private String departmentName;
            private String priority;
            private String deadline;
            private Long daysRemaining;
            private String status;
            private String summary;
            private String uploadDate;
            private String urgencyLevel;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PendingActionDTO {
        private Long documentId;
        private String fileName;
        private DocumentType documentType;
        private String documentTypeName;
        private String departmentName;
        private Long departmentId;
        private Priority priority;
        private String priorityDisplay;
        private Boolean isHighPriority;
        private LocalDate deadline;
        private String deadlineFormatted;
        private Long daysRemaining;
        private String daysRemainingText;
        private Boolean isOverdue;
        private Boolean isDueToday;
        private Boolean hasDeadline;
        private String status;
        private String summary;
        private LocalDateTime uploadDate;
        private String uploadedByName;
        private String actionReason;
        private String urgencyLevel;

        public static String calculateDaysRemainingText(Long daysRemaining) {
            if (daysRemaining == null) return "No deadline";
            if (daysRemaining < 0) {
                long overdueDays = Math.abs(daysRemaining);
                return overdueDays + " day" + (overdueDays == 1 ? "" : "s") + " overdue";
            } else if (daysRemaining == 0) {
                return "Due today";
            } else if (daysRemaining == 1) {
                return "Due tomorrow";
            } else if (daysRemaining <= 3) {
                return daysRemaining + " days left (urgent)";
            } else if (daysRemaining <= 7) {
                return daysRemaining + " days left";
            } else {
                return daysRemaining + " days remaining";
            }
        }

        public static String calculateUrgencyLevel(Priority priority, Long daysRemaining, Boolean isOverdue) {
            if (Boolean.TRUE.equals(isOverdue)) return "CRITICAL";
            if (priority == Priority.HIGH) {
                if (daysRemaining != null && daysRemaining <= 3) return "CRITICAL";
                return "HIGH";
            }
            if (daysRemaining != null) {
                if (daysRemaining <= 1) return "HIGH";
                else if (daysRemaining <= 3) return "MEDIUM";
            }
            return "LOW";
        }

        public static String buildActionReason(Priority priority, Long daysRemaining, Boolean isOverdue) {
            StringBuilder reason = new StringBuilder();
            if (Boolean.TRUE.equals(isOverdue)) reason.append("OVERDUE - ");
            if (priority == Priority.HIGH) reason.append("High priority document");
            if (daysRemaining != null && daysRemaining >= 0) {
                if (reason.length() > 0) reason.append(" with ");
                if (daysRemaining == 0) reason.append("deadline TODAY");
                else if (daysRemaining == 1) reason.append("deadline tomorrow");
                else if (daysRemaining <= 3) reason.append("deadline in ").append(daysRemaining).append(" days");
            }
            return reason.length() > 0 ? reason.toString() : "Requires review";
        }
    }
}
