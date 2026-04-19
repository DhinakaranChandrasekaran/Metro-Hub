package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", length = 20)
    @Builder.Default
    private NotificationChannel notificationChannel = NotificationChannel.DASHBOARD;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    @Column(name = "sms_sent")
    @Builder.Default
    private Boolean smsSent = false;

    @Column(name = "sla_reminder_hours")
    private Integer slaReminderHours;

    @Column(name = "sla_dept_admin_escalation_hours")
    private Integer slaDeptAdminEscalationHours;

    @Column(name = "sla_super_admin_escalation_hours")
    private Integer slaSuperAdminEscalationHours;

    @Column(name = "sla_violation_hours")
    private Integer slaViolationHours;

    @Column(name = "is_manual_sla")
    private Boolean isManualSla;

    @Column(name = "policy_name", length = 100)
    private String policyName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AlertType {
        HIGH_PRIORITY_UPLOAD,
        DEADLINE_APPROACHING,
        DEADLINE_OVERDUE,
        DOCUMENT_PENDING_REVIEW,
        DEADLINE_TODAY,
        NEW_DOCUMENT_UPLOADED,
        ACKNOWLEDGEMENT_REQUIRED,
        COMPLIANCE_REMINDER,
        ESCALATION_DEPT_ADMIN,
        ESCALATION_SUPER_ADMIN,
        COMPLIANCE_VIOLATION_CREATED,
        SLA_CONFIGURED
    }

    public enum NotificationChannel {
        DASHBOARD,
        EMAIL,
        SMS
    }

    public String getSeverity() {
        return switch (alertType) {
            case HIGH_PRIORITY_UPLOAD, DEADLINE_OVERDUE, COMPLIANCE_VIOLATION_CREATED, ESCALATION_SUPER_ADMIN -> "HIGH";
            case DEADLINE_TODAY, DEADLINE_APPROACHING, ACKNOWLEDGEMENT_REQUIRED, ESCALATION_DEPT_ADMIN, COMPLIANCE_REMINDER -> "MEDIUM";
            case DOCUMENT_PENDING_REVIEW, NEW_DOCUMENT_UPLOADED, SLA_CONFIGURED -> "LOW";
        };
    }

    public Long getTargetUserId() {
        return targetUser != null ? targetUser.getId() : null;
    }

    public Long getDocumentId() {
        return document != null ? document.getId() : null;
    }

    public Long getDepartmentId() {
        return department != null ? department.getId() : null;
    }

    public boolean isHighPriority() {
        return "HIGH".equals(getSeverity());
    }
}
