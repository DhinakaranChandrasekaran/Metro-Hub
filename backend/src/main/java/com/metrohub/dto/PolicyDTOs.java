package com.metrohub.dto;

import com.metrohub.models.Document;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class PolicyDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PolicyRuleDTO {
        private Long id;
        private String name;
        private String description;
        private Long departmentId;
        private String departmentName;
        private Document.Priority priority;
        private String priorityName;
        private String scopeDescription;
        private Integer reminderHours;
        private Integer deptAdminEscalationHours;
        private Integer superAdminEscalationHours;
        private Integer violationHours;
        private Boolean emailEnabled;
        private Boolean smsEnabled;
        private Boolean dashboardEnabled;
        private Boolean isActive;
        private Boolean isDefault;
        private Boolean deptAdminEscalationEnabled;
        private Boolean superAdminEscalationEnabled;
        private Boolean violationEnabled;
        private Long createdById;
        private String createdByName;
        private Long updatedById;
        private String updatedByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PolicyRuleRequestDTO {
        @NotBlank(message = "Policy name is required")
        @Size(max = 100, message = "Policy name cannot exceed 100 characters")
        private String name;

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;

        private Long departmentId;
        private Document.Priority priority;

        @Min(value = 0, message = "Reminder hours must be >= 0")
        @Builder.Default
        private Integer reminderHours = 0;

        @Min(value = 0, message = "Dept admin escalation hours must be >= 0")
        @Builder.Default
        private Integer deptAdminEscalationHours = 0;

        @Min(value = 0, message = "Super admin escalation hours must be >= 0")
        @Builder.Default
        private Integer superAdminEscalationHours = 0;

        @Min(value = 0, message = "Violation hours must be >= 0")
        @Builder.Default
        private Integer violationHours = 0;

        @Builder.Default
        private Boolean emailEnabled = true;

        @Builder.Default
        private Boolean smsEnabled = false;

        @Builder.Default
        private Boolean dashboardEnabled = true;

        @Builder.Default
        private Boolean isActive = true;

        @Builder.Default
        private Boolean isDefault = false;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LegalHoldRequestDTO {
        @NotBlank(message = "Legal hold reason is required")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        private String reason;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LegalHoldResponseDTO {
        private Long documentId;
        private String documentName;
        private Boolean legalHold;
        private String legalHoldReason;
        private Long legalHoldById;
        private String legalHoldByName;
        private LocalDateTime legalHoldDate;
        private String message;
    }
}
