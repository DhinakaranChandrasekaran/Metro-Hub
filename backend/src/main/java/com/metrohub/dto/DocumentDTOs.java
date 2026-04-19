package com.metrohub.dto;

import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.models.Document.DocumentStatus;
import com.metrohub.models.DocumentReminder;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DocumentDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocumentUploadDTO {
        @NotNull(message = "File is required")
        private MultipartFile file;

        @NotNull(message = "Department is required")
        private Long departmentId;

        private DocumentType documentType;

        @Builder.Default
        private Priority priority = Priority.MEDIUM;

        private String description;
        private String tags;
        private String equipmentId;
        private String vendorName;
        private String referenceNumber;
        private Integer slaAckHours;
        private Integer slaEsc1;
        private Integer slaEsc2;
        private Integer slaEsc3;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocumentResponseDTO {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String fileExtension;
        private DocumentType documentType;
        private String documentTypeName;
        private Priority priority;
        private String departmentName;
        private Long departmentId;
        private Double classificationConfidence;
        private String extractedTextPreview;
        private Boolean isTextExtracted;
        private String extractionMethod;
        private String uploadedByName;
        private String uploadedByEmail;
        private LocalDateTime uploadDate;
        private DocumentStatus status;
        private String description;
        private String tags;
        private String downloadUrl;
        private String previewUrl;
        private Integer slaReminderHours;
        private Integer slaDeptAdminEscalationHours;
        private Integer slaSuperAdminEscalationHours;
        private Integer slaViolationHours;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocumentCardDTO {
        private Long id;
        private String fileName;
        private String fileType;
        private String fileExtension;
        private Long fileSize;
        private String fileSizeFormatted;
        private DocumentType documentType;
        private String documentTypeName;
        private Priority priority;
        private String priorityDisplay;
        private String departmentName;
        private Long departmentId;
        private Double classificationConfidence;
        private String summary;
        private String textPreview;
        private LocalDateTime uploadDate;
        private String uploadDateFormatted;
        private LocalDate deadline;
        private String deadlineFormatted;
        private Long daysUntilDeadline;
        private String deadlineStatus;
        private Boolean isOverdue;
        private DocumentStatus status;
        private String statusDisplay;
        private String uploadedByName;
        private Boolean hasDeadline;
        private Boolean isPendingAction;
        private String downloadUrl;

        public static String formatFileSize(Long bytes) {
            if (bytes == null || bytes == 0) return "0 B";
            String[] units = {"B", "KB", "MB", "GB"};
            int unitIndex = 0;
            double size = bytes;
            while (size >= 1024 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }
            return String.format("%.1f %s", size, units[unitIndex]);
        }

        public static String getDocumentTypeDisplayName(DocumentType type) {
            if (type == null) return "Unknown";
            return switch (type) {
                case JOB_CARD -> "Job Card";
                case INVOICE -> "Invoice";
                case POLICY -> "Policy";
                case SAFETY_CIRCULAR -> "Safety Circular";
                case LEGAL_NOTICE -> "Legal Notice";
                case CONTRACT -> "Contract";
                case MANUAL -> "Manual";
                case REPORT -> "Report";
                case MEMO -> "Memo";
                case CERTIFICATE -> "Certificate";
                case OTHER -> "Other";
            };
        }

        public static String getPriorityDisplay(Priority priority) {
            if (priority == null) return "Unknown";
            return switch (priority) {
                case HIGH -> "High";
                case MEDIUM -> "Medium";
                case LOW -> "Low";
            };
        }

        public static String getStatusDisplayName(DocumentStatus status) {
            if (status == null) return "Unknown";
            return switch (status) {
                case ACTIVE -> "Active";
                case ARCHIVED -> "Archived";
                case PENDING_REVIEW -> "Pending Review";
                case DELETED -> "Deleted";
            };
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentSlaConfigDTO {
        @Min(value = 0, message = "Reminder hours cannot be negative")
        private Integer reminderHours;

        @Min(value = 0, message = "Dept admin escalation hours cannot be negative")
        private Integer deptAdminEscalationHours;

        @Min(value = 0, message = "Super admin escalation hours cannot be negative")
        private Integer superAdminEscalationHours;

        @Min(value = 0, message = "Violation hours cannot be negative")
        private Integer violationHours;

        private Boolean emailEnabled;
        private Boolean smsEnabled;
        private Boolean dashboardEnabled;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ExtractedTextResponseDTO {
        private Long documentId;
        private String fileName;
        private String extractedText;
        private String extractionMethod;
        private Boolean isTextExtracted;
        private String ocrLanguage;
        private Integer textLength;
        private Integer wordCount;
        private DocumentType suggestedDocumentType;
        private String suggestedDepartment;
        private Priority suggestedPriority;
        private Double classificationConfidence;
        private String[] detectedKeywords;
        private LocalDateTime extractionDate;
        private String fileType;
        private Long fileSize;
        private String status;
        private String errorMessage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentReminderDTO {
        private Long id;
        private Long documentId;
        private String documentName;
        private Long targetUserId;
        private String targetUserEmail;
        private LocalDateTime reminderDate;
        private String message;
        private DocumentReminder.ReminderType reminderType;
        private Boolean isSent;
        private LocalDateTime sentAt;
        private Boolean isRecurring;
        private Integer recurrenceHours;
        private Integer maxOccurrences;
        private Integer occurrenceCount;
        private String createdByEmail;
        private LocalDateTime createdAt;
        private Boolean isActive;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DocumentReminderRequestDTO {
        @NotNull(message = "Document ID is required")
        private Long documentId;

        private Long targetUserId;

        @NotNull(message = "Reminder date is required")
        @Future(message = "Reminder date must be in the future")
        private LocalDateTime reminderDate;

        private String message;

        @Builder.Default
        private DocumentReminder.ReminderType reminderType = DocumentReminder.ReminderType.ACKNOWLEDGEMENT;

        @Builder.Default
        private Boolean isRecurring = false;

        @Positive(message = "Recurrence hours must be positive")
        private Integer recurrenceHours;

        @Positive(message = "Max occurrences must be positive")
        private Integer maxOccurrences;
    }
}
