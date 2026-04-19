package com.metrohub.dto;

import com.metrohub.models.Alert;
import com.metrohub.models.Alert.AlertType;
import lombok.*;
    
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDTO {

    private Long id;
    private AlertType alertType;
    private String alertTypeName;
    private String message;
    private String severity;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String createdAtFormatted;
    private String timeAgo;
    private Long documentId;
    private String documentName;
    private String documentPriority;
    private String departmentName;
    private String icon;
    private String colorClass;

    public static String getAlertTypeName(AlertType type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case HIGH_PRIORITY_UPLOAD -> "High Priority Upload";
            case DEADLINE_APPROACHING -> "Deadline Approaching";
            case DEADLINE_OVERDUE -> "Deadline Overdue";
            case DOCUMENT_PENDING_REVIEW -> "Pending Review";
            case DEADLINE_TODAY -> "Deadline Today";
            case NEW_DOCUMENT_UPLOADED -> "New Document";
            case ACKNOWLEDGEMENT_REQUIRED -> "Acknowledgement Required";
            case COMPLIANCE_REMINDER -> "Compliance Reminder";
            case ESCALATION_DEPT_ADMIN -> "Escalation to Department Admin";
            case ESCALATION_SUPER_ADMIN -> "Escalation to Super Admin";
            case COMPLIANCE_VIOLATION_CREATED -> "Compliance Violation Created";
            case SLA_CONFIGURED -> "SLA Configured";
        };
    }

    public static String getAlertIcon(AlertType type) {
        if (type == null) return "icon-bell";
        return switch (type) {
            case HIGH_PRIORITY_UPLOAD -> "icon-alert-circle";
            case DEADLINE_APPROACHING -> "icon-clock";
            case DEADLINE_OVERDUE -> "icon-x-circle";
            case DOCUMENT_PENDING_REVIEW -> "icon-file-text";
            case DEADLINE_TODAY -> "icon-calendar";
            case NEW_DOCUMENT_UPLOADED -> "icon-file";
            case ACKNOWLEDGEMENT_REQUIRED -> "icon-check-square";
            case COMPLIANCE_REMINDER -> "icon-alert-triangle";
            case ESCALATION_DEPT_ADMIN -> "icon-arrow-up";
            case ESCALATION_SUPER_ADMIN -> "icon-alert-octagon";
            case COMPLIANCE_VIOLATION_CREATED -> "icon-slash";
            case SLA_CONFIGURED -> "icon-settings";
        };
    }

    public static String getAlertColorClass(AlertType type) {
        if (type == null) return "gray";
        return switch (type) {
            case HIGH_PRIORITY_UPLOAD, DEADLINE_OVERDUE, COMPLIANCE_VIOLATION_CREATED -> "red";
            case DEADLINE_TODAY, DEADLINE_APPROACHING, ACKNOWLEDGEMENT_REQUIRED, COMPLIANCE_REMINDER -> "orange";
            case DOCUMENT_PENDING_REVIEW, NEW_DOCUMENT_UPLOADED, SLA_CONFIGURED -> "blue";
            case ESCALATION_DEPT_ADMIN, ESCALATION_SUPER_ADMIN -> "purple";
        };
    }

    public static String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = hours / 24;
        if (days < 7) return days + " day" + (days == 1 ? "" : "s") + " ago";
        long weeks = days / 7;
        if (weeks < 4) return weeks + " week" + (weeks == 1 ? "" : "s") + " ago";
        long months = days / 30;
        return months + " month" + (months == 1 ? "" : "s") + " ago";
    }

    public static AlertDTO fromEntity(Alert alert) {
        if (alert == null) return null;
        AlertDTOBuilder builder = AlertDTO.builder()
            .id(alert.getId())
            .alertType(alert.getAlertType())
            .alertTypeName(getAlertTypeName(alert.getAlertType()))
            .message(alert.getMessage())
            .severity(alert.getSeverity() != null ? alert.getSeverity() : "MEDIUM")
            .isRead(alert.getIsRead())
            .createdAt(alert.getCreatedAt())
            .createdAtFormatted(alert.getCreatedAt() != null ?
                alert.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")) : null)
            .timeAgo(calculateTimeAgo(alert.getCreatedAt()))
            .icon(getAlertIcon(alert.getAlertType()))
            .colorClass(getAlertColorClass(alert.getAlertType()));
        if (alert.getDocument() != null) {
            builder.documentId(alert.getDocument().getId())
                   .documentName(alert.getDocument().getFileName())
                   .documentPriority(alert.getDocument().getPriority() != null ?
                       alert.getDocument().getPriority().name() : null);
            if (alert.getDocument().getDepartment() != null) {
                builder.departmentName(alert.getDocument().getDepartment().getName());
            }
        }
        return builder.build();
    }
}
