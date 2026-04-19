package com.metrohub.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ComplianceSummaryDTO {
        private Long totalDocuments;
        private Long documentsThisMonth;
        private Long documentsThisWeek;
        private Long safetyDocuments;
        private Long circularDocuments;
        private Long policyDocuments;
        private Long otherDocuments;
        private Long totalAcknowledgements;
        private Long pendingAcknowledgements;
        private Long acknowledgementsThisMonth;
        private Long lateAcknowledgements;
        private Double acknowledgementRate;
        private Long totalViolations;
        private Long resolvedViolations;
        private Long unresolvedViolations;
        private Long criticalViolations;
        private Long highViolations;
        private Long mediumViolations;
        private Long violationsThisMonth;
        private Double overallCompliancePercentage;
        private Double safetyCompliancePercentage;
        private Double nonSafetyCompliancePercentage;
        private LocalDateTime filterStartDate;
        private LocalDateTime filterEndDate;
        private Long filterDepartmentId;
        private String filterDepartmentName;
        private String filterDocumentType;
        private LocalDateTime generatedAt;
        private String generatedBy;
        private String reportPeriod;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentComplianceDTO {
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private String departmentHead;
        private String departmentEmail;
        private Long totalUsers;
        private Long activeUsers;
        private Long usersWithViolations;
        private Long repeatDefaulters;
        private Long documentsReceived;
        private Long documentsAcknowledged;
        private Long documentsPending;
        private Long totalAcknowledgementsRequired;
        private Long totalAcknowledgementsDone;
        private Long pendingAcknowledgements;
        private Long lateAcknowledgements;
        private Double acknowledgementRate;
        private Long totalViolations;
        private Long resolvedViolations;
        private Long unresolvedViolations;
        private Long criticalViolations;
        private Long highViolations;
        private Long mediumViolations;
        private Double complianceScore;
        private String riskLevel;
        private Integer riskRank;
        private Long violationsLastMonth;
        private Long violationsThisMonth;
        private Double violationTrend;
        private LocalDateTime dataAsOf;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentAuditTrailDTO {
        private Long documentId;
        private String fileName;
        private String documentType;
        private String priority;
        private String status;
        private Long fileSizeBytes;
        private String fileExtension;
        private String description;
        private Long uploadedById;
        private String uploadedByName;
        private String uploadedByEmail;
        private String uploadedByEmployeeId;
        private LocalDateTime uploadDate;
        private Long targetDepartmentId;
        private String targetDepartmentName;
        private String targetDepartmentCode;
        private Integer totalUsersNotified;
        private Integer totalUsersInDepartment;
        private List<NotificationRecord> notificationHistory;
        private Integer acknowledgedCount;
        private Integer pendingCount;
        private Double acknowledgementRate;
        private List<AcknowledgementRecord> acknowledgedUsers;
        private List<PendingUserRecord> pendingUsers;
        private List<EscalationRecord> escalationHistory;
        private Integer remindersSent;
        private Integer deptAdminEscalations;
        private Integer superAdminEscalations;
        private Integer totalViolations;
        private Integer resolvedViolations;
        private Integer unresolvedViolations;
        private List<ViolationRecord> violationDetails;
        private LocalDateTime reportGeneratedAt;
        private String reportGeneratedBy;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class NotificationRecord {
            private Long userId;
            private String userName;
            private String userEmail;
            private String notificationChannel;
            private LocalDateTime sentAt;
            private Boolean delivered;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class AcknowledgementRecord {
            private Long userId;
            private String userName;
            private String userEmail;
            private String employeeId;
            private LocalDateTime acknowledgedAt;
            private String ipAddress;
            private Boolean wasLate;
            private Integer daysToAcknowledge;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PendingUserRecord {
            private Long userId;
            private String userName;
            private String userEmail;
            private String employeeId;
            private Integer daysPending;
            private Boolean hasViolation;
            private Boolean reminderSent;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class EscalationRecord {
            private String escalationType;
            private Long targetUserId;
            private String targetUserName;
            private LocalDateTime escalatedAt;
            private String message;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class ViolationRecord {
            private Long violationId;
            private Long userId;
            private String userName;
            private String userEmail;
            private LocalDateTime violationDate;
            private Integer daysDelayed;
            private String severity;
            private Boolean resolved;
            private LocalDateTime resolvedDate;
            private String resolvedByName;
            private String remarks;
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReportFilterDTO {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long departmentId;
        private String departmentName;
        private String documentType;
        private Long userId;
        private Boolean includeResolved;
        private Boolean includeUnresolved;
        private String violationSeverity;
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDirection;
        private String exportFormat;
        private Boolean includeCharts;
        private Boolean includeSummary;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserDefaulterDTO {
        private Long userId;
        private String userName;
        private String userEmail;
        private String employeeId;
        private String userRole;
        private Boolean isActive;
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Long totalDocumentsAssigned;
        private Long documentsAcknowledged;
        private Long documentsPending;
        private Long lateAcknowledgements;
        private Double onTimeRate;
        private Long totalViolations;
        private Long resolvedViolations;
        private Long unresolvedViolations;
        private Long criticalViolations;
        private Long highViolations;
        private Long mediumViolations;
        private Long violationsLast6Months;
        private List<MonthlyViolationCount> violationHistory;
        private Long averageDelayDays;
        private Long maxDelayDays;
        private Boolean isRepeatDefaulter;
        private Boolean isChronicDefaulter;
        private String defaulterCategory;
        private Integer defaulterRank;
        private LocalDateTime lastAcknowledgementDate;
        private LocalDateTime lastViolationDate;
        private LocalDateTime lastLoginDate;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class MonthlyViolationCount {
            private String month;
            private Long violationCount;
            private Long lateAckCount;
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ViolationTrendDTO {
        private Long totalViolationsAllTime;
        private Long violationsThisYear;
        private Long violationsThisMonth;
        private Long violationsLastMonth;
        private Double monthOverMonthChange;
        private List<MonthlyTrend> monthlyTrends;
        private List<DepartmentRisk> departmentRiskRanking;
        private Long safetyViolations;
        private Long nonSafetyViolations;
        private Double safetyToNonSafetyRatio;
        private Double averageAcknowledgementDelayDays;
        private Double medianAcknowledgementDelayDays;
        private Integer maxAcknowledgementDelayDays;
        private List<DelayDistribution> delayDistribution;
        private Long totalDefaulters;
        private Long repeatDefaulters;
        private Long chronicDefaulters;
        private List<ChronicDefaulter> topChronicDefaulters;
        private Double averageResolutionTimeDays;
        private Long resolvedThisMonth;
        private Long pendingResolution;
        private Double resolutionRate;
        private Long totalReminders;
        private Long deptAdminEscalations;
        private Long superAdminEscalations;
        private Double escalationRate;
        private LocalDateTime analysisStartDate;
        private LocalDateTime analysisEndDate;
        private LocalDateTime generatedAt;
        private String generatedBy;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class MonthlyTrend {
            private String month;
            private String monthName;
            private Long newViolations;
            private Long resolvedViolations;
            private Long cumulativeViolations;
            private Double complianceRate;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class DepartmentRisk {
            private Integer rank;
            private Long departmentId;
            private String departmentName;
            private String departmentCode;
            private Long totalViolations;
            private Long unresolvedViolations;
            private Double complianceRate;
            private String riskLevel;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class DelayDistribution {
            private String delayRange;
            private Long count;
            private Double percentage;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class ChronicDefaulter {
            private Integer rank;
            private Long userId;
            private String userName;
            private String employeeId;
            private String departmentName;
            private Long totalViolations;
            private Long unresolvedViolations;
            private String defaulterCategory;
        }
    }
}
