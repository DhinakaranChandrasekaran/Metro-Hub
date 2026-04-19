package com.metrohub.dto;

import com.metrohub.models.RiskScoreSnapshot.RiskLevel;
import com.metrohub.models.RiskScoreSnapshot.EntityType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentRiskDTO {
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Integer riskScore;
        private RiskLevel riskLevel;
        private String riskLevelColor;
        private String riskLevelDescription;
        private Integer lateAcknowledgementCount;
        private Integer totalViolationCount;
        private Integer pendingViolationCount;
        private Integer resolvedViolationCount;
        private Integer deptAdminEscalationCount;
        private Integer superAdminEscalationCount;
        private Integer legalHoldDocumentCount;
        private Integer safetyViolationCount;
        private Integer repeatOffenderCount;
        private Double complianceRate;
        private Integer totalDocuments;
        private Integer acknowledgedDocuments;
        private Integer pendingAcknowledgements;
        private Double avgDaysDelayed;
        private Integer violationsLast7Days;
        private Integer violationsLast30Days;
        private Integer violationsTrendPercentage;
        private String trendDirection;
        private Integer totalUsers;
        private Integer activeUsers;
        private Integer highRiskUserCount;
        private LocalDateTime lastCalculatedAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private Boolean hasRapidViolationIncrease;
        private Boolean hasMultipleEscalations;
        private Boolean hasCriticalPendingViolations;
        private Boolean hasRepeatOffenders;

        public boolean needsImmediateAttention() {
            return riskLevel == RiskLevel.CRITICAL ||
                   Boolean.TRUE.equals(hasRapidViolationIncrease) ||
                   Boolean.TRUE.equals(hasCriticalPendingViolations);
        }

        public String getWarningsSummary() {
            StringBuilder sb = new StringBuilder();
            if (Boolean.TRUE.equals(hasRapidViolationIncrease)) sb.append("Rapid violation increase detected. ");
            if (Boolean.TRUE.equals(hasMultipleEscalations)) sb.append("Multiple escalations in recent period. ");
            if (Boolean.TRUE.equals(hasCriticalPendingViolations)) sb.append("Critical pending violations exist. ");
            if (Boolean.TRUE.equals(hasRepeatOffenders)) sb.append("Repeat offenders identified. ");
            return sb.length() > 0 ? sb.toString().trim() : "No active warnings";
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RiskSummaryDTO {
        private Integer organizationRiskScore;
        private RiskLevel organizationRiskLevel;
        private String riskLevelColor;
        private String riskLevelDescription;
        private Integer totalDepartments;
        private Integer lowRiskDepartments;
        private Integer mediumRiskDepartments;
        private Integer highRiskDepartments;
        private Integer criticalRiskDepartments;
        private List<DepartmentRiskDTO> topRiskDepartments;
        private Integer totalUsers;
        private Integer activeUsers;
        private Integer lowRiskUsers;
        private Integer mediumRiskUsers;
        private Integer highRiskUsers;
        private Integer criticalRiskUsers;
        private Integer chronicDefaultersCount;
        private Integer repeatOffendersCount;
        private List<UserRiskDTO> topDefaulters;
        private Long totalViolations;
        private Long pendingViolations;
        private Long resolvedViolations;
        private Long lateAcknowledgedViolations;
        private Long criticalViolations;
        private Long highSeverityViolations;
        private Long mediumSeverityViolations;
        private Long totalRemindersSent;
        private Long deptAdminEscalations;
        private Long superAdminEscalations;
        private Long activeEscalations;
        private Double overallComplianceRate;
        private Long totalDocuments;
        private Long totalAcknowledgements;
        private Long pendingAcknowledgements;
        private Double avgResolutionTimeDays;
        private Long legalHoldDocuments;
        private Long legalHoldViolations;
        private Long safetyDocuments;
        private Long safetyViolations;
        private Long pendingSafetyViolations;
        private Long violationsLast7Days;
        private Long violationsLast30Days;
        private Long violationsLast90Days;
        private Integer violationTrendPercentage;
        private String trendDirection;
        private Map<String, Integer> departmentRiskDistribution;
        private Map<String, Integer> userRiskDistribution;
        private Boolean hasRapidViolationIncrease;
        private Boolean hasCriticalDepartments;
        private Boolean hasMultipleChronicDefaulters;
        private Boolean hasSafetyComplianceIssues;
        private List<String> activeWarnings;
        private LocalDateTime calculatedAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private String calculationVersion;

        public String getHealthStatus() {
            if (organizationRiskLevel == RiskLevel.CRITICAL) return "CRITICAL - Immediate action required";
            if (organizationRiskLevel == RiskLevel.HIGH) return "WARNING - Elevated risk detected";
            if (organizationRiskLevel == RiskLevel.MEDIUM) return "CAUTION - Moderate risk present";
            return "HEALTHY - Risk within acceptable limits";
        }

        public String getExecutiveSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Organization Risk Score: %d/100 (%s). ", organizationRiskScore, organizationRiskLevel));
            if (criticalRiskDepartments > 0) sb.append(String.format("%d department(s) at critical risk. ", criticalRiskDepartments));
            if (pendingViolations > 0) sb.append(String.format("%d pending violations. ", pendingViolations));
            if (chronicDefaultersCount > 0) sb.append(String.format("%d chronic defaulters identified. ", chronicDefaultersCount));
            sb.append(String.format("Overall compliance rate: %.1f%%.", overallComplianceRate));
            return sb.toString();
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RiskHeatmapDTO {
        private String title;
        private String description;
        private LocalDateTime generatedAt;
        private String periodCovered;
        private List<HeatmapCell> cells;
        private Map<String, Integer> riskDistribution;
        private Integer totalDepartments;
        private Integer avgRiskScore;
        private Integer maxRiskScore;
        private Integer minRiskScore;
        private String highestRiskDepartment;
        private String lowestRiskDepartment;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class HeatmapCell {
            private Long departmentId;
            private String departmentName;
            private String departmentCode;
            private Integer riskScore;
            private String riskLevel;
            private String cellColor;
            private Integer violationCount;
            private Integer pendingCount;
            private Double complianceRate;
            private String tooltip;
        }

        public static String getColorForScore(int score) {
            if (score <= 20) return "#22c55e";
            if (score <= 40) return "#fbbf24";
            if (score <= 70) return "#f97316";
            return "#ef4444";
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RiskTrendDTO {
        private EntityType entityType;
        private Long entityId;
        private String entityName;
        private String trendPeriod;
        private List<TrendDataPoint> riskScoreTrend;
        private List<TrendDataPoint> violationTrend;
        private List<TrendDataPoint> escalationTrend;
        private List<TrendDataPoint> complianceRateTrend;
        private Map<String, MonthlyStats> monthlyStatistics;
        private String overallTrendDirection;
        private Integer trendPercentageChange;
        private Integer avgRiskScore;
        private Integer maxRiskScore;
        private Integer minRiskScore;
        private Double standardDeviation;
        private Integer projectedNextMonthScore;
        private RiskLevel projectedRiskLevel;
        private String forecastConfidence;
        private String forecastNotes;
        private PeriodComparison thisVsLastPeriod;
        private PeriodComparison thisVsSamePeriodLastYear;

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class TrendDataPoint {
            private String date;
            private Integer value;
            private RiskLevel riskLevel;
            private String label;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class MonthlyStats {
            private String month;
            private Integer riskScore;
            private RiskLevel riskLevel;
            private Long violationCount;
            private Long resolvedCount;
            private Long escalationCount;
            private Double complianceRate;
            private Integer newViolations;
            private Integer closedViolations;
        }

        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class PeriodComparison {
            private String currentPeriod;
            private String comparisonPeriod;
            private Integer currentScore;
            private Integer comparisonScore;
            private Integer scoreDifference;
            private Integer percentageChange;
            private String changeDirection;
            private Long currentViolations;
            private Long comparisonViolations;
        }

        public boolean isConcerningTrend() {
            return "UP".equals(overallTrendDirection) && trendPercentageChange != null && trendPercentageChange > 20;
        }

        public String getTrendSummary() {
            if (trendPercentageChange == null) return "Insufficient data for trend analysis";
            if ("STABLE".equals(overallTrendDirection))
                return String.format("Risk has remained stable (±%d%%) over the period", Math.abs(trendPercentageChange));
            String direction = "UP".equals(overallTrendDirection) ? "increased" : "decreased";
            return String.format("Risk has %s by %d%% compared to previous period", direction, Math.abs(trendPercentageChange));
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopDefaulterDTO {
        private Long userId;
        private String userName;
        private String email;
        private String employeeId;
        private String role;
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Integer totalViolations;
        private Integer pendingViolations;
        private Integer resolvedViolations;
        private Integer lateAcknowledgements;
        private Integer riskScore;
        private RiskLevel riskLevel;
        private String riskLevelColor;
        private Boolean isChronicDefaulter;
        private Boolean isRepeatOffender;
        private String defaulterCategory;
        private Integer deptAdminEscalations;
        private Integer superAdminEscalations;
        private Boolean hasActiveEscalation;
        private LocalDateTime firstViolationDate;
        private LocalDateTime lastViolationDate;
        private Integer violationsLast30Days;
        private Integer violationsLast90Days;

        public String getSeverityLabel() {
            if (totalViolations >= 10) return "SEVERE";
            if (totalViolations >= 5) return "HIGH";
            if (totalViolations >= 3) return "MODERATE";
            return "LOW";
        }

        public String getRecommendedAction() {
            if (Boolean.TRUE.equals(isChronicDefaulter)) return "Requires management intervention and formal counseling";
            if (Boolean.TRUE.equals(isRepeatOffender)) return "Department admin should review and address pattern";
            if (pendingViolations > 0) return "Follow up on pending violations";
            return "Continue monitoring";
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserRiskDTO {
        private Long userId;
        private String userName;
        private String email;
        private String employeeId;
        private String role;
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Integer riskScore;
        private RiskLevel riskLevel;
        private String riskLevelColor;
        private String riskLevelDescription;
        private Integer lateAcknowledgementCount;
        private Integer totalViolationCount;
        private Integer pendingViolationCount;
        private Integer resolvedViolationCount;
        private Integer deptAdminEscalationCount;
        private Integer superAdminEscalationCount;
        private Integer legalHoldDocumentCount;
        private Integer safetyViolationCount;
        private Double complianceRate;
        private Integer totalAssignedDocuments;
        private Integer acknowledgedDocuments;
        private Integer pendingAcknowledgements;
        private Double avgDaysDelayed;
        private Integer maxDaysDelayed;
        private Integer violationsLast7Days;
        private Integer violationsLast30Days;
        private Integer violationsLast90Days;
        private String trendDirection;
        private Integer trendPercentage;
        private Boolean isChronicDefaulter;
        private Boolean isRepeatOffender;
        private Boolean hasOpenEscalation;
        private Boolean hasLegalHoldViolation;
        private LocalDateTime lastAcknowledgementDate;
        private LocalDateTime lastViolationDate;
        private LocalDateTime lastLoginDate;
        private Integer daysSinceLastAcknowledgement;
        private LocalDateTime lastCalculatedAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;

        public boolean isHighRisk() {
            return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
        }

        public boolean needsManagementAttention() {
            return Boolean.TRUE.equals(isChronicDefaulter) ||
                   Boolean.TRUE.equals(hasOpenEscalation) ||
                   riskLevel == RiskLevel.CRITICAL;
        }

        public String getDefaulterStatus() {
            if (Boolean.TRUE.equals(isChronicDefaulter)) return "CHRONIC DEFAULTER";
            if (Boolean.TRUE.equals(isRepeatOffender)) return "REPEAT OFFENDER";
            if (pendingViolationCount != null && pendingViolationCount > 0) return "HAS PENDING VIOLATIONS";
            return "COMPLIANT";
        }
    }
}
