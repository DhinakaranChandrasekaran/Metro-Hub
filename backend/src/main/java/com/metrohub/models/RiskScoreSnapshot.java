package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_score_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_name", length = 200)
    private String entityName;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "late_acknowledgement_count")
    @Builder.Default
    private Integer lateAcknowledgementCount = 0;

    @Column(name = "violation_count")
    @Builder.Default
    private Integer violationCount = 0;

    @Column(name = "pending_violation_count")
    @Builder.Default
    private Integer pendingViolationCount = 0;

    @Column(name = "dept_admin_escalation_count")
    @Builder.Default
    private Integer deptAdminEscalationCount = 0;

    @Column(name = "super_admin_escalation_count")
    @Builder.Default
    private Integer superAdminEscalationCount = 0;

    @Column(name = "legal_hold_count")
    @Builder.Default
    private Integer legalHoldCount = 0;

    @Column(name = "safety_violation_count")
    @Builder.Default
    private Integer safetyViolationCount = 0;

    @Column(name = "repeat_offense_count")
    @Builder.Default
    private Integer repeatOffenseCount = 0;

    @Column(name = "calculation_period_start")
    private LocalDateTime calculationPeriodStart;

    @Column(name = "calculation_period_end")
    private LocalDateTime calculationPeriodEnd;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "calculation_notes", length = 500)
    private String calculationNotes;

    public enum EntityType {
        USER, DEPARTMENT
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public static RiskLevel calculateRiskLevel(int score) {
        if (score <= 20) return RiskLevel.LOW;
        if (score <= 40) return RiskLevel.MEDIUM;
        if (score <= 70) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    public String getRiskLevelColor() {
        return switch (riskLevel) {
            case LOW -> "#22c55e";
            case MEDIUM -> "#f59e0b";
            case HIGH -> "#f97316";
            case CRITICAL -> "#ef4444";
        };
    }

    public String getRiskLevelDescription() {
        return switch (riskLevel) {
            case LOW -> "Risk is within acceptable limits";
            case MEDIUM -> "Moderate risk - monitoring recommended";
            case HIGH -> "High risk - immediate attention required";
            case CRITICAL -> "Critical risk - urgent intervention needed";
        };
    }
}
