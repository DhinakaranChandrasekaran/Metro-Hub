package com.metrohub.dto;

import com.metrohub.models.ComplianceViolation.ViolationType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class ViolationDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @SuppressWarnings("unused")
    public static class ViolationDTO {
        private Long id;
        private Long documentId;
        private String documentName;
        private String documentType;
        private String documentPriority;
        private LocalDateTime documentUploadDate;
        private Long userId;
        private String userName;
        private String userEmail;
        private String userEmployeeId;
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private ViolationType violationType;
        private String violationTypeDisplay;
        private LocalDateTime violationDate;
        private Integer daysDelayed;
        private Long policyRuleId;
        private String policyRuleName;
        private Integer slaHoursApplied;
        private Boolean resolved;
        private Long resolvedById;
        private String resolvedByName;
        private LocalDateTime resolvedDate;
        private String remarks;
        private Boolean acknowledgedLate;
        private LocalDateTime lateAcknowledgementDate;
        private Boolean reminderSent;
        private LocalDateTime reminderSentAt;
        private Boolean deptAdminEscalated;
        private LocalDateTime deptAdminEscalatedAt;
        private Boolean superAdminEscalated;
        private LocalDateTime superAdminEscalatedAt;
        private LocalDateTime createdAt;
        private String status;
        private String severity;
        private Integer escalationLevel;

        public Integer getEscalationLevel() {
            if (Boolean.TRUE.equals(superAdminEscalated)) return 2;
            if (Boolean.TRUE.equals(deptAdminEscalated)) return 1;
            return 0;
        }

        public String getStatus() {
            if (Boolean.TRUE.equals(resolved)) return "RESOLVED";
            else if (Boolean.TRUE.equals(acknowledgedLate)) return "LATE_ACKNOWLEDGED";
            return "PENDING";
        }

        public String getSeverity() {
            if (daysDelayed == null) return "LOW";
            if (daysDelayed >= 14) return "CRITICAL";
            if (daysDelayed >= 10) return "HIGH";
            if (daysDelayed >= 7) return "MEDIUM";
            return "LOW";
        }

        public String getViolationTypeDisplay() {
            if (violationType == null) return "Unknown";
            return switch (violationType) {
                case ACK_DELAY -> "Acknowledgement Delay";
            };
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ViolationResolveRequestDTO {
        @NotBlank(message = "Remarks are required when resolving a violation")
        @Size(min = 10, max = 1000, message = "Remarks must be between 10 and 1000 characters")
        private String remarks;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ViolationSummaryDTO {
        private Long totalViolations;
        private Long pendingViolations;
        private Long resolvedViolations;
        private Long lateAcknowledgedViolations;
        private Long criticalCount;
        private Long highCount;
        private Long mediumCount;
        private Long remindersSent;
        private Long deptAdminEscalations;
        private Long superAdminEscalations;
        private Long departmentId;
        private String departmentName;
        private Long userId;
        private String userName;
        private Long violationsLast7Days;
        private Long violationsLast30Days;
        private Double complianceRate;

        public Double calculateComplianceRate(Long totalDocuments, Long onTimeAcknowledgements) {
            if (totalDocuments == null || totalDocuments == 0) return 100.0;
            return (onTimeAcknowledgements.doubleValue() / totalDocuments.doubleValue()) * 100;
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @SuppressWarnings("unused")
    public static class DepartmentViolationStatsDTO {
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Long totalViolations;
        private Long pendingViolations;
        private Long resolvedViolations;
        private Long lateAcknowledged;
        private Long criticalCount;
        private Long highCount;
        private Long mediumCount;
        private Double complianceRate;
        private String riskLevel;

        public String getRiskLevel() {
            if (pendingViolations == null) return "LOW_RISK";
            if (criticalCount != null && criticalCount > 0) return "HIGH_RISK";
            if (pendingViolations >= 5) return "HIGH_RISK";
            if (pendingViolations >= 2) return "MEDIUM_RISK";
            return "LOW_RISK";
        }
    }
}
