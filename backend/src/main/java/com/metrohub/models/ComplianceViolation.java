package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_violations",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_violation_doc_user",
           columnNames = {"document_id", "user_id"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 50)
    @Builder.Default
    private ViolationType violationType = ViolationType.ACK_DELAY;

    @Column(name = "violation_date", nullable = false)
    private LocalDateTime violationDate;

    @Column(name = "days_delayed", nullable = false)
    private Integer daysDelayed;

    @Column(name = "resolved")
    @Builder.Default
    private Boolean resolved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "acknowledged_late")
    @Builder.Default
    private Boolean acknowledgedLate = false;

    @Column(name = "late_acknowledgement_date")
    private LocalDateTime lateAcknowledgementDate;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "dept_admin_escalated")
    @Builder.Default
    private Boolean deptAdminEscalated = false;

    @Column(name = "dept_admin_escalated_at")
    private LocalDateTime deptAdminEscalatedAt;

    @Column(name = "super_admin_escalated")
    @Builder.Default
    private Boolean superAdminEscalated = false;

    @Column(name = "super_admin_escalated_at")
    private LocalDateTime superAdminEscalatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_rule_id")
    private PolicyRule policyRule;

    @Column(name = "sla_hours_applied")
    private Integer slaHoursApplied;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ViolationType {
        ACK_DELAY
    }

    public Long getDocumentId() { return document != null ? document.getId() : null; }
    public Long getUserId() { return user != null ? user.getId() : null; }
    public Long getDepartmentId() { return department != null ? department.getId() : null; }
    public String getDocumentName() { return document != null ? document.getFileName() : null; }
    public String getUserName() { return user != null ? user.getName() : null; }
    public String getUserEmail() { return user != null ? user.getEmail() : null; }
    public String getDepartmentName() { return department != null ? department.getName() : null; }
    public String getResolvedByName() { return resolvedBy != null ? resolvedBy.getName() : null; }
    public Long getPolicyRuleId() { return policyRule != null ? policyRule.getId() : null; }
    public String getPolicyRuleName() { return policyRule != null ? policyRule.getName() : null; }
    public boolean isPending() { return !resolved; }

    public void markLateAcknowledgement() {
        this.acknowledgedLate = true;
        this.lateAcknowledgementDate = LocalDateTime.now();
    }

    public void resolve(User admin, String remarks) {
        this.resolved = true;
        this.resolvedBy = admin;
        this.resolvedDate = LocalDateTime.now();
        this.remarks = remarks;
    }
}
