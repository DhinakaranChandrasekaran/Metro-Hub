package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @Column(name = "is_manually_classified")
    @Builder.Default
    private Boolean isManuallyClassified = false;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "extraction_method", length = 50)
    private String extractionMethod;

    @Column(name = "is_text_extracted")
    @Builder.Default
    private Boolean isTextExtracted = false;

    @Column(name = "extracted_file_path", length = 500)
    private String extractedFilePath;

    @Column(name = "ocr_language", length = 20)
    private String ocrLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "upload_date", updatable = false)
    private LocalDateTime uploadDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String tags;

    @Column(name = "legal_hold")
    @Builder.Default
    private Boolean legalHold = false;

    @Column(name = "legal_hold_reason", length = 500)
    private String legalHoldReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_hold_by")
    private User legalHoldBy;

    @Column(name = "legal_hold_date")
    private LocalDateTime legalHoldDate;

    @Column(name = "sla_reminder_hours")
    private Integer slaReminderHours;

    @Column(name = "sla_dept_admin_escalation_hours")
    private Integer slaDeptAdminEscalationHours;

    @Column(name = "sla_super_admin_escalation_hours")
    private Integer slaSuperAdminEscalationHours;

    @Column(name = "sla_violation_hours")
    private Integer slaViolationHours;

    @Column(name = "sla_configured_at")
    private LocalDateTime slaConfiguredAt;

    @Column(name = "is_sla_manual")
    @Builder.Default
    private Boolean isSlaManual = false;

    @Column(name = "sla_email_enabled")
    private Boolean slaEmailEnabled;

    @Column(name = "sla_sms_enabled")
    private Boolean slaSmsEnabled;

    @Column(name = "sla_dashboard_enabled")
    private Boolean slaDashboardEnabled;

    public enum DocumentType {
        JOB_CARD, INVOICE, POLICY, SAFETY_CIRCULAR, LEGAL_NOTICE,
        CONTRACT, MANUAL, REPORT, MEMO, CERTIFICATE, OTHER
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum DocumentStatus {
        ACTIVE, ARCHIVED, PENDING_REVIEW, DELETED
    }
}
