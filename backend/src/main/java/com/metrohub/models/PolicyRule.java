package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_rules",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_policy_dept_priority",
           columnNames = {"department_id", "priority"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private Document.Priority priority;

    @Column(name = "reminder_hours", nullable = false)
    @Builder.Default
    private Integer reminderHours = 24;

    @Column(name = "dept_admin_escalation_hours", nullable = false)
    @Builder.Default
    private Integer deptAdminEscalationHours = 48;

    @Column(name = "super_admin_escalation_hours", nullable = false)
    @Builder.Default
    private Integer superAdminEscalationHours = 72;

    @Column(name = "violation_hours", nullable = false)
    @Builder.Default
    private Integer violationHours = 168;

    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(name = "dashboard_enabled")
    @Builder.Default
    private Boolean dashboardEnabled = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getDepartmentId() { return department != null ? department.getId() : null; }
    public String getDepartmentName() { return department != null ? department.getName() : "All Departments"; }
    public String getPriorityName() { return priority != null ? priority.name() : "All Priorities"; }
    public boolean isGlobalRule() { return department == null && priority == null; }
    public boolean isDeptAdminEscalationEnabled() { return deptAdminEscalationHours != null && deptAdminEscalationHours > 0; }
    public boolean isSuperAdminEscalationEnabled() { return superAdminEscalationHours != null && superAdminEscalationHours > 0; }
    public boolean isViolationEnabled() { return violationHours != null && violationHours > 0; }
    public String getCreatedByName() { return createdBy != null ? createdBy.getName() : null; }
    public String getUpdatedByName() { return updatedBy != null ? updatedBy.getName() : null; }

    public String getScopeDescription() {
        StringBuilder scope = new StringBuilder();
        scope.append(department != null ? department.getName() : "All Departments");
        scope.append(" / ");
        scope.append(priority != null ? priority.name() + " Priority" : "All Priorities");
        return scope.toString();
    }
}
